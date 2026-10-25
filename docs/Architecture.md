# Architecture

`energy-monitor-backend` is a **modular monolith** (Spring Modulith). Each bounded context is a
top-level package under `com.energymonitor`, organised internally as Hexagonal Architecture
(`domain`, `application`, `adapter`, `infrastructure`), and exposes to the others only what is in
its `api` package. `ModularityTests` fails the build when a module reaches past another one's
`api` or depends on one it does not declare.

Three contexts have moved out to their own services: **security** (`en-monitor-security-service`,
:8082), **devices** (`en-monitor-device-service`, :8084) and **measurements**
(`en-monitor-measurement-service`, :8085). Their packages stay here as thin proxies, so the
modules that use them did not change.

## Modules

| Module | Owns | May use |
|---|---|---|
| `home` | Homes, memberships, home roles (`OWNER`, `MEMBER`), consumption thresholds | `security::api` |
| `measurement` | Proxy of the measurement service: consumption levels (cached), hourly energy, reporting devices, recorded readings | nothing |
| `alert` | Threshold and connectivity alerts | `home::api`, `device::api`, `measurement::api` |
| `recommendation` | Hourly consumption advice | `device::api`, `measurement::api`, `home::api` |
| `notification` | Mail, web push and mobile push delivery, delivery log, administrator notices | `security::api`, `home::api`, `device::api`, `alert::api`, `recommendation::api` |
| `security` | Proxy of the security service: token validation, admin permission checks, profiles and administrators | nothing |
| `device` | Proxy of the device service: cached device reads, device events | nothing |

```
                 ┌──────── security::api ◄─────── home, notification
 security proxy ─┤
                 └── adapter/out/remote ──► en-monitor-security-service /internal/**

                 ┌──────── device::api ◄───────── alert, recommendation, notification
 device proxy ───┤
                 ├── adapter/out/remote ──► en-monitor-device-service /internal/devices|homes/**
                 └── adapter/in/messaging ◄── energy-monitor.device (RabbitMQ)

                      ┌──── measurement::api ◄──── alert, recommendation
 measurement proxy ───┤
                      ├── adapter/out/remote ──► en-monitor-measurement-service /internal/measurements/**,
                      │                          /internal/consumption-levels
                      └── adapter/in/messaging ◄── energy-monitor.measurement (RabbitMQ)
```

## The proxies

All three follow the same shape:

- **`api`**: the contract the other modules already used (`UserProfileQuery`, `AdministratorQuery`;
  `DeviceApi`, `DeviceConnectivityLost`, `DeviceConnectivityRestored`; `MeasurementApi`,
  `MeasurementRecorded`, `RiskConsumption`), unchanged by the extraction except for the methods no
  module called, plus an "unavailable" exception for when the service does not answer.
- **`adapter/out/remote`**: an HTTP client of the service's internal API, with
  `X-Internal-Token`, short timeouts and a Caffeine cache (security: permissions and
  administrators 30 s, profiles 60 s; devices: 30 s, unknown ones included, and a stale copy up
  to 1 h when the service does not answer; consumption levels: 10 min, stale copy up to 24 h; the
  energy is never cached).
- **`adapter/in/messaging`** (devices, measurements) or the notification and home consumers
  (security): the service's events, turned into the in-memory events the modules listen to.
- **`infrastructure`**: the wiring; no domain, no tables.

## Messaging

| Direction | Exchange | Routing keys | Here |
|---|---|---|---|
| in | `energy-monitor.security` | `account.*`, `mail.*` | notification and home consumers |
| in | `energy-monitor.device` | `device.connectivity-lost`, `device.connectivity-restored`, `device.linked`, `device.unlinked` | `device` proxy: alerts and cache |
| in | `energy-monitor.measurement` | `measurement.recorded` | `measurement` proxy: `MeasurementRecorded` for the alerts |
| out | `energy-monitor.home` | `home.deleted` | `home_outbox` (`home-007`), relayed every second |

Every queue is durable, with a dead-letter queue after three attempts; delivery is at least once
and the consumers tolerate duplicates. The outbox publishes with correlated confirms and mandatory
routing, oldest first, and stops at the first failure.

## Internal API

Calls between the services use `/internal/**`, an allowed network **and** `X-Internal-Token`; the
gateway never forwards them.

| Direction | Route |
|---|---|
| security → backend | `GET /internal/accounts/{id}/deletion-check` |
| device → backend | `GET /internal/homes/{homeId}/access?userId=` |
| measurement → backend | `GET /internal/homes/{homeId}/access?userId=`, `GET /internal/homes/{homeId}/thresholds` |
| backend → security | `/internal/users/{id}/permissions`, `/internal/users/profiles`, `/internal/administrators` |
| backend → device | `/internal/devices/{id}`, `/internal/homes/{homeId}/devices` |
| backend → measurement | `/internal/consumption-levels`, `/internal/measurements/hourly-energy`, `/internal/measurements/reporting-devices` |

## MQTT

This backend is not a broker client. The device service authenticates the broker, keeps the
connectivity and sends the commands; the measurement service subscribes to the telemetry and
stores the readings.

## Database

One MySQL schema shared by the services (ADR-003 of `en-monitor-docs`); each service owns its
tables and applies only its changelogs. Here: `home`, `alert`, `recommendation` and
`notification`. The `security-*`, `device-*` and `measurement-*` changelogs moved with their
services under the same paths, so `DATABASECHANGELOG` already lists them as applied. No foreign key crosses a service
boundary: other contexts are referenced by identifier.

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
| Home, alert, recommendation, notification | Migration of the four remaining modules to microservices | In progress (teammate) |

Everything above marked Done is in production since 24-Oct-2026.

## Working principles

1. A module reaches another only through its `api`.
2. Keep each domain free of Spring and JPA dependencies.
3. No business logic in controllers, consumers or JPA entities.
4. A context that moves out keeps its `api` here, answered by a proxy, until no module needs it.
