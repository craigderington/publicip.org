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
        String ip = request.getRemoteAddr();
        try {
            String reversePointer = getReversePointer(ip);
            return """
                <!DOCTYPE html>
                <html>
                <body style="background-color: black; color: white;">
                %s
                </body>
                </html>
                """.formatted(reversePointer);
        } catch (Exception e) {
            return """
                <!DOCTYPE html>
                <html>
                <body style="background-color: black; color: white;">
                Unknown host
                </body>
                </html>
                """;
        }
    }

    private String getReversePointer(String ip) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(ip);
        if (addr instanceof java.net.Inet4Address) {
            // IPv4: Construct x.x.x.x.in-addr.arpa
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
