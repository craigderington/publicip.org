package com.example.reverseip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class ReverseipApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReverseipApplication.class, args);
    }
}

@RestController
class ReverseIpController {
    @GetMapping(value = "/", produces = "text/html")
    public String getReverseIp(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String reversePointer;
        try {
            reversePointer = getReversePointer(clientIp);
        } catch (Exception e) {
            reversePointer = "Unable to generate reverse pointer";
        }

        // Build comprehensive diagnostic information
        StringBuilder diagnostics = new StringBuilder();

        // IP Information Section
        diagnostics.append("╔════════════════════════════════════════════════════════════════╗\n");
        diagnostics.append("║               IP ADDRESS DIAGNOSTIC TOOL                       ║\n");
        diagnostics.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        diagnostics.append("┌─ IP INFORMATION ────────────────────────────────────────────┐\n");
        diagnostics.append(String.format("│ Detected IP:        %s\n", clientIp));
        diagnostics.append(String.format("│ Reverse Pointer:    %s\n", reversePointer));

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            diagnostics.append(String.format("│ X-Forwarded-For:    %s\n", forwardedFor));
        }
        diagnostics.append(String.format("│ Remote Address:     %s\n", request.getRemoteAddr()));
        diagnostics.append(String.format("│ Remote Host:        %s\n", request.getRemoteHost()));
        diagnostics.append(String.format("│ Remote Port:        %d\n", request.getRemotePort()));
        diagnostics.append("└─────────────────────────────────────────────────────────────┘\n\n");

        // Connection Information
        diagnostics.append("┌─ CONNECTION DETAILS ────────────────────────────────────────┐\n");
        diagnostics.append(String.format("│ Protocol:           %s\n", request.getProtocol()));
        diagnostics.append(String.format("│ Method:             %s\n", request.getMethod()));
        diagnostics.append(String.format("│ Scheme:             %s\n", request.getScheme()));
        diagnostics.append(String.format("│ Server Name:        %s\n", request.getServerName()));
        diagnostics.append(String.format("│ Server Port:        %d\n", request.getServerPort()));
        diagnostics.append(String.format("│ Request URI:        %s\n", request.getRequestURI()));
        if (request.getQueryString() != null) {
            diagnostics.append(String.format("│ Query String:       %s\n", request.getQueryString()));
        }
        diagnostics.append("└─────────────────────────────────────────────────────────────┘\n\n");

        // HTTP Headers
        diagnostics.append("┌─ HTTP HEADERS ──────────────────────────────────────────────┐\n");
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            diagnostics.append(String.format("│ %-18s: %s\n", headerName, headerValue));
        }
        diagnostics.append("└─────────────────────────────────────────────────────────────┘\n\n");

        // Timestamp
        diagnostics.append(String.format("╭─ Timestamp: %s ─╮\n", java.time.Instant.now().toString()));

        return buildHtmlResponse(diagnostics.toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            // get the first IP in the chain, the client's ip address
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String buildHtmlResponse(String diagnostics) {
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>PublicIP.org - Network Diagnostics</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        background-color: #000000;
                        color: #00ff00;
                        font-family: 'Courier New', Courier, monospace;
                        padding: 20px;
                        line-height: 1.6;
                        min-height: 100vh;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                    }
                    .container {
                        max-width: 900px;
                        width: 100%;
                        margin: 0 auto;
                    }
                    pre {
                        font-family: 'Courier New', Courier, monospace;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        color: #00ff00;
                        text-shadow: 0 0 5px #00ff00;
                        font-size: 14px;
                    }
                    .controls {
                        margin: 20px 0;
                        display: flex;
                        gap: 10px;
                        flex-wrap: wrap;
                    }
                    button {
                        background-color: #003300;
                        color: #00ff00;
                        border: 2px solid #00ff00;
                        padding: 10px 20px;
                        font-family: 'Courier New', Courier, monospace;
                        cursor: pointer;
                        font-size: 14px;
                        transition: all 0.3s;
                    }
                    button:hover {
                        background-color: #00ff00;
                        color: #000000;
                        box-shadow: 0 0 10px #00ff00;
                    }
                    .footer {
                        margin-top: 30px;
                        text-align: center;
                        color: #00aa00;
                        font-size: 12px;
                        border-top: 1px solid #003300;
                        padding-top: 20px;
                    }
                    .blink {
                        animation: blink 1s infinite;
                    }
                    @keyframes blink {
                        0%, 50% { opacity: 1; }
                        25%, 75% { opacity: 0; }
                    }
                    a {
                        color: #00ff00;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                        text-shadow: 0 0 5px #00ff00;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="controls">
                        <button onclick="copyToClipboard()">⎘ COPY TO CLIPBOARD</button>
                        <button onclick="window.location.reload()">↻ REFRESH</button>
                        <button onclick="downloadAsText()">⇓ DOWNLOAD</button>
                    </div>
                    <pre id="diagnostics">DIAGNOSTICS_PLACEHOLDER</pre>
                    <div class="footer">
                        <span class="blink">▓</span> PublicIP.org - Network Diagnostic Tool <span class="blink">▓</span><br>
                        Powered by Spring Boot | <a href="https://github.com/anthropics/claude-code" target="_blank">Built with Claude Code</a>
                    </div>
                </div>
                <script>
                    function copyToClipboard() {
                        const text = document.getElementById('diagnostics').innerText;
                        navigator.clipboard.writeText(text).then(() => {
                            alert('✓ Diagnostics copied to clipboard!');
                        }).catch(err => {
                            console.error('Failed to copy:', err);
                        });
                    }

                    function downloadAsText() {
                        const text = document.getElementById('diagnostics').innerText;
                        const blob = new Blob([text], { type: 'text/plain' });
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = 'network-diagnostics-' + new Date().toISOString() + '.txt';
                        document.body.appendChild(a);
                        a.click();
                        document.body.removeChild(a);
                        window.URL.revokeObjectURL(url);
                    }
                </script>
            </body>
            </html>
            """;
        return htmlTemplate.replace("DIAGNOSTICS_PLACEHOLDER", diagnostics);
    }

    private String getReversePointer(String ip) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(ip);
        if (addr instanceof java.net.Inet4Address) {
            // IPv4: Construct x.x.x.x.in-addr.arpa
            System.out.println(ip);
            String[] octets = ip.split("\\.");
            return String.format("%s.%s.%s.%s.in-addr.arpa",
                    octets[3], octets[2], octets[1], octets[0]);
        } else if (addr instanceof java.net.Inet6Address) {
            // IPv6: Construct reverse pointer
            String ipv6 = addr.getHostAddress().replace(":", "");
            StringBuilder reverse = new StringBuilder();
            for (int i = ipv6.length() - 1; i >= 0; i--) {
                reverse.append(ipv6.charAt(i)).append(".");
            }
            return reverse.append("ip6.arpa").toString();
        }
        return "Invalid IP";
    }
}
