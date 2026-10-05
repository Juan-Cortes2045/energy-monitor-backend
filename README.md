# Energy Monitor Backend

Backend for Energy Monitor, built with Spring Boot 4.1.1 on Java 21.

## Requirements

- Java 21 or later
- Docker and Docker Compose, for the MySQL 8 database
- OpenSSL, to generate the RSA key pair
- No Maven install needed: use the bundled `./mvnw` (`mvnw.cmd` on Windows)

## 1. Database credentials

Copy the example file and set your own passwords:

```bash
cp .env.example .env
```

```powershell
Copy-Item .env.example .env
```

`.env` is gitignored and must never be committed.

## 2. Start the database

```bash
docker compose up -d
docker compose ps
```

Wait until the container reports `healthy`. It creates the `energy_monitor`
schema and the application user from the values in `.env`.

## 3. Application configuration

Copy the example profile and set the same password you put in `.env`:

```bash
cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml
```

```powershell
Copy-Item src\main\resources\application-local.yaml.example src\main\resources\application-local.yaml
```

This file is gitignored as well.

## 4. RSA keys for JWT (RS256)

Access tokens are signed with RS256, so the application needs an RSA key pair.
The `.pem` files are gitignored and must never be committed.

**Linux and macOS:**

```bash
mkdir -p ~/.energy-monitor/keys
chmod 700 ~/.energy-monitor/keys

openssl genpkey -algorithm RSA -out ~/.energy-monitor/keys/private.pem -pkeyopt rsa_keygen_bits:2048
chmod 600 ~/.energy-monitor/keys/private.pem

openssl rsa -in ~/.energy-monitor/keys/private.pem -pubout -out ~/.energy-monitor/keys/public.pem
chmod 644 ~/.energy-monitor/keys/public.pem
```

**Windows (PowerShell):**

```powershell
New-Item -ItemType Directory -Force "$HOME\.energy-monitor\keys"

openssl genpkey -algorithm RSA -out "$HOME\.energy-monitor\keys\private.pem" -pkeyopt rsa_keygen_bits:2048
openssl rsa -in "$HOME\.energy-monitor\keys\private.pem" -pubout -out "$HOME\.energy-monitor\keys\public.pem"
```

On Windows, restrict access to the private key through the file properties, or
keep it in a user-only folder. OpenSSL ships with Git for Windows; if it is not
on your `PATH`, run these from Git Bash.

PKCS#1 keys (`openssl genrsa` with `openssl rsa -RSAPublicKey_out`) are also
accepted, but PKCS#8 is the recommended format.

### Key paths

Point the application at the key pair:

```bash
export SECURITY_JWT_PRIVATE_KEY_PATH=$HOME/.energy-monitor/keys/private.pem
export SECURITY_JWT_PUBLIC_KEY_PATH=$HOME/.energy-monitor/keys/public.pem
```

```powershell
$env:SECURITY_JWT_PRIVATE_KEY_PATH = "$HOME\.energy-monitor\keys\private.pem"
$env:SECURITY_JWT_PUBLIC_KEY_PATH  = "$HOME\.energy-monitor\keys\public.pem"
```

Environment variables last only for the current shell session. To set them once,
add the two properties to `application-local.yaml` instead.

The application refuses to start when a key is missing, unreadable or invalid,
and the error message names the path it could not read.

## 5. Build and run

```bash
./mvnw clean test
./mvnw spring-boot:run
```

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`. Swagger UI is served at
`/swagger-ui.html` and the OpenAPI document at `/v3/api-docs`.

## Useful commands

| Command | Purpose |
|---|---|
| `docker compose stop` | Stop the database, keeping its data |
| `docker compose start` | Start it again |
| `docker compose down -v` | Delete the container and its data, rebuilding the schema from Liquibase on the next run |

Liquibase owns the database schema. Hibernate only validates it and never
creates or alters a table.