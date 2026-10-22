# Energy Monitor Backend

Backend for Energy Monitor, built with Spring Boot 4.1.1 on Java 21. The firmware of the
ESP32 + PZEM-004T V3 meter that publishes to it lives in [`firmware/`](firmware/README.md); MySQL
and the MQTT broker run from the
[energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra) repository.

Accounts, sign-in, sessions, password recovery and global roles are served by the security
service (`../en-monitor-security-service`). This backend validates the access tokens it issues,
asks it for permissions, names and administrators, and hears its account events over RabbitMQ
(section 12). The web and mobile clients reach both through the API gateway
(`../en-monitor-api-gateway`) on `:8080`; this backend listens on `:8083`.

## Requirements

- Java 21 or later
- Docker and Docker Compose, for MySQL 8 and the MQTT broker, which run from the
  [energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra) repository
  cloned at `../../infra`
- The security service (`../en-monitor-security-service`) running, for anything that signs in
- The API gateway (`../en-monitor-api-gateway`) running, for the web and mobile apps
- OpenSSL, to generate the RSA key pair and the VAPID keys
- No Maven install needed: use the bundled `./mvnw` (`mvnw.cmd` on Windows)

## Quick start on a new machine

The whole system runs on one PC in development: MySQL, RabbitMQ and the MQTT broker (from
`infra`), the security service, the API gateway, the backend (this repository), the web app (`frontend/energy-monitor-web`) and the ESP32 on the same
Wi-Fi. `infra` can also run the backend and the web app in containers (`docker compose up -d
--build`, see `infra/README.md`); the steps below are for working on the backend from this
repository. In order:

| # | Step | Where |
|---|---|---|
| 1 | `cp .env.example .env` and fill every `CHANGE_ME` | section 1 |
| 2 | MySQL and the broker from `infra` (development mode) | section 2 |
| 3 | `cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml` | section 3 |
| 4 | Point at the public key of the security service; same `INTERNAL_API_TOKEN` in both | section 4 |
| 5 | Mail account (Gmail app password) | section 5 |
| 6 | `./mvnw spring-boot:run` → `http://localhost:8083/actuator/health` says `UP`; start the security service and the gateway too | section 6 |
| 7 | MQTT broker: already started in step 2; firewall rule on Linux | section 9 |
| 8 | Optional: VAPID keys for browser push | section 10 |
| 9 | Web app: `npm install`, `npm run dev` | `frontend/energy-monitor-web/README.md` |
| 10 | Firmware: flash the ESP32 and link it from the web | `firmware/README.md` |

The backend has to be up before the broker accepts any connection: Mosquitto asks it to
authenticate every client, including the backend itself (it retries every 10 s on its own).

## 1. Database credentials

Copy the example file:

```bash
cp .env.example .env
```

```powershell
Copy-Item .env.example .env
```

Set `SPRING_DATASOURCE_PASSWORD` to `MYSQL_PASSWORD` of `infra/.env`: MySQL is created by `infra`,
not by this repository. `.env` is gitignored and must never be committed.

## 2. Start the database and the broker

MySQL and the MQTT broker run from the `infra` repository. With the backend on this machine
(`./mvnw`), start only those two, in development mode:

```bash
cd ../../infra
./prepare.sh        # first time only: infra/.env, keys and the broker certificate
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql mosquitto
docker compose ps
```

Wait until `mysql` reports `healthy`. It creates the `energy_monitor` schema and the `energy_app`
user from `infra/.env`. If the full stack was up, stop its backend first
(`docker compose stop backend frontend`): two backends would store every reading twice.

## 3. Application configuration

Copy the example profile and set the same password (`MYSQL_PASSWORD` of `infra/.env`):

```bash
cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml
```

```powershell
Copy-Item src\main\resources\application-local.yaml.example src\main\resources\application-local.yaml
```

This file is gitignored as well.

## 4. Access tokens (public key of the security service)

The security service signs the access tokens (RS256) with its private key. This backend only
validates them, so it needs **the public key only**: the same `public.pem` the security service
is given, generated as its README explains. The private key never goes here.

Set the path once, in `.env` or in `application-local.yaml`:

```properties
SECURITY_JWT_PUBLIC_KEY_PATH=/home/you/.energy-monitor/keys/public.pem
```

```yaml
security:
  jwt:
    public-key-path: /home/you/.energy-monitor/keys/public.pem
```

