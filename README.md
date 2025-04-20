# Tarmoqchi - The Lightweight Tunneling Tool 

HTTP tunneling. Create tunnels to expose your local services to the internet.

![Tarmoqchi Logo](https://pub-efb2bbde7206420692920475ba73046f.r2.dev/logo.png)

## Installation

### Quick Install on MacOS/Linux

```bash
curl -fsSL https://github.com/jamshid-elmurodov/tarmoqchi/releases/download/Tarmoqchi-1.0.0/install.sh | sudo bash
```

### Manual Installation on Windows

1. Download the latest release from the [releases page](https://github.com/jamshid-elmurodov/tarmoqchi/releases/tag/Tarmoqchi-1.0.0)

## Usage

### Authentication

Before using Tarmoqchi, you need to authenticate using your auth token:

```bash
tarmoqchi --auth YOUR_AUTH_TOKEN
```

You can get your auth token by visiting [Tarmoqchi.uz](https://tarmoqchi.uz/).

### Creating a Tunnel

To create a tunnel to your local service:

```bash
tarmoqchi --port YOUR_PORT
```

Replace `YOUR_PORT` with the local port number your service is running on (e.g., 8080, 3000, etc.).

### Command Options

```bash
# Show version information
tarmoqchi --version

# Display help information
tarmoqchi --help
```
© 2025 Tarmoqchi. All rights reserved.
