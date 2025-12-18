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

        // Build formatted sections
        String ipSection = formatIpSection(request, clientIp, reversePointer);
        String connectionSection = formatConnectionSection(request);
        String headersSection = formatHeadersSection(request);

        // Build comprehensive diagnostic information for Classic View
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("╔════════════════════════════════════════════════════════════════╗\n");
        diagnostics.append("║               IP ADDRESS DIAGNOSTIC TOOL                       ║\n");
        diagnostics.append("╚════════════════════════════════════════════════════════════════╝\n\n");
        diagnostics.append(ipSection).append("\n\n");
        diagnostics.append(connectionSection).append("\n\n");
        diagnostics.append(headersSection).append("\n\n");
        diagnostics.append(String.format("╭─ Timestamp: %s ─╮\n", java.time.Instant.now().toString()));

        String fullDiagnostics = diagnostics.toString();

        // Build JSON data for Terminal Mode
        String diagnosticJson = buildDiagnosticDataJson(request, clientIp, reversePointer,
                ipSection, connectionSection, headersSection, fullDiagnostics);

        return buildHtmlResponse(fullDiagnostics, diagnosticJson);
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

    private String escapeJson(String str) {
        if (str == null) return "null";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String buildDiagnosticDataJson(HttpServletRequest request, String clientIp, String reversePointer,
                                            String ipSection, String connectionSection, String headersSection,
                                            String fullDiagnostics) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // IP section
        json.append("  \"ip\": {\n");
        json.append(String.format("    \"detected\": \"%s\",\n", escapeJson(clientIp)));
        json.append(String.format("    \"reversePointer\": \"%s\",\n", escapeJson(reversePointer)));

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            json.append(String.format("    \"xForwardedFor\": \"%s\",\n", escapeJson(forwardedFor)));
        }
        json.append(String.format("    \"remoteAddr\": \"%s\",\n", escapeJson(request.getRemoteAddr())));
        json.append(String.format("    \"remoteHost\": \"%s\",\n", escapeJson(request.getRemoteHost())));
        json.append(String.format("    \"remotePort\": %d\n", request.getRemotePort()));
        json.append("  },\n");

        // Connection section
        json.append("  \"connection\": {\n");
        json.append(String.format("    \"protocol\": \"%s\",\n", escapeJson(request.getProtocol())));
        json.append(String.format("    \"method\": \"%s\",\n", escapeJson(request.getMethod())));
        json.append(String.format("    \"scheme\": \"%s\",\n", escapeJson(request.getScheme())));
        json.append(String.format("    \"serverName\": \"%s\",\n", escapeJson(request.getServerName())));
        json.append(String.format("    \"serverPort\": %d,\n", request.getServerPort()));
        json.append(String.format("    \"requestURI\": \"%s\"", escapeJson(request.getRequestURI())));
        if (request.getQueryString() != null) {
            json.append(String.format(",\n    \"queryString\": \"%s\"", escapeJson(request.getQueryString())));
        }
        json.append("\n  },\n");

        // Headers section
        json.append("  \"headers\": {\n");
        var headerNames = request.getHeaderNames();
        boolean first = true;
        while (headerNames.hasMoreElements()) {
            if (!first) json.append(",\n");
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            json.append(String.format("    \"%s\": \"%s\"", escapeJson(headerName), escapeJson(headerValue)));
            first = false;
        }
        json.append("\n  },\n");

        // Formatted sections
        json.append("  \"formatted\": {\n");
        json.append(String.format("    \"ipSection\": \"%s\",\n", escapeJson(ipSection)));
        json.append(String.format("    \"connectionSection\": \"%s\",\n", escapeJson(connectionSection)));
        json.append(String.format("    \"headersSection\": \"%s\",\n", escapeJson(headersSection)));
        json.append(String.format("    \"fullDiagnostics\": \"%s\"\n", escapeJson(fullDiagnostics)));
        json.append("  },\n");

        // Metadata
        json.append("  \"metadata\": {\n");
        json.append(String.format("    \"timestamp\": \"%s\"\n", java.time.Instant.now().toString()));
        json.append("  }\n");

        json.append("}");
        return json.toString();
    }

    private String buildHtmlResponse(String diagnostics, String diagnosticJson) {
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
                    /* View Controls */
                    .view-controls {
                        display: flex;
                        gap: 10px;
                        justify-content: flex-start;
                        margin-bottom: 20px;
                    }
                    .view-toggle {
                        opacity: 0.5;
                    }
                    .view-toggle.active {
                        opacity: 1;
                        background-color: #00ff00;
                        color: #000000;
                    }
                    #toggleTerminal {
                        background-color: #004400;
                        border-color: #00ffff;
                        color: #00ffff;
                        font-weight: bold;
                        opacity: 1;
                        box-shadow: 0 0 8px #00ffff;
                    }
                    #toggleTerminal:hover {
                        background-color: #00ffff;
                        color: #000000;
                        box-shadow: 0 0 15px #00ffff;
                    }
                    #toggleTerminal.active {
                        background-color: #00ffff;
                        color: #000000;
                        border-color: #00ffff;
                    }
                    /* View Containers */
                    .view-container.hidden {
                        display: none;
                    }
                    /* Terminal View */
                    .terminal-output {
                        min-height: 400px;
                        max-height: 70vh;
                        overflow-y: auto;
                        padding: 20px;
                        margin-bottom: 10px;
                        border: 2px solid #00ff00;
                        background-color: #000000;
                    }
                    .terminal-output::-webkit-scrollbar {
                        width: 10px;
                    }
                    .terminal-output::-webkit-scrollbar-track {
                        background: #001100;
                    }
                    .terminal-output::-webkit-scrollbar-thumb {
                        background: #00ff00;
                        border: 1px solid #000000;
                    }
                    .terminal-line {
                        margin-bottom: 5px;
                        font-family: 'Courier New', Courier, monospace;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        color: #00ff00;
                    }
                    .command-echo {
                        color: #00ff00;
                    }
                    .command-text {
                        font-weight: bold;
                    }
                    .response-error {
                        color: #ff0000;
                    }
                    .response-info {
                        color: #00ffff;
                    }
                    .terminal-input-container {
                        display: flex;
                        align-items: center;
                        border: 2px solid #00ff00;
                        padding: 10px;
                        background-color: #001100;
                    }
                    .prompt {
                        color: #00ff00;
                        font-family: 'Courier New', Courier, monospace;
                        margin-right: 5px;
                        font-weight: bold;
                    }
                    .terminal-input {
                        flex: 1;
                        background: transparent;
                        border: none;
                        color: #00ff00;
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 14px;
                        outline: none;
                        caret-color: transparent;
                    }
                    .cursor {
                        color: #00ff00;
                        font-family: 'Courier New', Courier, monospace;
                        animation: blink 1s infinite;
                        margin-left: 2px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="view-controls">
                        <button id="toggleClassic" class="view-toggle active">CLASSIC VIEW</button>
                        <button id="toggleTerminal" class="view-toggle">TERMINAL MODE</button>
                    </div>

                    <div id="classicView" class="view-container active">
                        <div class="controls">
                            <button onclick="copyToClipboard()">⎘ COPY TO CLIPBOARD</button>
                            <button onclick="window.location.reload()">↻ REFRESH</button>
                            <button onclick="downloadAsText()">⇓ DOWNLOAD</button>
                        </div>
                        <pre id="diagnostics">DIAGNOSTICS_PLACEHOLDER</pre>
                    </div>

                    <div id="terminalView" class="view-container hidden">
                        <div id="terminalOutput" class="terminal-output"></div>
                        <div class="terminal-input-container">
                            <span class="prompt">publicip&gt; </span>
                            <input type="text" id="terminalInput" class="terminal-input"
                                   autocomplete="off" spellcheck="false" autofocus>
                            <span class="cursor">_</span>
                        </div>
                    </div>

                    <div class="footer">
                        <span class="blink">▓</span> PublicIP.org - Network Diagnostic Tool <span class="blink">▓</span><br>
                        Powered by Spring Boot | <a href="https://github.com/anthropics/claude-code" target="_blank">Built with Claude Code</a>
                    </div>
                </div>
                <script id="diagnosticDataScript" type="application/json">
JSON_DATA_PLACEHOLDER
                </script>
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

                    // ViewToggle Class
                    class ViewToggle {
                        constructor() {
                            this.classicView = document.getElementById('classicView');
                            this.terminalView = document.getElementById('terminalView');
                            this.classicBtn = document.getElementById('toggleClassic');
                            this.terminalBtn = document.getElementById('toggleTerminal');

                            this.classicBtn.addEventListener('click', () => this.switchTo('classic'));
                            this.terminalBtn.addEventListener('click', () => this.switchTo('terminal'));
                        }

                        switchTo(view) {
                            if (view === 'classic') {
                                this.classicView.classList.remove('hidden');
                                this.terminalView.classList.add('hidden');
                                this.classicBtn.classList.add('active');
                                this.terminalBtn.classList.remove('active');
                            } else {
                                this.terminalView.classList.remove('hidden');
                                this.classicView.classList.add('hidden');
                                this.terminalBtn.classList.add('active');
                                this.classicBtn.classList.remove('active');
                                if (window.terminalInstance) {
                                    window.terminalInstance.focusInput();
                                }
                            }
                        }
                    }

                    // Terminal Class
                    class Terminal {
                        constructor(data) {
                            this.data = data;
                            this.history = [];
                            this.historyIndex = -1;
                            this.output = document.getElementById('terminalOutput');
                            this.input = document.getElementById('terminalInput');
                            this.parser = new CommandParser(this.data);
                            this.renderer = new TerminalRenderer(this.output);

                            this.init();
                        }

                        init() {
                            this.input.addEventListener('keydown', (e) => this.handleKeyDown(e));
                            this.displayWelcome();
                        }

                        handleKeyDown(e) {
                            if (e.key === 'Enter') {
                                e.preventDefault();
                                this.executeCommand();
                            } else if (e.key === 'ArrowUp') {
                                e.preventDefault();
                                this.navigateHistory(-1);
                            } else if (e.key === 'ArrowDown') {
                                e.preventDefault();
                                this.navigateHistory(1);
                            } else if (e.key === 'Tab') {
                                e.preventDefault();
                                this.handleTabComplete();
                            } else if (e.key === 'c' && e.ctrlKey) {
                                e.preventDefault();
                                this.input.value = '';
                            } else if (e.key === 'l' && e.ctrlKey) {
                                e.preventDefault();
                                this.output.innerHTML = '';
                                this.displayWelcome();
                            }
                        }

                        executeCommand() {
                            const cmd = this.input.value.trim();
                            if (cmd) {
                                this.history.push(cmd);
                                this.historyIndex = this.history.length;
                                this.renderer.renderCommand(cmd);
                                const result = this.parser.execute(cmd);
                                if (result.type === 'clear') {
                                    this.output.innerHTML = '';
                                    this.displayWelcome();
                                } else {
                                    this.renderer.renderResponse(result);
                                }
                                this.input.value = '';
                                this.scrollToBottom();
                            }
                        }

                        navigateHistory(dir) {
                            const newIndex = this.historyIndex + dir;
                            if (newIndex >= 0 && newIndex < this.history.length) {
                                this.historyIndex = newIndex;
                                this.input.value = this.history[this.historyIndex];
                            } else if (newIndex === this.history.length) {
                                this.historyIndex = newIndex;
                                this.input.value = '';
                            }
                        }

                        handleTabComplete() {
                            const partial = this.input.value.toLowerCase();
                            const commands = this.parser.getCommands();
                            const matches = commands.filter(c => c.startsWith(partial));
                            if (matches.length === 1) {
                                this.input.value = matches[0];
                            }
                        }

                        displayWelcome() {
                            this.renderer.renderText(`╔════════════════════════════════════════════════════════════════╗
║               IP ADDRESS DIAGNOSTIC TOOL                       ║
╚════════════════════════════════════════════════════════════════╝

Your IP: ` + this.data.ip.detected + `

Type 'help' for available commands.
`);
                        }

                        focusInput() {
                            this.input.focus();
                        }

                        scrollToBottom() {
                            this.output.scrollTop = this.output.scrollHeight;
                        }
                    }

                    // CommandParser Class
                    class CommandParser {
                        constructor(data) {
                            this.data = data;
                            this.commands = {
                                ip: () => ({ type: 'success', content: this.data.formatted.ipSection, animated: true }),
                                headers: () => ({ type: 'success', content: this.data.formatted.headersSection, animated: true }),
                                connection: () => ({ type: 'success', content: this.data.formatted.connectionSection, animated: true }),
                                all: () => ({ type: 'success', content: this.data.formatted.fullDiagnostics, animated: false }),
                                clear: () => ({ type: 'clear' }),
                                help: () => this.showHelp(),
                                download: () => this.downloadSession(),
                                history: () => this.showHistory()
                            };
                        }

                        execute(cmdLine) {
                            const [cmd] = cmdLine.toLowerCase().trim().split(/\\s+/);
                            const cmdFn = this.commands[cmd];
                            return cmdFn ? cmdFn() : {
                                type: 'error',
                                content: `Command not found: ` + cmd + `. Type 'help' for available commands.`,
                                animated: false
                            };
                        }

                        showHelp() {
                            return {
                                type: 'info',
                                content: `
Available Commands:

  ip           - Show IP address and reverse pointer
  headers      - Display all HTTP headers
  connection   - Show connection details
  all          - Display complete diagnostics
  clear        - Clear the terminal screen
  help         - Show this help message
  download     - Download session as text file
  history      - Show command history

Keyboard Shortcuts:
  Ctrl+C       - Clear current input
  Ctrl+L       - Clear screen
  ↑/↓          - Navigate command history
  Tab          - Auto-complete command
`,
                                animated: false
                            };
                        }

                        downloadSession() {
                            const content = document.getElementById('terminalOutput').innerText;
                            const blob = new Blob([content], { type: 'text/plain' });
                            const url = window.URL.createObjectURL(blob);
                            const a = document.createElement('a');
                            a.href = url;
                            a.download = 'terminal-session-' + new Date().toISOString() + '.txt';
                            document.body.appendChild(a);
                            a.click();
                            document.body.removeChild(a);
                            window.URL.revokeObjectURL(url);
                            return { type: 'success', content: 'Session downloaded.', animated: false };
                        }

                        showHistory() {
                            const hist = window.terminalInstance.history;
                            if (hist.length === 0) {
                                return { type: 'info', content: 'No command history.', animated: false };
                            }
                            let content = '\\nCommand History:\\n\\n';
                            hist.forEach((cmd, i) => {
                                content += '  ' + (i+1).toString().padStart(3) + '.  ' + cmd + '\\n';
                            });
                            return { type: 'info', content, animated: false };
                        }

                        getCommands() {
                            return Object.keys(this.commands);
                        }
                    }

                    // TerminalRenderer Class
                    class TerminalRenderer {
                        constructor(output) {
                            this.output = output;
                        }

                        renderCommand(cmd) {
                            const div = document.createElement('div');
                            div.className = 'terminal-line command-echo';
                            div.innerHTML = `<span class="prompt">publicip&gt; </span><span class="command-text">` + this.escape(cmd) + `</span>`;
                            this.output.appendChild(div);
                        }

                        renderResponse(result) {
                            const div = document.createElement('div');
                            div.className = `terminal-line response response-` + result.type;

                            if (result.animated && result.content.length < 500) {
                                this.animateText(div, result.content);
                            } else {
                                div.textContent = result.content;
                                this.output.appendChild(div);
                            }
                        }

                        renderText(text) {
                            const div = document.createElement('div');
                            div.className = 'terminal-line';
                            div.textContent = text;
                            this.output.appendChild(div);
                        }

                        animateText(element, text) {
                            this.output.appendChild(element);
                            let i = 0;
                            const interval = setInterval(() => {
                                if (i < text.length) {
                                    element.textContent += text[i++];
                                } else {
                                    clearInterval(interval);
                                }
                            }, 1);
                        }

                        escape(text) {
                            const div = document.createElement('div');
                            div.textContent = text;
                            return div.innerHTML;
                        }
                    }

                    // Initialize on page load
                    document.addEventListener('DOMContentLoaded', () => {
                        const dataScript = document.getElementById('diagnosticDataScript');
                        const diagnosticData = JSON.parse(dataScript.textContent);

                        new ViewToggle();
                        window.terminalInstance = new Terminal(diagnosticData);
                    });
                </script>
            </body>
            </html>
            """;
        return htmlTemplate.replace("DIAGNOSTICS_PLACEHOLDER", diagnostics)
                .replace("JSON_DATA_PLACEHOLDER", diagnosticJson);
    }

    private String formatIpSection(HttpServletRequest request, String clientIp, String reversePointer) {
        StringBuilder section = new StringBuilder();
        section.append("┌─ IP INFORMATION ────────────────────────────────────────────┐\n");
        section.append(String.format("│ Detected IP:        %s\n", clientIp));
        section.append(String.format("│ Reverse Pointer:    %s\n", reversePointer));

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            section.append(String.format("│ X-Forwarded-For:    %s\n", forwardedFor));
        }
        section.append(String.format("│ Remote Address:     %s\n", request.getRemoteAddr()));
        section.append(String.format("│ Remote Host:        %s\n", request.getRemoteHost()));
        section.append(String.format("│ Remote Port:        %d\n", request.getRemotePort()));
        section.append("└─────────────────────────────────────────────────────────────┘");
        return section.toString();
    }

    private String formatConnectionSection(HttpServletRequest request) {
        StringBuilder section = new StringBuilder();
        section.append("┌─ CONNECTION DETAILS ────────────────────────────────────────┐\n");
        section.append(String.format("│ Protocol:           %s\n", request.getProtocol()));
        section.append(String.format("│ Method:             %s\n", request.getMethod()));
        section.append(String.format("│ Scheme:             %s\n", request.getScheme()));
        section.append(String.format("│ Server Name:        %s\n", request.getServerName()));
        section.append(String.format("│ Server Port:        %d\n", request.getServerPort()));
        section.append(String.format("│ Request URI:        %s\n", request.getRequestURI()));
        if (request.getQueryString() != null) {
            section.append(String.format("│ Query String:       %s\n", request.getQueryString()));
        }
        section.append("└─────────────────────────────────────────────────────────────┘");
        return section.toString();
    }

    private String formatHeadersSection(HttpServletRequest request) {
        StringBuilder section = new StringBuilder();
        section.append("┌─ HTTP HEADERS ──────────────────────────────────────────────┐\n");
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            section.append(String.format("│ %-18s: %s\n", headerName, headerValue));
        }
        section.append("└─────────────────────────────────────────────────────────────┘");
        return section.toString();
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