Write the path **absolute**. Java does not expand `~`, so `~/.energy-monitor/keys/public.pem`
is taken literally and the key comes back unreadable. On Windows use
`C:\Users\you\.energy-monitor\keys\public.pem`. X.509 (`openssl rsa -pubout`) and PKCS#1
(`openssl rsa -RSAPublicKey_out`) are both accepted.

The application refuses to start when the key is missing, unreadable or a private key, and the
error message names the path it could not read. The issuer (`security.jwt.issuer`) must match the
service's; both keep `https://energy-monitor-backend`.

The same `INTERNAL_API_TOKEN` goes in this `.env` and in the service's (section 12), and
`SECURITY_SERVICE_URL` says where it answers (`http://localhost:8082` by default).

## 5. Email delivery (verification, recovery and alert mail)

The verification and recovery codes the security service hands over (section 12), and the alerts
of section 10, are mailed from here over SMTP. Every credential comes from the
environment; none is ever written into a committed file.

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
the context. That is deliberate: the security service answers the recovery request with `202`
whether or not the account exists, so a mail failure must never change what the caller sees.

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

The API listens on `http://localhost:8083` (`SERVER_PORT`). Swagger UI is served at
`/swagger-ui.html` and the OpenAPI document at `/v3/api-docs`.

The clients do not call it there: they call the API gateway on `http://localhost:8080`, which
forwards `/api/v1/auth/**` and `/api/v1/admin/users|roles/**` to the security service and every
other `/api/v1/**` here, after checking the access token and answering CORS. The gateway drops the
`Origin` header, so this backend never answers CORS itself and `SECURITY_CORS_ALLOWED_ORIGINS` is
set in the gateway's `.env` instead.

## 7. Running behind a reverse proxy

The application reads the caller's address from `request.getRemoteAddr()` and **ignores
`X-Forwarded-For`**: a header the client sets is not evidence of anything. The internal routes
(`/internal/mqtt/auth/**` and `/internal/accounts/**`) trust a caller partly by that address, so
behind a proxy every request would look like it came from the proxy. A public reverse proxy must
therefore **never forward `/internal/**`**; the web image's nginx forwards `/api/` only, and the
API gateway refuses `/internal/**` outright. This backend keeps ignoring `X-Forwarded-For` behind
the gateway too: it limits nothing by address, so it has no use for the caller's.

## 8. Running the tests

```bash
./mvnw test
```

The suite is not hermetic: the persistence tests run against the real database,
because Liquibase owns the schema and Hibernate only validates it. Start it first
(section 2, from `infra`).

Those tests need `SPRING_DATASOURCE_PASSWORD`. A handful of test classes activate
the `test` profile, and `spring.profiles.default: local` only applies when no
profile is active, so under `test` the `application-local.yaml` that holds your
local credentials **is not loaded**. The `test` profile therefore has to be told
the password, and it reads that one variable:

```properties
SPRING_DATASOURCE_PASSWORD=  # same value as MYSQL_PASSWORD of infra/.env
```

Put it in `.env`, which the application imports automatically
(`spring.config.import`), so nothing has to be exported:

```bash
(cd ../../infra && docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql mosquitto)
./mvnw test
```

Exported in the shell it also works, and wins over `.env`. Left unset, the
password resolves to an empty string and the persistence tests fail with
`Access denied for user 'energy_app'` rather than connecting to something they
were not meant to.

Nothing else has to be exported. The tests that boot the application generate a key pair of
their own and sign their tokens with it, as the security service would; `application-test.yaml`
carries a test-only `INTERNAL_API_TOKEN` and points the security service at an address where
nothing listens, so the suite never reaches a service running on this machine. Neither the
security service nor RabbitMQ is needed.

## 9. MQTT broker and devices

Devices (ESP32 + PZEM-004T) publish telemetry over **MQTT with TLS** to Eclipse Mosquitto. The
broker runs from the `infra` repository (`infra/mosquitto/mosquitto.conf`) and delegates
authentication and topic ACL to this backend
(`POST /internal/mqtt/auth/user` and `/acl`, mosquitto-go-auth), so a device registered in the
`device` table can connect with its `device_code` / API key and nothing else is configured in
the broker. Only the SHA-256 of each key is stored (`device.api_key_hash`); the key in clear is
returned once, when the module is linked or gets new credentials, and cannot be read back. Full contract and firmware steps: `firmware/README.md`.

