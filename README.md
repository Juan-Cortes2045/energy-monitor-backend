# Energy Monitor Backend

Backend for Energy Monitor, built with Spring Boot 4.1.1 on Java 21.

## Requirements

- Java 21 or later
- Docker and Docker Compose, for MySQL 8 and the MQTT broker
- OpenSSL, to generate the RSA key pair, the VAPID keys and the broker's TLS certificate
- No Maven install needed: use the bundled `./mvnw` (`mvnw.cmd` on Windows)

## Quick start on a new machine

The whole system runs on one PC in development: MySQL and the backend (this repository), the
MQTT broker (`devices/docker`), the web app (`frontend/energy-monitor-web`) and the ESP32 on the
same Wi-Fi. In order:

| # | Step | Where |
|---|---|---|
| 1 | `cp .env.example .env` and fill every `CHANGE_ME` | section 1 |
| 2 | `docker compose up -d` (MySQL) | section 2 |
| 3 | `cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml` | section 3 |
| 4 | Generate the RSA key pair for JWT | section 4 |
| 5 | Mail account (Gmail app password) | section 5 |
| 6 | `./mvnw spring-boot:run` → `http://localhost:8080/actuator/health` says `UP` | section 6 |
| 7 | MQTT broker: TLS certificate, firewall rule, `docker compose up -d` in `devices/docker` | section 9 |
| 8 | Optional: VAPID keys for browser push | section 10 |
| 9 | Web app: `npm install`, `npm run dev` | `frontend/energy-monitor-web/README.md` |
| 10 | Firmware: flash the ESP32 and link it from the web | `devices/README.md` |

The backend has to be up before the broker accepts any connection: Mosquitto asks it to
authenticate every client, including the backend itself (it retries every 10 s on its own).

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

## 9. MQTT broker and devices

Devices (ESP32 + PZEM-004T) publish telemetry over **MQTT with TLS** to Eclipse Mosquitto. The
broker lives in `devices/docker` and delegates authentication and topic ACL to this backend
(`POST /internal/mqtt/auth/user` and `/acl`, mosquitto-go-auth), so a device registered in the
`device` table can connect with its `device_code` / `api_key` and nothing else is configured in
the broker. Full contract and firmware steps: `devices/README.md`.

1. `.env`: `MQTT_BACKEND_USER`, `MQTT_BACKEND_PASS` (any random value) and
   `DEVICE_MQTT_PUBLIC_HOST` = the LAN IP of this PC (`hostname -I`, never `localhost`). That IP
   is what the web app hands to a module when it is linked.
2. TLS certificate for the broker, valid for the IPs of this PC (re-run it if the IP changes):

   ```bash
   cd ../../devices/docker
   ./mosquitto/generate-dev-certs.sh
   ```

3. Linux with `ufw` only: the broker container calls the backend on the host, which ufw blocks.
   Allow the Docker networks, not the LAN:

   ```bash
   sudo ufw allow from 172.16.0.0/12 to any port 8080 proto tcp comment 'mosquitto -> backend'
   ```

4. Start the broker (with the backend already running):

   ```bash
   docker compose up -d
   docker compose logs -f mosquitto   # the backend shows up as energy-monitor-backend-telemetry / -status
   ```

| Variable | Default | Meaning |
|---|---|---|
| `MQTT_BROKER_URL` | `tcp://localhost:1883` | internal listener (plain, loopback only) |
| `MQTT_BACKEND_USER` / `MQTT_BACKEND_PASS` | `em-backend` / `change-me` | backend credentials in the broker |
| `MQTT_CLIENT_ID` | `energy-monitor-backend` | must differ per backend instance |
| `MQTT_AUTH_ALLOWED_NETWORKS` | `127.0.0.1/32,::1/128,172.16.0.0/12` | who may call `/internal/mqtt/auth/**` |
| `DEVICE_MQTT_PUBLIC_HOST` / `_PORT` | — / `8883` | broker address given to devices when linked |
| `DEVICE_OFFLINE_AFTER` | `PT150S` | a device silent this long is marked OFFLINE |

A public reverse proxy must **not** forward `/internal/**` (section 7: behind it every request
looks local).

## 10. Alert notifications (mail and browser push)

Every alert (device connected after being linked, device disconnected, high/critical
consumption, daily or monthly limit reached) is delivered to all
members of the home, by mail and by Web Push according to each user's
**Settings > Notifications** (`/api/v1/notifications/preferences`, both on by default). Mail uses
the SMTP account of section 5; every attempt is a row of the `notification` table.

Push needs a VAPID key pair in `.env` (`NOTIFICATION_VAPID_PUBLIC_KEY` / `_PRIVATE_KEY`); the
commands are in `.env.example`. Generate it **once per environment** and keep it: browsers bind
their subscription to the public key. Without it push is off and mail still works. Browsers
only allow push on `https://` or `http://localhost`.

