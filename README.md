# Energy Monitor Backend

Backend for Energy Monitor, built with Spring Boot 4.1.1 on Java 21. The firmware of the
ESP32 + PZEM-004T V3 meter that publishes to it lives in [`firmware/`](firmware/README.md); MySQL
and the MQTT broker run from the
[energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra) repository.

Accounts, sign-in, sessions, password recovery and global roles are served by the security
service (`../en-monitor-security-service`). This backend validates the access tokens it issues
and nothing else of it. The mail and push delivery of the alerts and recommendations, the
verification and recovery mail and the administrator notifications are served by the notification
service (`../en-monitor-notification-service`, `:8087`); this backend announces its alerts and
recommendations to it over RabbitMQ (section 12). The devices, their credentials, the broker's authentication and their connectivity
are served by the device service (`../en-monitor-device-service`, `:8084`); this backend reads
devices from it and hears its events. The readings, their MQTT ingestion and the consumption of a
home are served by the measurement service (`../en-monitor-measurement-service`, `:8085`); this
backend reads consumption levels and energy from it and hears every stored reading, for the alerts
and the recommendations. The homes are served by the home service (`../en-monitor-home-service`,
`:8086`). The web and mobile clients reach all of them through the API gateway
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
- OpenSSL, to generate the RSA key pair
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
| 4 | Point at the public key of the security service; same `INTERNAL_API_TOKEN` in every service | section 4 |
| 5 | Mail and push: set them up in the notification service | section 5 |
| 6 | `./mvnw spring-boot:run` → `http://localhost:8083/actuator/health` says `UP`; start the security service and the gateway too | section 6 |
| 7 | Readings: run the device and measurement services; this backend does not use the broker | section 9 |
| 8 | Web app: `npm install`, `npm run dev` | `frontend/energy-monitor-web/README.md` |
| 9 | Firmware: flash the ESP32 and link it from the web | `firmware/README.md` |

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

The same `INTERNAL_API_TOKEN` goes in this `.env` and in the device, home and measurement
services' (section 12): this backend calls their internal APIs with it.

## 5. Mail and push

This backend sends no mail and no push. The verification and recovery codes, the mail and push of
the alerts and recommendations, the channels of each user and the administrator notifications
are the notification service's (`en-monitor-notification-service`): the Gmail app password
(`MAIL_*`), the VAPID keys and `EXPO_ACCESS_TOKEN` go in its `.env`, and its README explains
them. This backend only announces the alerts and recommendations to it (section 12).

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
sends each path to the service that owns it (the alerts and the recommendations here), after
checking the access token and answering CORS. The gateway drops the
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
carries a test-only `INTERNAL_API_TOKEN` and points the device, home and measurement services at
an address where nothing listens, so the suite never reaches a service running on this machine.
No other service and no RabbitMQ is needed.

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

## 10. Alerts

Every alert (device connected after being linked, device disconnected, high/critical consumption,
daily or monthly limit reached) is stored here and announced as `alert.raised` (section 12). The
notification service delivers it to all members of the home, by mail and push according to each
user's **Settings > Notifications**.

- **Device linked** is raised by the first reading the module sends after it was linked, once per
  link: retrying the linking from the web while the module cannot join the network sends nothing.
- **Limit reached** (`alert.limit.daily` / `alert.limit.monthly`) is raised when the energy of the
  home since the start of the day or month (`ALERT_LIMIT_ZONE`, default `America/Bogota`)
  reaches the limit set in the thresholds. It resolves itself when a new period starts or the
  limit is raised above the consumption.

New recommendations (section 11) are announced the same way, as `recommendation.created`.

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

## 12. The other services

### Messages (RabbitMQ)

This backend consumes the events of the device and measurement services (their sections below) and
publishes its alerts and recommendations (next section). The account events and the mail requests
of the security service are the notification service's and the home service's.

- Each queue is declared here at startup, durable, with a `.dlq` dead-letter queue: a message that
  fails three attempts (1, 2 and 4 s apart) is moved there instead of blocking the queue.
- Connection: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD`. The
  consumers are active, so **without a reachable broker `/actuator/health` reports DOWN**. The
  test profile does not start them and needs no broker.

### Alert and recommendation events this backend publishes

Every alert raised and every recommendation created is announced over RabbitMQ, for the
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
- **On by default:** `ALERT_OUTBOX_RELAY_ENABLED` (`messaging.outbox.relay-enabled`). The
  notification service is the only one that delivers the alerts; `false` only to go back to one
  whose consumers are off, and the events are kept in `backend_outbox` meanwhile.
- **Settings:** `messaging.alert-exchange`, `messaging.recommendation-exchange` and
  `messaging.outbox.*` (`relay-enabled`, `poll-interval`, `batch-size`, `confirm-timeout`). The
  test profile keeps the relay off and stores the rows already discarded, so the suite never
  publishes to a broker a running notification service consumes.

### The device service

The devices live in the device service (`en-monitor-device-service`, port 8084). This backend reads
them through `DeviceApi`, answered by its internal API (`device/adapter/out/remote`):

| Call | Used by | Reused for | Without an answer |
|---|---|---|---|
| `GET /internal/devices/{id}` | alerts and recommendations (existence, name, installation date, home) | 30 s, unknown devices included | the last answer, kept 1 h; with none, the call fails |
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
The alerts and the recommendations read them through `HomeApi`, answered by its
internal API (`home/adapter/out/remote`):

| Call | Answers | Used by | Reused for | Without an answer |
|---|---|---|---|---|
| `GET /internal/homes/{id}` | `findHome`, `memberIds`, `homeExists` | alerts and recommendations (the home exists) | 30 s, missing homes included | the last answer, kept 1 h; with none, the call fails |
| `GET /internal/homes/{id}/thresholds` | `findThresholds` | limit alerts, recommendations | 30 s | the same |
| `GET /internal/homes/{id}/access?userId=` | `isMember`, `isOwner` | alerts and recommendations of a home | 30 s per user and home | **never stale**: a removed member must not keep access while the service is down, so the call fails |

`findThresholds` returns the limits and their period, which is all the alerts and recommendations
read: the home service does not expose the identifier of the thresholds row nor whether they are
the system defaults (`null` and `false`).

Settings: `HOME_SERVICE_URL` (`home.service.url`, default `http://localhost:8086`),
`home.service.ttl`, `home.service.stale-ttl` and the timeouts (1 s connect, 2 s read). The calls
carry `INTERNAL_API_TOKEN`. The test profile points the client at a port where nothing listens.

### Internal calls with the other services

This backend serves no `/internal/**` route. It only calls the device, measurement and home
services, with `INTERNAL_API_TOKEN` in `X-Internal-Token`, and asks nothing of the security
service: the caller comes from the access token. It serves no `/api/v1/admin/**` route either:
the users and roles are the security service's and the administrator notifications the
notification service's, and any that reaches this backend is denied.

## 13. Docker image

`Dockerfile` builds the image (Temurin 21, runs as a non-root user, uid 10001). The `infra`
repository builds it and runs it next to MySQL, RabbitMQ, the broker, the security service, the
API gateway, the other services and the web app (port 8083, reached only through the Docker
network):
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