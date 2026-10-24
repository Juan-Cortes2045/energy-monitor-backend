# Architecture

`energy-monitor-backend` is a **modular monolith** (Spring Modulith). Each bounded context is a
top-level package under `com.energymonitor`, organised internally as Hexagonal Architecture
(`domain`, `application`, `adapter`, `infrastructure`), and exposes to the others only what is in
its `api` package. `ModularityTests` fails the build when a module reaches past another one's
`api` or depends on one it does not declare.

Two contexts have moved out to their own services: **security** (`en-monitor-security-service`,
:8082) and **devices** (`en-monitor-device-service`, :8084). Their packages stay here as thin
proxies, so the modules that use them did not change.

## Modules

| Module | Owns | May use |
|---|---|---|
| `home` | Homes, memberships, home roles (`OWNER`, `MEMBER`), consumption thresholds | `security::api` |
| `measurement` | Ingestion of the readings, history, statistics, consumption levels | `device::api`, `home::api` |
| `alert` | Threshold and connectivity alerts | `home::api`, `device::api`, `measurement::api` |
| `recommendation` | Hourly consumption advice | `device::api`, `measurement::api`, `home::api` |
| `notification` | Mail, web push and mobile push delivery, delivery log, administrator notices | `security::api`, `home::api`, `device::api`, `alert::api`, `recommendation::api` |
| `security` | Proxy of the security service: token validation, admin permission checks, profiles and administrators | nothing |
| `device` | Proxy of the device service: cached device reads, device events | nothing |

```
                 ┌──────── security::api ◄─────── home, notification
 security proxy ─┤
                 └── adapter/out/remote ──► en-monitor-security-service /internal/**

                 ┌──────── device::api ◄───────── measurement, alert, recommendation, notification
 device proxy ───┤
                 ├── adapter/out/remote ──► en-monitor-device-service /internal/devices|homes/**
                 └── adapter/in/messaging ◄── energy-monitor.device (RabbitMQ)
```

## The proxies

Both follow the same shape:

- **`api`**: the contract the other modules already used (`UserProfileQuery`, `AdministratorQuery`;
  `DeviceApi`, `DeviceConnectivityLost`, `DeviceConnectivityRestored`), unchanged by the
  extraction, plus an "unavailable" exception for when the service does not answer.
- **`adapter/out/remote`**: an HTTP client of the service's internal API, with
  `X-Internal-Token`, short timeouts and a Caffeine cache (security: permissions and
  administrators 30 s, profiles 60 s; devices: 30 s, unknown ones included, and a stale copy up
  to 1 h when the service does not answer).
- **`adapter/in/messaging`** (devices) or the notification and home consumers (security): the
  service's events, turned into the in-memory events the modules listen to.
- **`infrastructure`**: the wiring; no domain, no tables.

## Messaging

| Direction | Exchange | Routing keys | Here |
|---|---|---|---|
| in | `energy-monitor.security` | `account.*`, `mail.*` | notification and home consumers |
| in | `energy-monitor.device` | `device.connectivity-lost`, `device.connectivity-restored`, `device.linked`, `device.unlinked` | `device` proxy: alerts and cache |
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
| backend → security | `/internal/users/{id}/permissions`, `/internal/users/profiles`, `/internal/administrators` |
| backend → device | `/internal/devices/{id}`, `/internal/homes/{homeId}/devices` |

## MQTT

This backend is a read-only broker client (`MQTT_BACKEND_USER`): it subscribes to the telemetry
of every device and stores a reading once `DeviceApi` confirms the device. The device service
authenticates the broker, keeps the connectivity and sends the commands.

## Database

One MySQL schema shared by the services (ADR-003 of `en-monitor-docs`); each service owns its
tables and applies only its changelogs. Here: `home`, `measurement`, `alert`, `recommendation` and
`notification`. The `security-*` and `device-*` changelogs moved with their services under the
same paths, so `DATABASECHANGELOG` already lists them as applied. No foreign key crosses a service
boundary: other contexts are referenced by identifier.

## Migration status

| Step | Content | Status |
|---|---|---|
| Security service | Module moved out; events, internal API, proxy here, gateway in front | Done |
| Device service: reads | `DeviceApi` answered by the device service's internal API, cached | Done |
| Device service: events | Connectivity and link events consumed here; `home.deleted` published from `home_outbox` | Done |
| Device service: removal | Device module removed; broker authentication, commands and connectivity there | Done |
| Device service: deployment | Gateway routes, infra container, Mosquitto pointing at the device service | Done |
| End-to-end check | Whole stack in Docker | In progress |

## Working principles

1. A module reaches another only through its `api`.
2. Keep each domain free of Spring and JPA dependencies.
3. No business logic in controllers, consumers or JPA entities.
4. A context that moves out keeps its `api` here, answered by a proxy, until no module needs it.
