#!/bin/bash

set -e

URL_PREFIX="https://github.com/jamshid-elmurodov/tarmoqchi/releases/download/Tarmoqchi-1.0.0"
INSTALL_DIR=${INSTALL_DIR:-/usr/local/bin}

case "$(uname -sm)" in
  "Darwin x86_64") FILENAME="tarmoqchi-darwin-amd64" ;;
  "Darwin arm64") FILENAME="tarmoqchi-darwin-arm64" ;;
  "Linux x86_64") FILENAME="tarmoqchi-linux-amd64" ;;
  "Linux i686") FILENAME="tarmoqchi-linux-386" ;;
  "Linux armv7l") FILENAME="tarmoqchi-linux-arm" ;;
  "Linux aarch64") FILENAME="tarmoqchi-linux-arm64" ;;
  "CYGWIN"*|"MINGW"*|"MSYS"*) FILENAME="tarmoqchi-windows-amd64.exe" ; INSTALL_DIR="/c/Windows/System32" ;;
  *) echo "Noma'lum arxitektura: $(uname -sm) qo‘llab-quvvatlanmaydi." >&2; exit 1 ;;
esac

echo "$FILENAME fayli GitHub'dan yuklanmoqda..."
if ! curl -sSLf "$URL_PREFIX/$FILENAME" -o "$INSTALL_DIR/tarmoqchi"; then
  echo "$INSTALL_DIR ga yozib bo‘lmadi; iltimos, sudo bilan urinib ko‘ring." >&2
  exit 1
fi

if ! chmod +x "$INSTALL_DIR/tarmoqchi"; then
  echo "$INSTALL_DIR/tarmoqchi ga bajariladigan huquq berib bo‘lmadi." >&2
  exit 1
fi

echo "Tarmoqchi muvaffaqiyatli o‘rnatildi!"