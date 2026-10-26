# Energy Monitor Backend

Backend for Energy Monitor, built with Spring Boot 4.1.1 on Java 21. The firmware of the
ESP32 + PZEM-004T V3 meter that publishes to it lives in [`firmware/`](firmware/README.md); MySQL
and the MQTT broker run from the
[energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra) repository.

Accounts, sign-in, sessions, password recovery and global roles are served by the security
service (`../en-monitor-security-service`). This backend validates the access tokens it issues,
asks it for permissions, names and administrators, and hears its account events over RabbitMQ
(section 12). The devices, their credentials, the broker's authentication and their connectivity
are served by the device service (`../en-monitor-device-service`, `:8084`); this backend reads
devices from it and hears its events. The readings, their MQTT ingestion and the consumption of a
home are served by the measurement service (`../en-monitor-measurement-service`, `:8085`); this
backend reads consumption levels and energy from it and hears every stored reading, for the alerts
and the recommendations. The web and mobile clients reach the four through the API gateway
(`../en-monitor-api-gateway`) on `:8080`; this backend listens on `:8083`.

The modules, the two proxies and the migration status are in
[`docs/Architecture.md`](docs/Architecture.md).

## Requirements

- Java 21 or later
- Docker and Docker Compose, for MySQL 8 and RabbitMQ, which run from the
  [energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra) repository
  cloned at `../../infra`
- The security service (`../en-monitor-security-service`) running, for anything that signs in
- The API gateway (`../en-monitor-api-gateway`) running, for the web and mobile apps
- OpenSSL, to generate the RSA key pair and the VAPID keys
- No Maven install needed: use the bundled `./mvnw` (`mvnw.cmd` on Windows)

## Quick start on a new machine

The whole system runs on one PC in development: MySQL, RabbitMQ and the MQTT broker (from
`infra`), the security service, the API gateway, the backend (this repository), the web app (`frontend/energy-monitor-web`) and the ESP32 on the same
Wi-Fi. `infra` can also run the whole platform in containers (`docker compose up -d --build`,
see `infra/README.md`); the steps below are for working on the backend from this repository. In order:

| # | Step | Where |
|---|---|---|
| 1 | `cp .env.example .env` and fill every `CHANGE_ME` | section 1 |
| 2 | MySQL and the broker from `infra` (development mode) | section 2 |
| 3 | `cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml` | section 3 |
| 4 | Point at the public key of the security service; same `INTERNAL_API_TOKEN` in both | section 4 |
| 5 | Mail account (Gmail app password) | section 5 |
| 6 | `./mvnw spring-boot:run` → `http://localhost:8083/actuator/health` says `UP`; start the security service and the gateway too | section 6 |
| 7 | Readings: run the device and measurement services; this backend does not use the broker | section 9 |
| 8 | Optional: VAPID keys for browser push | section 10 |
| 9 | Web app: `npm install`, `npm run dev` | `frontend/energy-monitor-web/README.md` |
| 10 | Firmware: flash the ESP32 and link it from the web | `firmware/README.md` |

The broker asks the device service to authenticate every client, so it has to be up before any
module connects (Mosquitto retries every 10 s on its own).

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

MySQL, RabbitMQ and the MQTT broker run from the `infra` repository. With the backend on this
machine (`./mvnw`), start only those three, in development mode:

```bash
cd ../../infra
./prepare.sh        # first time only: infra/.env, keys and the broker certificate
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql rabbitmq mosquitto
docker compose ps
```

Wait until `mysql` reports `healthy`. It creates the `energy_monitor` schema and the `energy_app`
user from `infra/.env`. If the full stack was up, stop its services first
(`docker compose stop gateway security backend frontend`): two backends would store every reading
twice.

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
the context. That is deliberate: the security service has already answered the recovery request
(`202` for a registered address, `404` when no account uses it) before the mail is sent, so a mail
failure must never change what the caller sees.

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
`X-Forwarded-For`**: a header the client sets is not evidence of anything. It serves no
`/internal/**` route of its own (those of the other services trust a caller partly by its
address), and a public reverse proxy must **never forward `/internal/**`** to any of them; the web
image's nginx forwards `/api/` only, and the API gateway refuses `/internal/**` outright. This
backend keeps ignoring `X-Forwarded-For` behind the gateway too: it limits nothing by address, so
it has no use for the caller's.

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

