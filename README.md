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

The application needs to be told where those two files are. There are two ways, and
they are equivalent.

**Once per shell session:**

```bash
export SECURITY_JWT_PRIVATE_KEY_PATH=$HOME/.energy-monitor/keys/private.pem
export SECURITY_JWT_PUBLIC_KEY_PATH=$HOME/.energy-monitor/keys/public.pem
```

```powershell
$env:SECURITY_JWT_PRIVATE_KEY_PATH = "$HOME\.energy-monitor\keys\private.pem"
$env:SECURITY_JWT_PUBLIC_KEY_PATH  = "$HOME\.energy-monitor\keys\public.pem"
```

**Permanently, in `application-local.yaml`** — this is what survives closing the terminal,
so it is the one to use if you run the application often:

```yaml
security:
  jwt:
    private-key-path: /home/you/.energy-monitor/keys/private.pem
    public-key-path: /home/you/.energy-monitor/keys/public.pem
```

Write the path **absolute**. Java does not expand `~`, so `~/.energy-monitor/keys/private.pem`
is taken literally and the key comes back unreadable. On Windows use `C:\Users\you\.energy-monitor\keys\private.pem`.

In the copy created from `application-local.yaml.example` the two properties default to empty
and read from those environment variables, so leaving them empty there keeps both routes open.

The application refuses to start when a key is missing, unreadable or invalid,
and the error message names the path it could not read.

## 5. Email delivery (password recovery and email verification)

Password recovery and email verification mails are sent over SMTP. Every credential comes from the
environment; none is ever written into a committed file.

### The recovery code, and why it is short

`POST /password/forgot` mails a **six-digit code**, valid for 15 minutes and usable
once. Six digits is a usability decision — a person has to read it off a screen and
type it back — and on its own it would be no defence at all, about 20 bits. Two
other things carry the weight:

- **A pepper.** What is stored in `password_reset_token.reset_token_hash` is
  `HMAC-SHA256(code, pepper)`, not a plain digest. A plain digest of a six-digit
  code can be reversed from a database dump in seconds: a million candidates, all
  cheap to hash. The pepper lives in the configuration, not in the table, so a
  leaked dump yields nothing usable.
- **A rate limit.** Ten redemption attempts per caller address per 15 minutes,
  counted whether the code presented was right or wrong. The endpoint answers `429`
  with `Retry-After` past that, which is what makes a small space survivable.

One further rule does a lot of quiet work: **issuing a code consumes whatever codes
the account already held.** With several outstanding, the rate limit would bound
guessing only after being multiplied by how many an attacker could first arrange to
have.

Generate the pepper with `openssl rand -hex 32` and set it as `security.reset.pepper`
in `application-local.yaml`. The application refuses to start without it, the same
way it refuses to start without a signing key. Changing it invalidates every code
already issued, and codes issued before the peppered digest was introduced can no
longer be redeemed.

What was deliberately **not** done: BCrypt. It is the obvious way to make a small
secret expensive, and it is wrong here for a structural reason — it is salted, so the
same code hashes differently every time, and redemption resolves a code to its row
*by* hashing the presented value. With BCrypt there would be nothing stable to look
up.

Known limits, stated plainly: the attempt counter lives in memory, so it is per
instance, resets on restart, and cannot be shared between replicas. Behind a load
balancer, add a rate limiter there too. And because the limit is keyed by address,
everyone behind one NAT shares it, which is why the allowance is generous enough to
absorb an office.

### The email verification code

Registering mails a six-digit code to the new address. `POST /api/v1/auth/email/verify`
with `{email, code}` marks the address as verified; `POST /api/v1/auth/email/verification/resend`
sends the current code again and always answers `202`, like the recovery request.

The code is derived, not stored: `HMAC-SHA256` with the same pepper, over the account, its
address and the current fifteen-minute window, reduced to six digits. A code is accepted during
its window and the next one, so it lives between 15 and 30 minutes, and changing the address
invalidates it. Guessing is bounded twice: the per-address limit above, and five attempts per
address per window, so spreading guesses over many callers does not help.

An unverified account can still sign in. Verification records that the address works; it does
not gate access.

### Deleting an account

`POST /api/v1/auth/account/delete` with `{password}` deletes the caller's own account after
checking the password (`401` when it is wrong, `204` when done). It is a soft delete: `user` and
`person` get `deleted_at`, the account's sessions are closed and its home memberships removed.
The address is released: the deleted row keeps `deleted+<id_user>@deleted.invalid` instead, so
the same address can register a new account. A
home the account is the only member of is deleted with it. If the account is the only owner of a
home that still has other members, the request is refused with `409` and a readable `message`:
the ownership must be handed over or the members removed first. Both modules write inside one transaction: the security module
publishes `AccountDeleted` and the home module reacts to it, so either everything is deleted or
nothing is. An access token issued before the deletion keeps working until it expires, at most
15 minutes; no refresh token can renew it.

### Gmail app password

Gmail does not accept the account password from an application. Create an app
password instead:

1. Enable two-step verification at
   <https://myaccount.google.com/signinoptions/twosv>.
2. Create the app password at <https://myaccount.google.com/apppasswords>.
   Google shows 16 characters once and never again.
3. Write them without spaces: `abcdefghijklmnop`.

The app password is a secret. It never goes into the code, into git, or into a
chat.

### Configuration