1. `.env`: `MQTT_BACKEND_USER`, `MQTT_BACKEND_PASS` (any random value) and
   `DEVICE_MQTT_PUBLIC_HOST` = the LAN IP of this PC (`hostname -I`, never `localhost`). That IP
   is what the web app hands to a module when it is linked. Put the same IP in
   `DEVICE_MQTT_PUBLIC_HOST` of `infra/.env`: `infra/prepare.sh` adds it to the broker's TLS
   certificate (re-run it, and restart the broker, if the IP changes).
2. Linux with `ufw` only: in development mode the broker container calls the backend on the host,
   which ufw blocks. Allow the Docker networks, not the LAN:

   ```bash
   sudo ufw allow from 172.16.0.0/12 to any port 8083 proto tcp comment 'mosquitto -> backend'
   ```

3. The broker was started in section 2. Check that the backend connected:

   ```bash
   cd ../../infra
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

Device endpoints of a home (`/api/v1/homes/{homeId}/devices`): `GET` lists them (members);
`POST` links a module, `PUT /{deviceId}` edits appliance, name and room,
`POST /{deviceId}/credentials` issues a new api key plus the broker address to give a linked
module a new Wi-Fi network (not a new link), and `DELETE /{deviceId}` unlinks it (owner only).

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

## 12. The security service

### Messages (RabbitMQ)

What the security service announces arrives here through RabbitMQ, on the topic exchange
`energy-monitor.security`:

| Queue | Routing keys | What this backend does |
|---|---|---|
| `energy-monitor-backend.notification.account-events` | `account.registered`, `account.administered`, `account.deleted` | In-app notices to the administrators; a deleted account's mail and push preferences and devices are dropped |
| `energy-monitor-backend.notification.mail` | `mail.email-verification`, `mail.password-reset` | Mails the code |
| `energy-monitor-backend.home.account-deleted` | `account.deleted` | Drops the memberships and the homes nobody else is left in |

- The queues are declared here at startup, durable, each with a `.dlq` dead-letter queue: a
  message that fails three attempts (1, 2 and 4 s apart) is moved there instead of blocking the
  queue.
- A message may arrive twice. The administrator notices are written once (same account, message
  and instant), a mail request reuses its message id as the id of the delivery record, and the
  account cleanup finds nothing left to remove the second time.
- The security service asks first whether an account may go (below) and does not delete the only
  owner of a home that still has other members. If one slips through anyway (a member joined in
  between), the oldest remaining member becomes the owner and `HomeOwnershipTransferred` is
  published, so a home is never left without one.
- Connection: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD`. The
  consumers are active, so **without a reachable broker `/actuator/health` reports DOWN**. The
  test profile does not start them and needs no broker.

### Internal calls with the security service

The security service asks this backend `GET /internal/accounts/{id}/deletion-check` before it
deletes an account: `204` lets it go, `409` with `{message}` refuses it (the account is the only
owner of a home with other members). The path has its own filter chain: the caller must come from
`INTERNAL_API_ALLOWED_NETWORKS` **and** send `INTERNAL_API_TOKEN` in `X-Internal-Token`, the same
token configured in the security service. Without a token every call is refused. The reverse
proxy must not forward `/internal/**`.

In the other direction this backend asks the service, with the same token
(`security/adapter/out/remote`):

| Route of the security service | Used for | Cached | When it does not answer |
|---|---|---|---|
| `GET /internal/users/{id}/permissions` | `USER.READ` on `/api/v1/admin/notifications` | 30 s | `503` |
| `POST /internal/users/profiles` | names in the member list, alert mail addresses, administrator notifications | 60 s | names left out; alert mail skipped, push still sent |
| `GET /internal/administrators` | who receives the administrator notifications | 30 s | the message is retried, then dead-lettered |

A call waits 1 s for the connection and 2 s for the answer (`security.service.*`). A failure is
never cached, so the next request tries again.

### Administrator notifications

Administrators (accounts holding the global `ADMIN` role of the security service) are told,
inside the web app, about:

| Message | Sent when | Who receives it |
|---|---|---|
| `account.registered` | an account is created (registration or first Google sign-in) | every active administrator |
| `account.administered` | an administrator changes another account's status, or grants or revokes a role (not on repeats) | every active administrator except the one who made the change |

