# For more, refer to:
# https://github.com/NixOS/nixpkgs/blob/master/doc/languages-frameworks/rust.section.md
{pkgs ? import <nixpkgs> {}, ...}: let
  lib = pkgs.lib;
  version = "1.0.0";
in
  pkgs.maven.buildMavenPackage {
    pname = "tarmoqchi";
    inherit version;

    src = ./.;
    mvnHash = "sha256-Hjs7qpaqRf8N+hVMJNaM0Oli1pY0K5Xcegi20gPhpA0=";

    nativeBuildInputs = with pkgs; [
      makeWrapper
    ];

    installPhase = ''
      runHook preInstall

      mkdir -p $out/bin $out/share/tarmoqchi
      install -Dm644 ./target/server-${version}.jar $out/share/tarmoqchi

      makeWrapper ${pkgs.jre}/bin/java $out/bin/tarmoqchi \
        --add-flags "-jar $out/share/tarmoqchi/server-${version}.jar"

      runHook postInstall
    '';

    meta = with lib; {
      description = "HTTP & TCP tunnelling";
      homepage = "https://tarmoqchi.uz";
      license = with lib.licenses; [mit];
      platforms = with platforms; linux ++ darwin;
      maintainers = [lib.maintainers.orzklv];
    };
  }
