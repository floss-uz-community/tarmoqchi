# For more, refer to:
# https://github.com/NixOS/nixpkgs/blob/master/doc/languages-frameworks/rust.section.md
{pkgs ? import <nixpkgs> {}}: let
  lib = pkgs.lib;
in
  pkgs.buildGoModule rec {
    pname = "tarmoqchi";
    version = "0.0.1";

    src = pkgs.lib.cleanSource ./.;

    vendorHash = "sha256-0Qxw+MUYVgzgWB8vi3HBYtVXSq/btfh4ZfV/m1chNrA=";

    meta = with lib; {
      homepage = "https://tarmoqchi.uz";
      mainProgram = "cli";
      description = "HTTP & TCP tunnelling";
      license = with lib.licenses; [mit];
      platforms = with platforms; linux ++ darwin;
      maintainers = [lib.maintainers.orzklv];
    };
  }
