flake: {
  config,
  lib,
  pkgs,
  ...
}: let
  inherit (lib) mkEnableOption mkOption mkIf mkMerge types;

  # Options
  cfg = config.services.tarmoqchi;

  # Flake shipped default binary
  fpkg = flake.packages.${pkgs.stdenv.hostPlatform.system}.server;

  # Toml management
  toml = pkgs.formats.toml {};

  # Find out whether shall we manage database locally
  local-database = (
    (cfg.database.host == "127.0.0.1") || (cfg.database.host == "localhost")
  );

  # The digesting configuration of server
  toml-config = toml.generate "config.toml" {
    app.port = toString cfg.port;
    spring.datasource.url = "#databaseUrl#";
    github = {
      client-id = "#ghcid#";
      client-secret = "#ghcsecret#";
      redirect-uri = "https://${cfg.proxy-reverse.domain}/github/callback";
    };
  };

  # Caddy proxy reversing
  caddy = mkIf (cfg.enable && cfg.proxy-reverse.enable && cfg.proxy-reverse.proxy == "caddy") {
    services.caddy.virtualHosts = lib.debug.traceIf (builtins.isNull cfg.proxy-reverse.domain) "domain can't be null, please specicy it properly!" {
      "${cfg.proxy-reverse.domain}" = {
        extraConfig = ''
          reverse_proxy 127.0.0.1:${toString cfg.port}
        '';
      };
    };
  };

  # Nginx proxy reversing
  nginx = mkIf (cfg.enable && cfg.proxy-reverse.enable && cfg.proxy-reverse.proxy == "nginx") {
    services.nginx.virtualHosts = lib.debug.traceIf (builtins.isNull cfg.proxy-reverse.domain) "domain can't be null, please specicy it properly!" {
      "${cfg.proxy-reverse.domain}" = {
        addSSL = true;
        enableACME = true;
        serverAliases = ["*.${cfg.proxy-reverse.domain}"];
        locations."/" = {
          proxyPass = "http://127.0.0.1:${toString cfg.port}";
          proxyWebsockets = true;
        };
      };
    };
  };

  # Systemd services
  service = mkIf cfg.enable {
    ## User for our services
    users.users = lib.mkIf (cfg.user == "tarmoqchi") {
      "tarmoqchi" = {
        description = "Tarmoqchi Service";
        home = cfg.dataDir;
        useDefaultShell = true;
        group = cfg.group;
        isSystemUser = true;
      };
    };

    ## Group to join our user
    users.groups = mkIf (cfg.group == "tarmoqchi") {
      "tarmoqchi" = {};
    };

    ## Postgresql service (turn on if it's not already on)
    services.postgresql = lib.optionalAttrs local-database {
      enable = lib.mkDefault true;

      ensureDatabases = [cfg.database.name];
      ensureUsers = [
        {
          name = cfg.database.user;
          ensureDBOwnership = true;
        }
      ];
    };

    # Configurator service (before actual server)
    systemd.services."tarmoqchi-config" = {
      wantedBy = ["tarmoqchi.target"];
      partOf = ["tarmoqchi.target"];
      path = with pkgs; [
        jq
        openssl
        replace-secret
      ];

      serviceConfig = {
        Type = "oneshot";
        User = cfg.user;
        Group = cfg.group;
        TimeoutSec = "infinity";
        Restart = "on-failure";
        WorkingDirectory = "${cfg.dataDir}";
        RemainAfterExit = true;

        ExecStartPre = let
          preStartFullPrivileges = ''
            set -o errexit -o pipefail -o nounset
            shopt -s dotglob nullglob inherit_errexit

            chown -R --no-dereference '${cfg.user}':'${cfg.group}' '${cfg.dataDir}'
            chmod -R u+rwX,g+rX,o-rwx '${cfg.dataDir}'
          '';
        in "+${pkgs.writeShellScript "tarmoqchi-pre-start-full-privileges" preStartFullPrivileges}";

        ExecStart = pkgs.writeShellScript "tarmoqchi-config" ''
          set -o errexit -o pipefail -o nounset
          shopt -s inherit_errexit

          umask u=rwx,g=rx,o=

          # Write configuration file for server
          cp -f ${toml-config} ${cfg.dataDir}/config.toml

          echo "DATABASE_URL=\"jdbc:postgresql://${cfg.database.host}:${toString cfg.database.port}/${cfg.database.name}?user=${cfg.database.user}\&password=#password#\"" > "${cfg.dataDir}/.env"
          echo "GITHUB_ID=#ghcid#" >> "${cfg.dataDir}/.env"
          echo "GITHUB_SECRET=#ghcsecret#" >> "${cfg.dataDir}/.env"

          replace-secret '#password#' '${cfg.database.passwordFile}' '${cfg.dataDir}/.env'
          replace-secret '#ghcid#' '${cfg.github.id}' '${cfg.dataDir}/.env'
          replace-secret '#ghcsecret#' '${cfg.github.secret}' '${cfg.dataDir}/.env'

          source "${cfg.dataDir}/.env"

          sed -i "s|#databaseUrl#|$DATABASE_URL|g" "${cfg.dataDir}/config.toml"
          sed -i "s|#ghcid#|$GITHUB_ID|g" "${cfg.dataDir}/config.toml"
          sed -i "s|#ghcsecret#|$GITHUB_SECRET|g" "${cfg.dataDir}/config.toml"
        '';
      };
    };

    ## Main server service
    systemd.services."tarmoqchi" = {
      description = "tarmoqchi HTTP & TCP tunneling";
      documentation = ["https://tarmoqchi.uz"];

      after = ["network.target" "tarmoqchi-config.service"] ++ lib.optional local-database "postgresql.service";
      requires = lib.optional local-database "postgresql.service";
      wants = ["network-online.target"];
      wantedBy = ["multi-user.target"];
      path = [cfg.package];

      serviceConfig = {
        User = cfg.user;
        Group = cfg.group;
        Restart = "always";
        ExecStart = "${lib.getBin cfg.package}/bin/tarmoqchi --config=${cfg.dataDir}/config.toml";
        ExecReload = "${pkgs.coreutils}/bin/kill -s HUP $MAINPID";
        StateDirectory = cfg.user;
        StateDirectoryMode = "0750";
        # Access write directories
        ReadWritePaths = [cfg.dataDir "/run/postgresql"];
        CapabilityBoundingSet = [
          "AF_NETLINK"
          "AF_INET"
          "AF_INET6"
        ];
        DeviceAllow = ["/dev/stdin r"];
        DevicePolicy = "strict";
        IPAddressAllow = "localhost";
        LockPersonality = true;
        NoNewPrivileges = true;
        PrivateDevices = true;
        PrivateTmp = true;
        PrivateUsers = false;
        ProtectClock = true;
        ProtectControlGroups = true;
        ProtectHome = true;
        ProtectHostname = true;
        ProtectKernelLogs = true;
        ProtectKernelModules = true;
        ProtectKernelTunables = true;
        ProtectSystem = "strict";
        ReadOnlyPaths = ["/"];
        RemoveIPC = true;
        RestrictAddressFamilies = [
          "AF_NETLINK"
          "AF_INET"
          "AF_INET6"
          "AF_UNIX"
        ];
        RestrictNamespaces = true;
        RestrictRealtime = true;
        RestrictSUIDSGID = true;
        SystemCallArchitectures = "native";
        SystemCallFilter = [
          "@system-service"
          "~@privileged"
          "~@resources"
          "@pkey"
        ];
        UMask = "0027";
      };
    };
  };

  # Various checks and tests of options
  asserts = lib.mkIf cfg.enable {
    ## Warning (nixos-rebuild doesn't fail if any warning shows up)
    warnings = [];
    # ++ lib.optional
    # (cfg.proxy-reverse.enable && (cfg.proxy-reverse.domain == null || cfg.proxy-reverse.domain == ""))
    # "services.tarmoqchi.proxy-reverse.domain must be set in order to properly generate certificate!";

    ## Tests (nixos-rebuilds fails if any test fails)
    assertions =
      lib.optional
      (cfg.proxy-reverse.enable)
      {
        assertion = cfg.proxy-reverse.domain != null && cfg.proxy-reverse.domain != "";
        message = "You must specify a valid domain when proxy-reverse is enabled!";
      };
  };
