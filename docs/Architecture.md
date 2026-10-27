# Architecture

`energy-monitor-backend` is a **modular monolith** (Spring Modulith). Each bounded context is a
top-level package under `com.energymonitor`, organised internally as Hexagonal Architecture
(`domain`, `application`, `adapter`, `infrastructure`), and exposes to the others only what is in
its `api` package. `ModularityTests` fails the build when a module reaches past another one's
`api` or depends on one it does not declare.

Six contexts have moved out to their own services: **security** (`en-monitor-security-service`,
:8082), **devices** (`en-monitor-device-service`, :8084), **measurements**
(`en-monitor-measurement-service`, :8085), **homes** (`en-monitor-home-service`, :8086),
**notifications** (`en-monitor-notification-service`, :8087) and **alerts**
(`en-monitor-alert-service`, :8088). The packages of the devices, measurements and homes stay here
as thin proxies, so the modules that use them did not change;
the security package keeps only the token validation, and the notification and alert ones are
gone. Only the recommendations are still served here.

## Modules

| Module | Owns | May use |
|---|---|---|
| `home` | Proxy of the home service: a home and its members (cached, stale copy when it does not answer), what a user is in a home (cached, never stale), its limits | nothing |
| `measurement` | Proxy of the measurement service: consumption levels (cached), hourly energy, reporting devices | nothing |
| `recommendation` | Hourly consumption advice | `device::api`, `measurement::api`, `home::api`, `outbox::api` |
| `outbox` | The events for other services (`backend_outbox`) and their relay to the broker | nothing |
| `security` | Validation of the access tokens the security service issues, CORS | nothing |
| `device` | Proxy of the device service: cached device reads, forgotten on a link or an unlink | nothing |

```
                 ┌──────── device::api ◄───────── recommendation
 device proxy ───┤
                 ├── adapter/out/remote ──► en-monitor-device-service /internal/devices|homes/**
                 └── adapter/in/messaging ◄── energy-monitor.device: device.linked, device.unlinked

                      ┌──── measurement::api ◄──── recommendation
 measurement proxy ───┤
                      └── adapter/out/remote ──► en-monitor-measurement-service /internal/measurements/**,
                                                 /internal/consumption-levels

                 ┌──────── home::api ◄──────────── recommendation
 home proxy ─────┤
                 └── adapter/out/remote ──► en-monitor-home-service /internal/homes/**

                 ┌──────── outbox::api ◄────────── recommendation
 outbox ─────────┤
                 └── relay ──► energy-monitor.recommendation (RabbitMQ)
```

## The proxies

All three follow the same shape:

- **`api`**: the contract the other modules already used (`DeviceApi`; `MeasurementApi`,
  `RiskConsumption`; `HomeApi`, `HomeDto`, `HomeThresholdsDto`), unchanged by the extraction
  except for the methods and events no module used, plus an "unavailable" exception for when the
  service does not answer. The connectivity events and `MeasurementRecorded` went with the alerts.
- **`adapter/out/remote`**: an HTTP client of the service's internal API, with
  `X-Internal-Token`, short timeouts and a Caffeine cache (devices: 30 s, unknown ones included,
  and a stale copy up to 1 h when the service does not answer; consumption levels: 10 min, stale
  copy up to 24 h; the energy is never cached; homes and limits: 30 s, stale copy up to 1 h; what
  a user is in a home: 30 s, never stale, so a removed member does not keep access while the
  service is down).
- **`adapter/in/messaging`** (devices): the links and unlinks, which drop the cached device and
  home. The measurement and home services publish nothing this backend needs.
- **`infrastructure`**: the wiring; no domain, no tables.

## Messaging

| Direction | Exchange | Routing keys | Here |
|---|---|---|---|
| in | `energy-monitor.device` | `device.linked`, `device.unlinked` | `device` proxy: cache |
| out | `energy-monitor.recommendation` | `recommendation.created` | `outbox`: for the notification service |

Every queue is durable, with a dead-letter queue after three attempts; delivery is at least once
and the consumers tolerate duplicates. The outbox publishes in order and marks a message only once
the broker confirmed it and a queue took it.

## Internal API

Calls between the services use `/internal/**`, an allowed network **and** `X-Internal-Token`; the
gateway never forwards them. This backend serves none: it only calls the others.

| Direction | Route |
|---|---|
| backend → home | `/internal/homes/{homeId}`, `/internal/homes/{homeId}/access?userId=`, `/internal/homes/{homeId}/thresholds` |
| backend → device | `/internal/devices/{id}`, `/internal/homes/{homeId}/devices` |
| backend → measurement | `/internal/consumption-levels`, `/internal/measurements/hourly-energy`, `/internal/measurements/reporting-devices` |

## MQTT

This backend is not a broker client. The device service authenticates the broker, keeps the
connectivity and sends the commands; the measurement service subscribes to the telemetry and
stores the readings.

## Database

One MySQL schema shared by the services (ADR-003 of `en-monitor-docs`); each service owns its
tables and applies only its changelogs. Here: `recommendation` and `outbox`. The
`security-*`, `device-*`, `measurement-*`, `home-*`, `notification-*` and `alert-*` changelogs moved with
their services under the same paths, so `DATABASECHANGELOG` already lists them as applied. No
foreign key crosses a service boundary: other contexts are referenced by identifier.

## Migration status

| Step | Content | Status |
|---|---|---|
| Security service | Module moved out; events, internal API, proxy here, gateway in front | Done |
| Device service: reads | `DeviceApi` answered by the device service's internal API, cached | Done |
| Device service: events | Connectivity and link events consumed here; `home.deleted` published from `home_outbox` | Done |
| Device service: removal | Device module removed; broker authentication, commands and connectivity there | Done |
| Device service: deployment | Gateway routes, infra container, Mosquitto pointing at the device service | Done |
| Measurement service: limits | `GET /internal/homes/{homeId}/thresholds` for the consumption summary | Done |
| Measurement service: removal | Measurement module removed; levels and energy read from the service, `measurement.recorded` consumed here; no MQTT client left | Done |
| Measurement service: deployment | Gateway routes, infra container, ingestion turned on there | Done |
| End-to-end check | Whole stack in Docker (`infra/e2e/device-flow.sh`, `infra/e2e/measurement-flow.sh`) | Done |
| Home service: reads | `HomeApi` answered by the home service's internal API, cached | Done |
| Home service: removal | Home module, its changelogs, its outbox relay, its `account.deleted` consumer and its internal routes removed | Done |
| Notification service: events | `alert.raised` and `recommendation.created` published from `backend_outbox` | Done |
| Notification service: removal | Notification module, its changelogs, its consumers of the security service, the security service client and the admin routes removed | Done |
| Alert service: removal | Alert module, its changelogs, its exchange, the `measurement.recorded` consumer and the connectivity events removed; the alerts are raised, served and announced by `en-monitor-alert-service` | Done |
| Recommendation | Migration of the last module to a microservice | Pending |

Everything above marked Done up to the home service is in production since 24-Oct-2026.

## Working principles

1. A module reaches another only through its `api`.
2. Keep each domain free of Spring and JPA dependencies.
3. No business logic in controllers, consumers or JPA entities.
4. A context that moves out keeps its `api` here, answered by a proxy, until no module needs it.
