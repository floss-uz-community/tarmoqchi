{
  pkgs ? let
    lock = (builtins.fromJSON (builtins.readFile ./flake.lock)).nodes.nixpkgs.locked;
    nixpkgs = fetchTarball {
      url = "https://github.com/nixos/nixpkgs/archive/${lock.rev}.tar.gz";
      sha256 = lock.narHash;
    };
  in
    import nixpkgs {overlays = [];},
  ...
}: let
  cwd = builtins.toString ./.;
in
  pkgs.stdenv.mkDerivation {
    name = "nix";

    nativeBuildInputs = with pkgs; [
      git
      just

      jdk17
      maven
      bashInteractive

      nixd
      statix
      deadnix
      alejandra
    ];

    JAVA_HOME_17 = "${pkgs.jdk17}/lib/openjdk";
    JAVA_HOME_11 = "${pkgs.jdk11}/lib/openjdk";
    MAVEN_OPTS = "-Dmaven.repo.local=${cwd}/.m2";
    NIX_CONFIG = "extra-experimental-features = nix-command flakes";
  }
