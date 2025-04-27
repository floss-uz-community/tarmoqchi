{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  # nativeBuildInputs is usually what you want -- tools you need to run
  nativeBuildInputs = with pkgs; [
    # Nix toolchain
    nixd
    statix
    deadnix
    alejandra

    # Go development
    go
    go-outline
    gopls
    gopkgs
    go-tools
    delve
  ];

  hardeningDisable = [ "all" ];
}
