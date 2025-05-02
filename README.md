![banner](https://pub-4e4118614197441ca01a142347434959.r2.dev/Screenshot%202025-05-03%20at%2001.19.16.png)

# Tarmoqchi - The HTTP Tunneling Tool 

Create tunnels to expose your local services to the internet.

![video](https://pub-4e4118614197441ca01a142347434959.r2.dev/Nomsizdizayn-ezgif.com-video-to-gif-converter.gif)

## Installation and update

### Quick Install on MacOS/Linux

```bash
curl -fsSL https://github.com/jamshid-elmurodov/tarmoqchi/releases/download/Tarmoqchi-1.1.0/install.sh | sudo bash
```

### Manual Installation on Windows

1. Download the latest release from the [releases page]()

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
![footer-banner](https://pub-4e4118614197441ca01a142347434959.r2.dev/Screenshot%202025-05-03%20at%2001.20.13.png)
