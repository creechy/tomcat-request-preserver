# Tomcat Preserver Valve

A lightweight Apache Tomcat Valve that captures and preserves original request headers and server values into request attributes before downstream valves (such as Tomcat's `RemoteIpValve`) modify or strip them.

---

## Background & Purpose

When configuring Tomcat behind reverse proxies, load balancers, or CDNs, valves like `org.apache.catalina.valves.RemoteIpValve` are often used to replace client IP addresses and protocol schemes with values from upstream proxy headers (e.g., `X-Forwarded-For`, `X-Forwarded-Proto`).

However, `RemoteIpValve` and similar valves may modify or consume these headers, making the original raw values unavailable later in the request lifecycle (such as within applications, filters, or access loggers).

**`PreserveRequestValve`** solves this by capturing configured request headers and Tomcat virtual host names early in the pipeline and copying them into standard Servlet request attributes.

---

## Features

- **Header Preservation**: Copies any incoming HTTP request header (e.g., `x-forwarded-for`, `x-real-ip`, `authorization`, etc.) to a request attribute.
- **Virtual Host Name Preservation**: Special support for `request-host-name` to preserve Tomcat's virtual host (`request.getHost().getName()`).
- **Configurable Prefix**: Prepend custom prefixes (defaults to `raw`) to prevent attribute name collisions.
- **Zero Runtime Dependencies**: Drop-in JAR requiring only standard Tomcat libraries.

---

## Requirements

- **Java**: 17+
- **Apache Tomcat**: 9.x (uses `javax.servlet` API)

---

## Building

Build the plain JAR using the Gradle wrapper:

```bash
./gradlew build
```

The compiled JAR will be generated at:
```
build/libs/tomcat-preserver-1.0.0-SNAPSHOT.jar
```

---

## Installation

1. Copy the built JAR into Tomcat's global library directory:
   ```bash
   cp build/libs/tomcat-preserver-1.0.0-SNAPSHOT.jar $CATALINA_HOME/lib/
   ```
2. Configure the valve in Tomcat's `conf/server.xml`.

---

## Configuration

Add the `<Valve>` entry to your `conf/server.xml` inside `<Engine>`, `<Host>`, or `<Context>`.

> **Important**: Place `PreserveRequestValve` **before** any valves that modify request headers (such as `RemoteIpValve`).

### Example: `server.xml`

```xml
<Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true">

  <!-- 1. Preserve original request headers/host before modification -->
  <Valve className="org.fakebelieve.tomcat.PreserveRequestValve"
         prefix="raw"
         names="request-host-name,x-forwarded-for,x-forwarded-proto" />

  <!-- 2. Remote IP valve processes and modifies request headers -->
  <Valve className="org.apache.catalina.valves.RemoteIpValve"
         remoteIpHeader="x-forwarded-for"
         protocolHeader="x-forwarded-proto" />

  <!-- 3. Access log valve can now reference the preserved attributes -->
  <Valve className="org.apache.catalina.valves.AccessLogValve"
         directory="logs"
         prefix="localhost_access_log"
         suffix=".txt"
         pattern="%h %l %u %t &quot;%r&quot; %s %b &quot;%{raw-x-forwarded-for}r&quot; &quot;%{raw-request-host-name}r&quot;" />

</Host>
```

### Configuration Options

| Attribute | Type | Default | Description |
|---|---|---|---|
| `prefix` | `String` | `raw` | The prefix prepended to request attribute names (e.g., `<prefix>-<name>`). |
| `names` | `String` | `request-host-name,x-forwarded-for` | Comma-separated list of header names and/or special tokens to capture. Case-insensitive. |

### Special Name Tokens

- **`request-host-name`**: Captures `request.getHost().getName()` and stores it under `<prefix>-request-host-name` (e.g., `raw-request-host-name`).

---

## Accessing Preserved Values

### 1. In Access Logs (`AccessLogValve`)

Use the `%{xxx}r` pattern token in `AccessLogValve` to print Servlet request attributes:

```xml
pattern="%h %t &quot;%r&quot; %s &quot;%{raw-x-forwarded-for}r&quot;"
```

### 2. In Servlets / Filters

Retrieve preserved attributes programmatically:

```java
String rawXForwardedFor = (String) request.getAttribute("raw-x-forwarded-for");
String rawHostName = (String) request.getAttribute("raw-request-host-name");
```

---

## Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with `palantirJavaFormat`.

To check formatting:
```bash
./gradlew spotlessCheck
```

To apply formatting:
```bash
./gradlew spotlessApply
```
