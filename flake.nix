{
  description = "HTTP & TCP tunnelling";

  inputs = {
    # Too old to work with most libraries
    # nixpkgs.url = "github:nixos/nixpkgs/nixos-24.11";

    # Perfect!
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";

    # The flake-utils library
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    ...
  }:
    flake-utils.lib.eachDefaultSystem
    (
      system: let
        pkgs = nixpkgs.legacyPackages.${system};
      in {
        # Nix script formatter
        formatter = pkgs.alejandra;

        # Development environment
        devShells = {
          default = self.devShells.${system}.server;
          cli = import ./cli/shell.nix {inherit pkgs;};
          server = import ./server/shell.nix {inherit pkgs;};
        };

        # Output package
        packages = {
          default = self.packages.${system}.cli;
          cli = pkgs.callPackage ./cli/default.nix {inherit pkgs;};
          server = pkgs.callPackage ./server/default.nix {inherit pkgs;};
        };
      }
    )
    // {
      # Overlay module
      nixosModules.tarmoqchi = import ./module.nix self;
    };
}
