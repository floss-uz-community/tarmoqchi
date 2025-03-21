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
  *) echo "Unknown architecture: $(uname -sm) is not supported." >&2; exit 1 ;;
esac

echo "Downloading $FILENAME from GitHub..."
if ! curl -sSLf "$URL_PREFIX/$FILENAME" -o "$INSTALL_DIR/tarmoqchi"; then
  echo "Failed to write to $INSTALL_DIR; please try running with sudo." >&2
  exit 1
fi

if ! chmod +x "$INSTALL_DIR/tarmoqchi"; then
  echo "Failed to grant execution permissions to $INSTALL_DIR/tarmoqchi." >&2
  exit 1
fi

echo "Tarmoqchi has been successfully installed!"