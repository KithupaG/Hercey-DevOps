# Hercey

A Java web application built with Jakarta Servlet, Hibernate ORM, and Jersey REST API.

## Tech Stack

- **Java 17** + **Maven** (WAR packaging)
- **Tomcat Embedded 10.1** (servlet container)
- **Jakarta Servlet 6.1** + **Jersey 3.1** (REST)
- **Hibernate ORM 6.1** + **MySQL**
- **OpenPDF**, **Jakarta Mail**, **GSON**

## Build & Run

```bash
mvn clean package
mvn tomcat7:run-war
```

## Cloud Tunnel
```cloudflared tunnel --config C:\Users\nimut\.cloudflared\config.yml run hercey
```bash