`NOTIFICATION_WEB_URL` is the address of the web app used by the links in the mail.

- **Device linked** is raised by the first reading the module sends after it was linked, once per
  link: retrying the linking from the web while the module cannot join the network sends nothing.
- **Limit reached** (`alert.limit.daily` / `alert.limit.monthly`) is raised when the energy of the
  home since the start of the day or month (`ALERT_LIMIT_ZONE`, default `America/Bogota`)
  reaches the limit set in the thresholds. It resolves itself when a new period starts or the
  limit is raised above the consumption.

New recommendations (section 11) are delivered the same way, with the source type
`RECOMMENDATION` in the `notification` table.

## 11. Consumption recommendations

Every hour (`RECOMMENDATION_EVALUATE_INTERVAL`, default `PT1H`, first run 2 minutes after start)
the backend reviews the hourly energy of every home whose devices reported in the last week and
stores a recommendation when a rule matches:

| Type | Message key | Rule |
|---|---|---|
| `PEAK_HOURS` | `recommendation.peakHours` | ≥ 40 % of the last 7 days' energy (min. 2 kWh) used between 18:00 and 22:00 |
| `SAVING` | `recommendation.standby` | a device (not a refrigerator) drew ≥ 15 W on average between 01:00 and 05:00 on each of the last 3 nights |
| `HISTORICAL_COMPARISON` | `recommendation.aboveAverage` | last 7 days ≥ 25 % above the 7 before (min. 1 kWh) |
| `THRESHOLD` | `recommendation.limitProjection` | at the current pace the home exceeds its daily or monthly limit (whichever the owner set) |
| `HIGH_CONSUMPTION` | `recommendation.deviceIncrease` | a device's mean power in the last 7 days ≥ 30 % above the 21 days before |

- Hours are local to `RECOMMENDATION_ZONE` (default `America/Bogota`).
- The same advice (home + type + device) is not repeated within 7 days, even if it was deleted.
- Members read them in the web app (Notifications > Recommendations): `GET
  /api/v1/recommendations?homeId=`, `PUT /api/v1/recommendations/{id}/read`, `DELETE
  /api/v1/recommendations/{id}` (read ones only) and `DELETE /api/v1/recommendations?homeId=`
  (all read ones). A non-member gets 404.
- The rules need history: the evening-peak and standby rules work after a few days of readings,
  the week-over-week comparison after two weeks and the per-device comparison after four.

## 12. Docker image

`Dockerfile` builds the production image (Temurin 21, runs as a non-root user, uid 10001).
`devices/docker/docker-compose.prod.yml` builds it and runs it next to MySQL and the broker on a
server; that compose file and `devices/README.md` section 7 describe the deployment.

To try the image on this machine (with MySQL and the broker of sections 2 and 9 running, and the
local backend **stopped**, or two backends would store every reading twice):

```bash
docker build -t energy-monitor-backend .
docker run --rm --network host --user "$(id -u):$(id -g)" \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/energy_monitor?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
  -e SPRING_DATASOURCE_USERNAME=energy_app \
  -e SPRING_DATASOURCE_PASSWORD=<same as MYSQL_PASSWORD> \
  -e SECURITY_JWT_PRIVATE_KEY_PATH=/run/keys/private.pem \
  -e SECURITY_JWT_PUBLIC_KEY_PATH=/run/keys/public.pem \
  -v "$HOME/.energy-monitor/keys:/run/keys:ro" \
  energy-monitor-backend
```

- Any profile other than `local` works (`prod` above): `application-local.yaml` is not inside
  the image, so every setting comes from the environment.
- `--user "$(id -u):$(id -g)"` lets the container read your `private.pem` (mode 600). On a
  server, either make the key readable by uid 10001 or set `BACKEND_UID`/`BACKEND_GID` in the
  production compose.

## 13. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Tests fail with `Access denied for user 'energymonitor'` | `.env` sets `SPRING_DATASOURCE_USERNAME` to something other than `energy_app`: remove it |
| `Too many connections` in a few persistence tests | the backend is running while the suite runs; both use the same MySQL. Run the failing class alone or stop the backend |
| Broker logs `context deadline exceeded` calling `backend:8080` | backend stopped, or the ufw rule of section 9 is missing |
| Device logs `rc=-2` / TLS error | certificate not valid for the PC's current IP: re-run `generate-dev-certs.sh` and restart the broker |
| Device logs `rc=4` / `rc=5` | `device_code` / `api_key` do not match the `device` table (link it again from the web) |
| Container: `Failed to read the RSA private key PEM` | the container user cannot read the key: see section 12 |

## Useful commands

| Command | Purpose |
|---|---|
| `docker compose stop` | Stop the database, keeping its data |
| `docker compose start` | Start it again |
| `docker compose down -v` | Delete the container and its data, rebuilding the schema from Liquibase on the next run |

Liquibase owns the database schema. Hibernate only validates it and never
creates or alters a table.