Copy `.env.example` to `.env` in the project root. The application imports it
automatically (`spring.config.import`), and `.env` is already ignored by git.

| Variable | Meaning |
|---|---|
| `MAIL_HOST` | SMTP host, `smtp.gmail.com` by default |
| `MAIL_PORT` | SMTP port, `587` by default |
| `MAIL_USERNAME` | The account that sends; also the owner of the app password |
| `MAIL_PASSWORD` | The 16-character app password, no spaces |
| `MAIL_FROM` | The sender address, same account as `MAIL_USERNAME` |

`.env` is read with the `.properties` format, which has two rules that surprise people:

- **No quotes.** `MAIL_FROM="Energy Monitor <a@b.com>"` puts the quote marks *into* the value,
  so the sender becomes `"Energy Monitor <a@b.com>"` with the quotation marks attached. Write
  `MAIL_FROM=Energy Monitor <a@b.com>` instead.
- **No `$`.** A dollar sign starts a placeholder reference, so a password containing one fails
  to resolve and the application refuses to start. A Gmail app password is letters and digits,
  so this does not affect it.

Because of the first rule, `.env` cannot be `source`d by a shell: `set -a && . ./.env` fails on
the spaces and angle brackets in `MAIL_FROM`. Docker Compose and Spring both parse the file with
their own readers and are unaffected, so this only matters when you want the values in your
current shell, in which case export `MAIL_USERNAME` and `MAIL_PASSWORD` yourself.

The application starts with no mail configured. A send attempt is then recorded
as `FAILED` in the `notification` table with the reason, rather than breaking
the context. That is deliberate: the recovery endpoint answers `202` whether or
not the account exists, so a mail failure must never change what the caller sees.

Every attempt is written to `notification` before the message leaves and closed
afterwards, so `SENT` and `FAILED` are both auditable and a retry is possible.
The message body is never stored: the recovery secret travels in it, and a
column holding it in clear text would defeat keeping only a hash.

### Trying it without a real mailbox

Mailpit catches the messages instead of sending them:

```bash
docker run -d --name mailpit -p 587:1025 -p 8025:8025 \
  -e MP_SMTP_AUTH_ACCEPT_ANY=1 -e MP_SMTP_AUTH_ALLOW_INSECURE=1 axllent/mailpit
```

Port `587` on the host rather than Mailpit's default `1025`, so switching between Mailpit and
Gmail changes only `MAIL_HOST` and the credentials.

```properties
MAIL_HOST=localhost
MAIL_PORT=587
MAIL_USERNAME=dev
MAIL_PASSWORD=dev
MAIL_FROM=Energy Monitor <no-reply@localhost>
```

Messages appear at <http://localhost:8025>.

## 6. Build and run

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

## 7. Running behind a reverse proxy

The application records `request.getRemoteAddr()` as the caller's address and
**ignores `X-Forwarded-For`**. A header the client sets is not evidence of
anything: the password-recovery attempt limiter is keyed on that address, so a
per-request header would hand out a fresh allowance every time and defeat it.

The cost of that choice is that behind a proxy every request looks like it comes
from the proxy, and the recovery rate limit therefore becomes one limit shared by
all users. A deployment in that position must configure the container's trusted
proxy handling, in the profile of that deployment rather than in `application.yaml`:

```properties
server.forward-headers-strategy=native
```

Tomcat's `RemoteIpValve` then validates the header against the internal proxies
it knows about and rewrites `getRemoteAddr()` with the real client address. Do
this in the container rather than in application code: the decision about which
peers may set the header belongs to the deployment's configuration, because only
the deployment knows who sits in front of it.

While that is not configured, expect two visible effects: every audit row shows
the proxy's address, and a burst of recovery attempts from unrelated users counts
against a single allowance.

## 8. Running the tests

```bash
./mvnw test
```

The suite is not hermetic: the persistence tests run against the real database,
because Liquibase owns the schema and Hibernate only validates it. Start it first
(section 2).

Those tests need `SPRING_DATASOURCE_PASSWORD`. A handful of test classes activate
the `test` profile, and `spring.profiles.default: local` only applies when no
profile is active, so under `test` the `application-local.yaml` that holds your
local credentials **is not loaded**. The `test` profile therefore has to be told
the password, and it reads that one variable:

```properties
SPRING_DATASOURCE_PASSWORD=  # same value as MYSQL_PASSWORD
```

Put it in `.env`, which the application imports automatically
(`spring.config.import`), so nothing has to be exported:

```bash
docker compose up -d     # wait until healthy
./mvnw test
```

Exported in the shell it also works, and wins over `.env`. Left unset, the
password resolves to an empty string and the persistence tests fail with
`Access denied for user 'energy_app'` rather than connecting to something they
were not meant to.

Nothing else has to be exported. `application-test.yaml` carries its own RSA key
paths, defaulting to `~/.energy-monitor/keys`, and a **test-only pepper** for the
recovery codes, committed on purpose so every machine hashes identically. That
pepper is confined to this file: outside the `test` profile the pepper is still
required through `SECURITY_RESET_PEPPER`, and the application refuses to start
without it.

## Useful commands

| Command | Purpose |
|---|---|
| `docker compose stop` | Stop the database, keeping its data |
| `docker compose start` | Start it again |
| `docker compose down -v` | Delete the container and its data, rebuilding the schema from Liquibase on the next run |

Liquibase owns the database schema. Hibernate only validates it and never
creates or alters a table.