## 9. Devices and readings

Devices (ESP32 + PZEM-004T) publish telemetry over **MQTT with TLS** to Eclipse Mosquitto, which
runs from the `infra` repository. This backend does not connect to the broker:

- **The devices belong to the device service** (`en-monitor-device-service`, port 8084): it links
  modules to homes, issues their API keys, answers the broker's authentication and ACL, sends the
  unlink commands and keeps the connectivity.
- **The readings belong to the measurement service** (`en-monitor-measurement-service`, port
  8085): it subscribes to `energy-monitor/devices/+/telemetry`, stores every reading and serves the
  measurement and consumption endpoints (`/api/v1/measurements/**`, `/api/v1/consumption-levels`,
  `/api/v1/homes/{homeId}/consumption/**`).

The API gateway routes those endpoints to each service. This backend reads devices and readings
from them and hears their events (section 12): every stored reading reaches the alerts as
`MeasurementRecorded`, as it did when the readings were stored here.

Firmware, contract and pairing: `firmware/README.md` and the device service's README.

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

## 12. The security, device, measurement and home services

### Messages (RabbitMQ)

What the security service announces arrives here through RabbitMQ, on the topic exchange
`energy-monitor.security`:

| Queue | Routing keys | What this backend does |
|---|---|---|
| `energy-monitor-backend.notification.account-events` | `account.registered`, `account.administered`, `account.deleted` | In-app notices to the administrators; a deleted account's mail and push preferences and devices are dropped |
| `energy-monitor-backend.notification.mail` | `mail.email-verification`, `mail.password-reset` | Mails the code |

- The queues are declared here at startup, durable, each with a `.dlq` dead-letter queue: a
  message that fails three attempts (1, 2 and 4 s apart) is moved there instead of blocking the
  queue.
- A message may arrive twice. The administrator notices are written once (same account, message
  and instant), a mail request reuses its message id as the id of the delivery record, and the
  account cleanup finds nothing left to remove the second time.
- The memberships and the homes of a deleted account are the home service's: it consumes
  `account.deleted` on a queue of its own.
