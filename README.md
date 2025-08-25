### PublicIP.org

Reverse IP Spring Boot Application

#### Overview

A simple Spring Boot application that returns the reverse DNS pointer (PTR record) for the client's IPv4 or IPv6 address.

#### Features

Detects client's IP address (IPv4 or IPv6) from HTTP request.
Returns reverse DNS pointer (e.g., x.x.x.x.in-addr.arpa for IPv4, or ...ip6.arpa for IPv6).
Renders result in a minimal HTML page (black background, white text, no header/footer).

#### Requirements

Java 21
Maven or Gradle
Spring Boot 3.3.x

#### Setup

Create Project:

Use [Spring Start](https://start.spring.io) to generate a Spring Boot project.

Select:
Java 21
Spring Boot 3.3.x
Dependency: Spring Web

Clone the repository or Download and extract the project.

Build and Run:

#### For Maven:
```
mvn clean install
mvn spring-boot:run
```

#### For Gradle:
```
gradle build
gradle bootRun
```
##### Access:
Open http://localhost:8080 in a browser to see the reverse DNS pointer.

##### Dependencies
Spring Web (included via spring-boot-starter-web)
No additional dependencies are required.

```
File Structure
src/
└── main/
└── java/
└── com/example/reverseip/
└── ReverseIpApplication.java
```

#### Usage

Send an HTTP GET request to /.
The application detects the client's IP address and returns its reverse DNS pointer.
Example outputs:

```
IPv4 (e.g., 192.168.1.1): 1.1.168.192.in-addr.arpa
IPv6 (e.g., 2001:0db8::1): 1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa
Invalid IP: Unknown host
```

#### Notes
Uses java.net.InetAddress for IP parsing (Java standard library).
No external DNS lookup performed; reverse pointer is constructed deterministically.
Ensure your environment supports Java 21.

