![banner](https://pub-4e4118614197441ca01a142347434959.r2.dev/Screenshot%202025-05-03%20at%2001.19.16.png)

<h2 align="center">Tarmoqchi - The HTTP Tunneling Tool</h2>

### Feature Overview
- Expose your local server to the internet easily.
- Access your local services from anywhere.
- No need for complex configurations or setups.
- Simple command-line interface.

---

### Installation and updating
<details>
<summary>macOS and linux</summary>

```
curl -fsSL https://github.com/floss-uz-community/tarmoqchi/releases/download/Tarmoqchi-2.0.0/install.sh | sudo bash
```
</details>
<details>
<summary>Windows</summary>
Download the latest exe from the [release page](https://github.com/floss-uz-community/tarmoqchi/releases/tag/Tarmoqchi-2.0.0)
</details>

---

### Usage

First, obtain your personal auth token by logging in at [tarmoqchi.uz](https://tarmoqchi.uz/)
```
tarmoqchi --auth <AUTH_TOKEN>
```
Expose your local service to the internet:
```
tarmoqchi --port <YOUR_PORT>
```
Replace `YOUR_PORT` with the local port number your service is running on (e.g., 8080, 3000, etc.).

You can also specify a custom subdomain:
```
tarmoqchi --port <YOUR_PORT> --sd <YOUR_SUBDOMAIN>
```
Replace `YOUR_SUBDOMAIN` with the desired subdomain name. For example:
```
tarmoqchi --port 8080 --sd mycustomsubdomain
```
<b>You can stop the tunnel by pressing `Ctrl + C` in the terminal.</b>

---

### Other Options
Show version information
```
tarmoqchi --version
```
Show help information
```
tarmoqchi --help
```

![footer-banner](https://pub-4e4118614197441ca01a142347434959.r2.dev/Screenshot%202025-05-03%20at%2001.20.13.png)