- Connection: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD`. The
  consumers are active, so **without a reachable broker `/actuator/health` reports DOWN**. The
  test profile does not start them and needs no broker.

### Alert and recommendation events this backend publishes

Every alert raised and every recommendation created is also announced over RabbitMQ, for the
notification service (`en-monitor-notification-service`), which delivers it to the members of
the home:

| Exchange (topic) | Routing key = message type | Body |
|---|---|---|
| `energy-monitor.alert` | `alert.raised` | `{idAlert, homeId, deviceId, type, messageKey, dateTime}` |
| `energy-monitor.recommendation` | `recommendation.created` | `{idRecommendation, homeId, deviceId, type, messageKey, dateTime}` |

- **Outbox** (module `outbox`, changelog `outbox-001`): the event is written to `backend_outbox`
  when its alert or recommendation is stored, and a relay publishes it every second (up to 50 at a
  time). A broker or a notification service that is down delays an event but does not lose it.
- **At least once, in order:** a message is marked published only when the broker confirms it
  (correlated publisher confirms) **and** a queue took it. Until the notification service has
  declared its queues the message comes back and stays pending; the first failure stops the run,
  so a later message never overtakes it. `attempts` and `last_error` show why one is stuck. The
  notification service drops a copy it already delivered.
- **Off until the notification module is removed:** `ALERT_OUTBOX_RELAY_ENABLED`
  (`messaging.outbox.relay-enabled`) defaults to `false`, because this backend's notification
  module still delivers the alerts in process. The events are kept in `backend_outbox`
  meanwhile; the relay is switched on in the deployment that removes the module, together with
  the consumers of the notification service.
- **Settings:** `messaging.alert-exchange`, `messaging.recommendation-exchange` and
  `messaging.outbox.*` (`relay-enabled`, `poll-interval`, `batch-size`, `confirm-timeout`). The
  test profile keeps the relay off and stores the rows already discarded, so the suite never
  publishes to a broker a running notification service consumes.

### The device service

The devices live in the device service (`en-monitor-device-service`, port 8084). This backend reads
them through `DeviceApi`, answered by its internal API (`device/adapter/out/remote`):

| Call | Used by | Reused for | Without an answer |
|---|---|---|---|
| `GET /internal/devices/{id}` | alerts, notifications, recommendations (existence, name, installation date, home) | 30 s, unknown devices included | the last answer, kept 1 h; with none, the call fails |
| `GET /internal/homes/{id}/devices` | limit alerts and recommendations of a home | 30 s | the same |

Its events arrive on the queue `energy-monitor-backend.device.events`, bound to
`energy-monitor.device` (with a `.dlq`):

| Routing key | What this backend does |
|---|---|
| `device.connectivity-lost` | Publishes `DeviceConnectivityLost` in memory: the alert module raises the CONNECTIVITY alert (one while it is pending, however many times the message arrives) |
| `device.connectivity-restored` | Publishes `DeviceConnectivityRestored`: the alert is resolved |
| `device.linked`, `device.unlinked` | Drops the cached device and home, so a module just linked is not refused for half a minute |

Settings: `DEVICE_SERVICE_URL` (`device.service.url`, default `http://localhost:8084`),
`device.service.ttl`, `device.service.stale-ttl` and the timeouts (1 s connect, 2 s read). The
calls carry `INTERNAL_API_TOKEN`. The test profile points the client at a port where nothing
listens.

### The measurement service

The readings live in the measurement service (`en-monitor-measurement-service`, port 8085). The
alerts and the recommendations read them through `MeasurementApi`, answered by its internal API
(`measurement/adapter/out/remote`):

| Call | Used by | Reused for | Without an answer |
|---|---|---|---|
| `GET /internal/consumption-levels` | the threshold alert classifies every reading (`[minLimit, maxLimit)`) | 10 min | the last levels, kept 24 h; with none, the reading goes back to the broker |
| `GET /internal/measurements/hourly-energy` | consumption limit alerts, recommendations | never | the limit is checked on the next reading, the recommendations on the next run |
| `GET /internal/measurements/reporting-devices` | recommendations | never | the same |

Every stored reading arrives on the queue `energy-monitor-backend.measurement.recorded`, bound to
`energy-monitor.measurement` with `measurement.recorded` (with a `.dlq`). It is published again in
memory as `MeasurementRecorded`, in a transaction of its own, so the alert module evaluates it as
before: threshold (one alert while it is pending, however many times the message arrives), daily
or monthly limit and linked device. Declaring the queue is what lets the measurement service
publish: until then its messages wait in its outbox.

Settings: `MEASUREMENT_SERVICE_URL` (`measurement.service.url`, default `http://localhost:8085`),
`measurement.service.levels-ttl`, `levels-stale-ttl` and the timeouts (1 s connect, 5 s read: the
energy of weeks for the recommendations is the slowest call). The calls carry
`INTERNAL_API_TOKEN`. The test profile points the client at a port where nothing listens.

### The home service

The homes live in the home service (`en-monitor-home-service`, port 8086): their members, roles,
ownership, thresholds and types, the `home.deleted` events and the cleanup of deleted accounts.
The alerts, the notifications and the recommendations read them through `HomeApi`, answered by its
internal API (`home/adapter/out/remote`):

| Call | Answers | Used by | Reused for | Without an answer |
|---|---|---|---|---|
| `GET /internal/homes/{id}` | `findHome`, `memberIds`, `homeExists` | notifications (members, name of the home) | 30 s, missing homes included | the last answer, kept 1 h; with none, the call fails |
| `GET /internal/homes/{id}/thresholds` | `findThresholds` | limit alerts, recommendations | 30 s | the same |
| `GET /internal/homes/{id}/access?userId=` | `isMember`, `isOwner` | alerts and recommendations of a home | 30 s per user and home | **never stale**: a removed member must not keep access while the service is down, so the call fails |

