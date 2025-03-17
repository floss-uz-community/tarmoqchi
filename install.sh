#!/bin/bash

# Tarmoqchi installation script

# Check for root privileges
if [ "$EUID" -ne 0 ]; then
  echo "Please run with sudo: sudo bash $0"
  exit 1
fi

# Detect architecture
ARCH=$(uname -m)

if [ "$ARCH" = "x86_64" ]; then
  ARCH_TYPE="x86_64"
elif [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ]; then
  ARCH_TYPE="arm64"
else
  echo "Unsupported architecture: $ARCH"
  echo "Only x86_64 and arm64 architectures are supported."
  exit 1
fi

# Detect OS type
OS=$(uname -s)
if [ "$OS" = "Darwin" ]; then
  OS_TYPE="macos"
elif [ "$OS" = "Linux" ]; then
  OS_TYPE="linux"
else
  echo "Unsupported operating system: $OS"
  echo "Only macOS and Linux are supported."
  exit 1
fi

# Set installation directory
INSTALL_DIR="/usr/local/bin"
BINARY_NAME="tarmoqchi"

# GitHub Releases URL
DOWNLOAD_URL="https://github.com/jamshid-elmurodov/tarmoqchi/releases/download/Tarmoqchi-1.0.0/tarmoqchi-${ARCH_TYPE}"

echo "Installing Tarmoqchi for ${OS_TYPE} on ${ARCH_TYPE}..."

# Download binary
curl -L "$DOWNLOAD_URL" -o "$INSTALL_DIR/$BINARY_NAME"

# Set executable permissions
chmod +x "$INSTALL_DIR/$BINARY_NAME"

# Verify installation
if [ -x "$INSTALL_DIR/$BINARY_NAME" ]; then
  echo "Tarmoqchi has been successfully installed to $INSTALL_DIR/$BINARY_NAME"
  echo "You can now run it by typing 'tarmoqchi' in the terminal."
else
  echo "Installation failed. Please check error messages above."
  exit 1
fi

exit 0