in {
  # Available user options
  options = with lib; {
    services.tarmoqchi = {
      enable = mkEnableOption ''
        Tarmoqchi, HTTP & TCP tunneling.
      '';

      port = mkOption {
        type = types.int;
        default = 39393;
        description = "Port to use for passing over proxy";
      };

      proxy-reverse = {
        enable = mkEnableOption ''
          Enable proxy reversing via nginx/caddy.
        '';

        domain = mkOption {
          type = with types; nullOr str;
          default = null;
          example = "tarmoqchi.uz";
          description = "Domain to use while adding configurations to web proxy server";
        };

        proxy = mkOption {
          type = with types;
            nullOr (enum [
              "nginx"
              "caddy"
            ]);
          default = "caddy";
          description = "Web server software for proxy reversing";
        };
      };

      github = {
        id = mkOption {
          type = types.nullOr types.path;
          default = null;
          example = "/run/keys/tarmoqchi-github-id";
          description = ''
            A file containing the github client id to
            {option}`github.id`.
          '';
        };

        secret = mkOption {
          type = types.nullOr types.path;
          default = null;
          example = "/run/keys/tarmoqchi-github-secret";
          description = ''
            A file containing the github secret corresponding to
            {option}`github.secret`.
          '';
        };

        redirect = mkOption {
          type = types.str;
          default = "https://${cfg.proxy-reverse.domain}/github/callback";
          description = "Address where successful oath should be redirected to";
        };
      };

      database = {
        host = mkOption {
          type = types.str;
          default = "127.0.0.1";
          description = "Database host address. Leave \"127.0.0.1\" if you want local database";
        };

        port = mkOption {
          type = types.port;
          default = config.services.postgresql.settings.port;
          defaultText = "5432";
          description = "Database host port.";
        };

        name = mkOption {
          type = types.str;
          default = "tarmoqchi";
          description = "Database name.";
        };

        user = mkOption {
          type = types.str;
          default = "tarmoqchi";
          description = "Database user.";
        };

        passwordFile = mkOption {
          type = types.nullOr types.path;
          default = null;
          example = "/run/keys/tarmoqchi-dbpassword";
          description = ''
            A file containing the password corresponding to
            {option}`database.user`.
          '';
        };
      };

      user = mkOption {
        type = types.str;
        default = "tarmoqchi";
        description = "User for running system + accessing keys";
      };

      group = mkOption {
        type = types.str;
        default = "tarmoqchi";
        description = "Group for running system + accessing keys";
      };

      dataDir = mkOption {
        type = types.str;
        default = "/var/lib/tarmoqchi";
        description = lib.mdDoc ''
          The path where tarmoqchi keeps its config, data, and logs.
        '';
      };

      package = mkOption {
        type = types.package;
        default = fpkg;
        description = ''
          Compiled tarmoqchi server package to use with the service.
        '';
      };
    };
  };

  config = mkMerge [asserts service caddy nginx];
}