`findThresholds` returns the limits and their period, which is all the alerts and recommendations
read: the home service does not expose the identifier of the thresholds row nor whether they are
the system defaults (`null` and `false`).

Settings: `HOME_SERVICE_URL` (`home.service.url`, default `http://localhost:8086`),
`home.service.ttl`, `home.service.stale-ttl` and the timeouts (1 s connect, 2 s read). The calls
carry `INTERNAL_API_TOKEN`. The test profile points the client at a port where nothing listens.

### Internal calls with the other services

This backend serves no `/internal/**` route: the deletion check, the home access and the limits of
a home that the security, device and measurement services ask are the home service's. It only
calls the other services, with `INTERNAL_API_TOKEN` in `X-Internal-Token`. The security service,
with the same token (`security/adapter/out/remote`):

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
repository builds it and runs it next to MySQL, RabbitMQ, the broker, the security service, the
API gateway and the web app (port 8083, reached only through the Docker network):
`infra/docker-compose.yml` on this machine and `infra/docker-compose.prod.yml` on a server (see
`infra/README.md`). Every setting comes from the environment: `application-local.yaml` is not
inside the image, and any profile other than `local` works (`prod`).

## 14. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Tests fail with `Access denied for user 'energymonitor'` | `.env` sets `SPRING_DATASOURCE_USERNAME` to something other than `energy_app`: remove it |
| `Too many connections` in a few persistence tests | the backend is running while the suite runs; both use the same MySQL. Run the failing class alone or stop the backend |
| Broker logs `context deadline exceeded` calling `device:8084` | the device service is not running, so the broker cannot authenticate anybody (see its README for the ufw rule in development mode) |
| Readings arrive but no threshold alert is raised, log `measurement service did not answer` | the measurement service is down and no consumption levels were kept, or `INTERNAL_API_TOKEN` differs between the `.env` files; the readings wait in `energy-monitor-backend.measurement.recorded` (or its `.dlq`) |
| Web app: every request fails or the browser reports CORS | the API gateway is not running on 8080, or `SECURITY_CORS_ALLOWED_ORIGINS` of its `.env` does not list the web's origin |
| Device logs `rc=-2` / TLS error | certificate not valid for the PC's current IP: update `DEVICE_MQTT_PUBLIC_HOST` in `infra/.env`, re-run `infra/prepare.sh` and restart the broker |
| Device logs `rc=4` / `rc=5` | `device_code` / API key refused by the device service (link it again from the web) |
| `403` on `/api/v1/admin/**`, or no **Administration** in the web menu | the account does not hold `ADMIN`: grant it as the security service's README explains, then reload the web |
| `503` on `/api/v1/admin/notifications`, members without names | the security service does not answer, or `INTERNAL_API_TOKEN` differs between the two `.env` files |
| `401` on every request with a fresh token | `SECURITY_JWT_PUBLIC_KEY_PATH` is not the public key of the security service's pair |
| Container: `Failed to read the RSA public key PEM` | the container cannot read `infra/keys/public.pem`: it must be readable by everyone (644, as `infra/prepare.sh` leaves it) |
| `Access denied for user 'energy_app'` when starting or testing | `application-local.yaml` / `SPRING_DATASOURCE_PASSWORD` do not match `MYSQL_PASSWORD` of `infra/.env` |

## Useful commands

| Command | Purpose |
|---|---|
| `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql mosquitto` (in `infra`) | Start MySQL and the broker |
| `docker compose stop` (in `infra`) | Stop them, keeping the data |
| `docker compose down -v` (in `infra`) | Delete the containers and their data, rebuilding the schema from Liquibase on the next run |

Liquibase owns the database schema. Hibernate only validates it and never
creates or alters a table.