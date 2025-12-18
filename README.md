# PublicIP.org

**Comprehensive Network Diagnostic Tool** - A Spring Boot application that provides detailed network diagnostics with reverse DNS pointer lookup in a terminal-inspired interface.

## 🚀 Features

### Network Diagnostics
- **IP Address Detection** - Automatically detects client IPv4 or IPv6 addresses
- **Reverse DNS Pointer** - Constructs reverse DNS PTR records (e.g., `x.x.x.x.in-addr.arpa` for IPv4, `...ip6.arpa` for IPv6)
- **Connection Details** - Protocol, method, scheme, server information, and request URI
- **HTTP Headers Analysis** - Complete visibility into all incoming request headers
- **Proxy Chain Detection** - Shows X-Forwarded-For chain when behind proxies/load balancers
- **Timestamp Tracking** - Records request timestamp for diagnostics

### Terminal/Hacker Aesthetic
- **Black background** with **bright green text** (#00ff00)
- **Monospace font** (Courier New) for authentic terminal look
- **ASCII art borders** using box-drawing characters
- **Glowing text effects** with CSS shadows
- **Animated blinking cursors** for that retro feel
- **Responsive design** for mobile and desktop

### Interactive Features
- **📋 Copy to Clipboard** - One-click copy of all diagnostic data
- **🔄 Refresh** - Quick page reload
- **💾 Download** - Save diagnostics as timestamped text file

### Security & Deployment
- **Reverse proxy ready** - Designed to run behind Apache/Nginx
- **Localhost binding** - Only listens on 127.0.0.1 for security
- **Proxy header trust** - Correctly handles X-Forwarded-For/X-Forwarded-Proto
- **Secure cookies** - HttpOnly, Secure, SameSite=strict protection

## 📋 Requirements

- **Java 21** or higher
- **Maven 3.6+** (Maven Wrapper included)
- **Spring Boot 3.5.5**

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd publicip.org
```

### 2. Build the Application
```bash
# Using Maven Wrapper (recommended)
./mvnw clean install

# Or using system Maven
mvn clean install
```

### 3. Run the Application
```bash
# Using Maven Wrapper
./mvnw spring-boot:run

# Or using system Maven
mvn spring-boot:run
```

### 4. Access the Application
Open your browser to:
```
http://127.0.0.1:8888/
```

> **Note:** The application binds to localhost only (127.0.0.1) for security. For production deployment, use a reverse proxy.

## 🏗️ Architecture

### Single-File Application
All application logic is contained in `ReverseipApplication.java`:
- **Main Application Class** - Spring Boot entry point
- **ReverseIpController** - Handles HTTP requests at `/`
- **IP Detection Logic** - Prioritizes X-Forwarded-For, falls back to remote address
- **Reverse Pointer Construction** - Deterministic generation (no DNS lookups)

### IP Detection Priority
1. Checks `X-Forwarded-For` header (for requests behind proxies)
2. Extracts first IP in chain (original client IP)
3. Falls back to `request.getRemoteAddr()` if no header present

### Reverse Pointer Generation
- **IPv4**: Reverses octets → `192.168.1.1` becomes `1.1.168.192.in-addr.arpa`
- **IPv6**: Expands address, reverses hex digits with dots → `...ip6.arpa`
- No actual DNS lookups performed - purely algorithmic

## ⚙️ Configuration

Configuration is in `src/main/resources/application.properties`:

### Server Settings
```properties
server.address=127.0.0.1
server.port=8888
```

### Proxy Header Trust (CRITICAL)
```properties
server.forward-headers-strategy=NATIVE
server.tomcat.remoteip.remote-ip-header=X-Forwarded-For
server.tomcat.remoteip.protocol-header=X-Forwarded-Proto
server.tomcat.remoteip.internal-proxies=127\.0\.0\.1
```

### Session Security
```properties
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
```

## 🚀 Production Deployment

### Apache Reverse Proxy Example
```apache
<VirtualHost *:443>
    ServerName publicip.org

    SSLEngine on
    SSLCertificateFile /path/to/cert.pem
    SSLCertificateKeyFile /path/to/key.pem

    ProxyPreserveHost On
    ProxyPass / http://127.0.0.1:8888/
    ProxyPassReverse / http://127.0.0.1:8888/

    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-For "%{REMOTE_ADDR}s"
</VirtualHost>
```

### Nginx Reverse Proxy Example
```nginx
server {
    listen 443 ssl;
    server_name publicip.org;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://127.0.0.1:8888;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Running as a Service (systemd)
Create `/etc/systemd/system/publicip.service`:
```ini
[Unit]
Description=PublicIP.org Network Diagnostic Tool
After=network.target

[Service]
Type=simple
User=publicip
WorkingDirectory=/opt/publicip.org
ExecStart=/opt/publicip.org/mvnw spring-boot:run
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable publicip.service
sudo systemctl start publicip.service
```

## 📁 Project Structure

```
publicip.org/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/reverseip/
│   │   │       └── ReverseipApplication.java    # Main application + controller
│   │   └── resources/
│   │       └── application.properties           # Configuration
│   └── test/
│       └── java/
│           └── com/example/reverseip/
│               └── ReverseipApplicationTests.java
├── pom.xml                                      # Maven dependencies
├── CLAUDE.md                                    # AI assistant guidance
├── README.md                                    # This file
└── LICENSE                                      # Apache License 2.0
```

## 🧪 Development

### Run Tests
```bash
./mvnw test
```

### Package Application
```bash
./mvnw package
```
The executable JAR will be in `target/reverseip-0.0.1-SNAPSHOT.jar`

### Run Packaged Application
```bash
java -jar target/reverseip-0.0.1-SNAPSHOT.jar
```

## 🎨 Customization

### Modify Color Scheme
Edit the `<style>` section in `ReverseipApplication.java` (`buildHtmlResponse` method):
```java
color: #00ff00;  // Green text
background-color: #000000;  // Black background
```

### Add Custom Headers
In the `getReverseIp` method, add to the `diagnostics` StringBuilder:
```java
diagnostics.append(String.format("│ Custom Header:      %s\n",
    request.getHeader("Custom-Header")));
```

## 📊 Example Output

```
╔════════════════════════════════════════════════════════════════╗
║               IP ADDRESS DIAGNOSTIC TOOL                       ║
╚════════════════════════════════════════════════════════════════╝

┌─ IP INFORMATION ────────────────────────────────────────────┐
│ Detected IP:        203.0.113.42
│ Reverse Pointer:    42.113.0.203.in-addr.arpa
│ X-Forwarded-For:    203.0.113.42
│ Remote Address:     127.0.0.1
│ Remote Host:        localhost
│ Remote Port:        54321
└─────────────────────────────────────────────────────────────┘

┌─ CONNECTION DETAILS ────────────────────────────────────────┐
│ Protocol:           HTTP/1.1
│ Method:             GET
│ Scheme:             https
│ Server Name:        publicip.org
│ Server Port:        443
│ Request URI:        /
└─────────────────────────────────────────────────────────────┘

┌─ HTTP HEADERS ──────────────────────────────────────────────┐
│ host              : publicip.org
│ user-agent        : Mozilla/5.0...
│ accept            : text/html...
│ x-forwarded-for   : 203.0.113.42
│ x-forwarded-proto : https
└─────────────────────────────────────────────────────────────┘

╭─ Timestamp: 2025-12-18T14:58:00.000Z ─╮
```

## 🔒 Security Considerations

- Application binds to **127.0.0.1 only** - not accessible from external networks
- **Reverse proxy required** for production deployment
- **X-Forwarded-For validation** - only trusts internal proxy (127.0.0.1)
- **Secure cookies** - HTTPS-only, HttpOnly, SameSite protection
- **No external dependencies** - minimal attack surface
- **No DNS lookups** - prevents DNS-based attacks
- **Input validation** - uses Java's InetAddress for safe IP parsing

## 📝 License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.boot.org/)
- Developed with assistance from [Claude Code](https://github.com/anthropics/claude-code)
- Inspired by classic terminal aesthetics and network diagnostic tools

## 📞 Support

For issues, questions, or suggestions, please open an issue in the GitHub repository.

---

**Made with ❤️ for network diagnostics and terminal aesthetics**