The security service sends them only once the change is committed (a registration that rolls back
notifies nobody). The notification module asks it who the administrators are and stores one row
per administrator in `notification`, with channel `IN_APP` and source
`ADMINISTRATION` (`notification-004-add-in-app-admin-notifications`). Only the message key and the
affected account are stored; the web builds the text. These notifications are not sent by mail
or push.

| Route (requires `USER.READ`, only the caller's own notifications) | Purpose |
|---|---|
| `GET /api/v1/admin/notifications` | latest 50, newest first, with the affected account's name and address (left out while the security service does not answer) |
| `PUT /api/v1/admin/notifications/{id}/read` | mark one as read (`204`; `404` if it is not the caller's) |
| `DELETE /api/v1/admin/notifications/{id}` | delete one (`204`; `404` if it is not the caller's) |

**What an administrator sees on the web.** An account holding `ADMIN` gets only the
administration experience: it lands on **Administration** after signing in, and its menu holds
Administration, Notifications (the administrator notifications above) and Settings (language,
theme, help). The home screens (dashboard, homes, favorites) redirect it to Administration. This
is a web decision only: the backend still lets that account call the home endpoints, so use a
dedicated account to administer and a regular one for your homes.

`USER.READ` is asked to the security service on each request and cached 30 seconds, so a role
granted or revoked takes effect within that time. While the service does not answer these routes
answer `503`; any other `/api/v1/admin/**` route of this backend is denied. The user directory and
the roles (`/api/v1/admin/users`, `/api/v1/admin/roles`) are served by the security service.

Not done yet: email or push for administrator notifications, managing support content and the
documentation from Settings, and refusing the home endpoints to administrators on the server.

The mobile app has no administration screens; it only uses the regular endpoints.

## 13. Docker image

`Dockerfile` builds the image (Temurin 21, runs as a non-root user, uid 10001). The `infra`
repository builds it and runs it next to MySQL, the broker and the web app:
`infra/docker-compose.yml` on this machine and `infra/docker-compose.prod.yml` on a server (see
`infra/README.md`). Every setting comes from the environment: `application-local.yaml` is not
inside the image, and any profile other than `local` works (`prod`).

## 14. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Tests fail with `Access denied for user 'energymonitor'` | `.env` sets `SPRING_DATASOURCE_USERNAME` to something other than `energy_app`: remove it |
| `Too many connections` in a few persistence tests | the backend is running while the suite runs; both use the same MySQL. Run the failing class alone or stop the backend |
| Broker logs `context deadline exceeded` calling `backend:8083` | backend stopped, or the ufw rule of section 9 is missing (the old one for 8080 no longer applies) |
| Web app: every request fails or the browser reports CORS | the API gateway is not running on 8080, or `SECURITY_CORS_ALLOWED_ORIGINS` of its `.env` does not list the web's origin |
| Device logs `rc=-2` / TLS error | certificate not valid for the PC's current IP: update `DEVICE_MQTT_PUBLIC_HOST` in `infra/.env`, re-run `infra/prepare.sh` and restart the broker |
| Device logs `rc=4` / `rc=5` | `device_code` / API key do not match the `device` table (link it again from the web) |
| `403` on `/api/v1/admin/**`, or no **Administration** in the web menu | the account does not hold `ADMIN`: grant it as the security service's README explains, then reload the web |
| `503` on `/api/v1/admin/notifications`, members without names | the security service does not answer, or `INTERNAL_API_TOKEN` differs between the two `.env` files |
| `401` on every request with a fresh token | `SECURITY_JWT_PUBLIC_KEY_PATH` is not the public key of the security service's pair |
| Container: `Failed to read the RSA public key PEM` | the container user cannot read the key: `BACKEND_UID`/`BACKEND_GID` in `infra/.env` must own `infra/keys` (`infra/prepare.sh` sets them) |
| `Access denied for user 'energy_app'` when starting or testing | `application-local.yaml` / `SPRING_DATASOURCE_PASSWORD` do not match `MYSQL_PASSWORD` of `infra/.env` |

## Useful commands

| Command | Purpose |
|---|---|
| `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql mosquitto` (in `infra`) | Start MySQL and the broker |
| `docker compose stop` (in `infra`) | Stop them, keeping the data |
| `docker compose down -v` (in `infra`) | Delete the containers and their data, rebuilding the schema from Liquibase on the next run |

Liquibase owns the database schema. Hibernate only validates it and never
creates or alters a table.