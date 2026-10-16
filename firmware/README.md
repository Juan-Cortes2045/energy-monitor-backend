# EnergyMonitor firmware (ESP32 + PZEM-004T V3)

Firmware for the **ESP32 + PZEM-004T V3** energy meter of the EnergyMonitor system. It lives in the
backend repository, next to the MQTT ingestion it talks to (it used to be the separate
energy-monitor-devices repository). The **MQTT broker** (Eclipse Mosquitto) is configured and run
from the [energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra)
repository, together with MySQL, the backend and the web app. The meter measures the electrical consumption of an appliance, is
linked to a home from the web app over Bluetooth, and publishes telemetry every 60 s over MQTT
with TLS. The broker authenticates every device against the backend, which stores the readings
and serves them to the web app.

```
[User's home]                                 [Server]
ESP32 + PZEM-004T ─Wi-Fi─► router ─internet─► Mosquitto :8883 (TLS)
                                                 │  ▲ auth / ACL: POST /internal/mqtt/auth/*
                                                 ▼  │
                                              Backend (Spring Boot) ──► MySQL
                                                 │ REST API (HTTPS)
Browser / phone of the user ◄───internet──────── Web app
```

The device only opens **outbound** connections, so it works behind any home router without
opening ports. In development the same picture runs on one PC.

Related repositories:

| Repository | Role |
|---|---|
| [energy-monitor-backend](https://github.com/Juan-Cortes2045/energy-monitor-backend) (this one, `firmware/`) | API, MQTT ingestion, broker authentication, alerts and notifications, and this firmware |
| [Energy-Monitor-application](https://github.com/Juan-Cortes2045/Energy-Monitor-application) | Web app (dashboard, device linking over Web Bluetooth) |
| [energy-monitor-infra](https://github.com/Juan-Cortes2045/energy-monitor-infra) | Docker: MySQL, broker, backend and web app (local and server) |

## Folder layout

Paths relative to `firmware/` of the backend repository.

| Path | Versioned | Content |
|---|---|---|
| `esp32-pzem-monitor/esp32-pzem-monitor.ino` | yes | Firmware |
| `esp32-pzem-monitor/config.h` | yes | Defaults and hardware settings. **No secrets** |
| `esp32-pzem-monitor/ca_certs.h` | yes | Let's Encrypt roots (ISRG Root X1 and X2), public certificates |
| `esp32-pzem-monitor/secrets.h.example` | yes | Template of `secrets.h` |
| `esp32-pzem-monitor/secrets.h` | **no** | Your setup-portal password and optional development defaults |
| `esp32-pzem-monitor/dev_ca.h` | **no** | Development CA of the broker, written by `infra/prepare.sh` |

The broker configuration (`mosquitto.conf`), its certificates and the compose files live in
`infra/` (see its README).

Everything marked **no** is in `.gitignore`. Never commit those files: they hold the api key of a
device, passwords or private keys.

---

## 1. Prerequisites

- **Docker** with Docker Compose v2 and **OpenSSL**, to run the `infra` stack (broker, MySQL,
  backend and web app).
- **Arduino toolchain**: [arduino-cli](https://arduino.github.io/arduino-cli/) or Arduino IDE 2,
  with the **esp32 by Espressif** core 3.x.
- An **ESP32 DevKit** (ESP32-WROOM), a **PZEM-004T V3** and a USB cable.
- The **infra**, **backend** (this one) and **web app** repositories, cloned side by side.
- To link from the web: **Chrome or Edge** on a computer, or **Chrome on Android**, with Bluetooth.

---

## 2. Step by step on a new machine (development)

The PC plays the server: MySQL, the backend, the broker and the web app run on it, and the ESP32
joins the same Wi-Fi.

### 2.1 Clone the repositories side by side

`infra` builds the backend and the web app from `../backend/energy-monitor-backend` and
`../frontend/energy-monitor-web`, and writes the development CA into the backend's
`firmware/esp32-pzem-monitor`. This layout works without changes:

```bash
mkdir energy-monitor && cd energy-monitor
git clone https://github.com/Juan-Cortes2045/energy-monitor-infra.git infra
git clone https://github.com/Juan-Cortes2045/energy-monitor-backend.git backend/energy-monitor-backend
git clone https://github.com/Juan-Cortes2045/Energy-Monitor-application.git frontend/energy-monitor-web
```

### 2.2 Start the stack (MySQL, broker, backend and web app)

Follow `infra/README.md`. In short:

```bash
cd infra
./prepare.sh                  # JWT keys, broker certificate and the firmware's dev_ca.h
# edit .env: passwords, MQTT_BACKEND_PASS and DEVICE_MQTT_PUBLIC_HOST
docker compose up -d --build
```

For the devices it matters that `infra/.env` sets:

```
DEVICE_MQTT_PUBLIC_HOST=<LAN IP of this PC>   # hostname -I (Linux) / ipconfig getifaddr en0 (macOS)
DEVICE_MQTT_PUBLIC_PORT=8883
```

`DEVICE_MQTT_PUBLIC_HOST` is the broker address the web app hands to a module when it is linked.
Never `localhost`: the ESP32 reaches the broker over the network.

### 2.3 Broker TLS certificate

`infra/prepare.sh` creates a development CA and the broker certificate in `infra/mosquitto/certs/`,
valid for the LAN IPs of this PC and for `DEVICE_MQTT_PUBLIC_HOST`, and writes
`esp32-pzem-monitor/dev_ca.h` so the firmware trusts that CA. **Build the firmware after running
it.** If the IP of the PC changes, update `DEVICE_MQTT_PUBLIC_HOST`, run it again and restart the
broker (`docker compose up -d --force-recreate mosquitto`): the CA is reused, so the devices do
**not** need to be flashed again.

### 2.4 Check the broker

```bash
cd infra
docker compose logs -f mosquitto
```

The backend shows up as two clients ending in `-telemetry` and `-status` (`energy-monitor-backend-local-…` in the full stack, `energy-monitor-backend-…` when it runs on the host).

### 2.5 Arduino toolchain (once)

```bash
arduino-cli core update-index
arduino-cli core install esp32:esp32
arduino-cli lib install PZEM004Tv30 PubSubClient ArduinoJson WiFiManager NimBLE-Arduino
```

| Library | Author | Tested version |
|---|---|---|
| `PZEM004Tv30` | Jakub Mandula | 1.2.1 |
| `PubSubClient` | Nick O'Leary | 2.8 |
| `ArduinoJson` | Benoit Blanchon | 7.x |
| `WiFiManager` | tzapu | 2.0.17 |
| `NimBLE-Arduino` | h2zero | 2.5.1 |

Do not confuse `PZEM004Tv30` with the old `PZEM-004T` library (v1/v2).

### 2.6 Your `secrets.h`

```bash
cd backend/energy-monitor-backend/firmware/esp32-pzem-monitor
cp secrets.h.example secrets.h
```

Set at least **`PORTAL_PASSWORD`** (8+ characters): it protects the `EnergyMonitor-Setup` access
point, and the default in `config.h` is public because the repository is. The identity and broker
lines are optional: the web app writes them when the module is linked.

### 2.7 Build and flash

The partition scheme must be **Minimal SPIFFS (1.9MB APP with OTA)**: with Bluetooth the binary
(~1.4 MB) does not fit in the default 1.2 MB. The NVS stays at the same address, so changing the
scheme keeps the saved configuration.

```bash
cd backend/energy-monitor-backend/firmware/esp32-pzem-monitor
arduino-cli compile --fqbn esp32:esp32:esp32:PartitionScheme=min_spiffs .
arduino-cli upload  --fqbn esp32:esp32:esp32:PartitionScheme=min_spiffs -p /dev/ttyUSB0 .
arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=115200
```

In Arduino IDE: board **ESP32 Dev Module**, *Partition Scheme* **Minimal SPIFFS**. Close any other
serial monitor first, or the port is busy. On Linux the user needs access to the port
(`sudo usermod -aG dialout $USER`, then log in again).

### 2.8 Link the module from the web app

Register the device from the web app; no manual database insert is needed.

1. Web app → home → **Devices** → **Link device** (owner only).
2. Hold **BOOT** on the ESP32 for 3 s until the LED blinks (a module never linked is already in
   setup mode after flashing).
3. **Find module** → the browser shows `EnergyMonitor-<code>` → pick it.
4. Choose the appliance, the name and the room.
5. Choose the home Wi-Fi from the list (they are the networks the ESP32 sees) and type its password.
6. The web app registers the device in the backend, which issues its `api_key`, and sends the
   module over Bluetooth: network, password, server (`DEVICE_MQTT_PUBLIC_HOST`), `idDevice` and
   `api_key`. The progress shown is the module's real one: Wi-Fi → password → server.
7. Once connected to the broker the module leaves setup mode on its own.

Notes:

- On **Linux**, Chrome ships Web Bluetooth disabled: enable
  `chrome://flags/#enable-experimental-web-platform-features` and relaunch.
- Web Bluetooth only works on `https://` or `http://localhost`.
- Linking the same module again keeps its identity and history and issues a new `api_key`. A
  module linked to another home is refused.

**Change the Wi-Fi of a linked module** (moving house, a new Wi-Fi password, a new internet
provider): web app → home → **Devices** → the refresh icon of the device (owner only). It is not a
new link: no appliance step, history and alerts stay, and no "device linked" notice is sent.

1. Without its network the module turns Bluetooth on by itself after 5 minutes; to skip the wait,
   hold **BOOT** for 3 s.
2. **Find module** and pick `EnergyMonitor-<code>`: the web app refuses any other module.
3. Pick the new network and type its password. The backend issues a new `api_key` (the old one
   stops working) and the web app writes network, server and key as in linking.

The pencil icon edits the appliance, name and room; it does not touch the module.

**Fallback without Web Bluetooth** (Safari, Firefox): join the `EnergyMonitor-Setup` Wi-Fi
(your `PORTAL_PASSWORD`), open `http://192.168.4.1` → **Configure WiFi**, and fill in the network
plus:

| Field | Development | Production |
|---|---|---|
| MQTT server | LAN IP of the PC | broker domain, e.g. `mqtt.example.com` |
| MQTT port | 8883 | 8883 |
| Device ID | `idDevice` of the device | same |
| Device code | `deviceCode` | same |
| API key | `apiKey` | same. **Empty = keep the saved one** |

This path requires the device to exist in the backend already (with its api key).

Expected serial output:

```
=== EnergyMonitor ESP32 + PZEM-004T V3 fw=1.3.8 ===
[PZEM] UART2 ready at 9600 baud, addr=0xf8
[TLS] trusting Let's Encrypt and the development CA
[CFG] deviceId=DEV0000042 deviceCode=EM1A2B broker=192.168.1.50:8883 (TLS) apiKey=set
[WiFi] connected to 'MyHome', IP=192.168.1.37 RSSI=-58dBm
[NTP] time synchronized
[MQTT] connected as esp32-DEV0000042
[PZEM] 231.4V 1.234A 285.5W 12.847kWh @2026-10-05T20:17:03Z
[MQTT] telemetry published
```

### 2.9 Verify the whole chain

In MySQL (`docker exec -it energy-monitor-mysql mysql -u energy_app -p energy_monitor`):

```sql
SELECT * FROM measurement WHERE device_id = '<idDevice>' ORDER BY date_time DESC LIMIT 5;
SELECT * FROM device_status_log WHERE device_id = '<idDevice>' ORDER BY last_seen DESC;
```

- One `ONLINE` row whose `last_seen` moves every minute.
- Unplug the ESP32 → after ~22 s (broker last will) an `OFFLINE` row, and a "device disconnected"
  alert in the web app.
- Plug it back → a new `ONLINE` row, the alert resolves itself, the offline queue is flushed.

Simulating a device without hardware (the certificate includes `localhost`):

```bash
docker exec energy-monitor-mqtt mosquitto_pub -h localhost -p 8883 \
  --cafile /mosquitto/certs/ca.crt -u <deviceCode> -P '<apiKey>' \
  -t energy-monitor/devices/<idDevice>/telemetry \
  -m '{"deviceId":"<idDevice>","voltage":120.1,"current":0.5,"activePower":60,"storedEnergy":1.2,"rssi":-60}'
```

---

## 3. MQTT contract

| Parameter | Value |
|---|---|
| Protocol | MQTT 3.1.1 |
| Port | **8883, TLS** (devices). 1883 without TLS for the backend only, never exposed |
| Client ID | `esp32-{deviceId}` |
| Username / password | `device_code` / `api_key` of the `device` table |
| Keep alive | 15 s (the broker publishes the last will ~22 s after a power cut) |
| QoS | **0 everywhere**: PubSubClient limitation, see "Known limitations" |
| Reconnection | backoff 2 s → 30 s, without user intervention (FRS5.4) |
| Cadence | telemetry every 60 s (FRS5.1), status every 5 min and on connect |

### Topics

| Topic | QoS | Retained | Content |
|---|---|---|---|
| `energy-monitor/devices/{deviceId}/telemetry` | 0 | no | Electrical reading |
| `energy-monitor/devices/{deviceId}/status` | 0 | yes | `online` / `offline` (also the last will), RSSI and IP |
| `energy-monitor/devices/{deviceId}/command` | 1 | yes | written by the backend, read by the module: `{"cmd":"unlink"}` when it is removed from its home (it forgets its key and offers Bluetooth at once); an empty message clears it when linked again |

The broker only lets a device **publish on its own two topics**: it cannot read anything or
impersonate another device. The backend takes the `deviceId` from the topic, not from the payload,
and drops a message whose `deviceId` does not match.

### Telemetry payload

```json
{
  "deviceId": "DEV0000042",
  "dateTime": "2026-10-05T20:17:03Z",
  "voltage": 231.4,
  "current": 1.234,
  "activePower": 285.5,
  "storedEnergy": 12.847,
  "frequency": 60,
  "powerFactor": 0.98,
  "rssi": -58,
  "sequence": 1791251823
}
```

| JSON | `measurement` column | Type | Unit |
|---|---|---|---|
| `deviceId` | `device_id` | VARCHAR(10) | — |
| `dateTime` | `date_time` | TIMESTAMP | ISO 8601 UTC, **time of the sample** |
| `voltage` | `voltage` | DOUBLE | V |
| `current` | `current` | DOUBLE | A |
| `activePower` | `active_power` | DOUBLE | W |
| `storedEnergy` | `stored_energy` | DOUBLE | kWh (cumulative counter of the PZEM) |

- Samples from the offline queue arrive late but with their real time.
- A sample taken before the clock was set is dated when sent (current time minus the time elapsed
  since it was taken). If there is still no time, `dateTime` is omitted and the backend uses the
  reception time instead of storing it in 1970.
- `frequency` and `powerFactor` are not in the schema; the backend ignores them.

### Status payload

```json
{
  "deviceId": "DEV0000042",
  "deviceCode": "EM1A2B",
  "deviceName": "Main meter",
  "location": "Room 1",
  "status": "online",
  "rssi": -58,
  "ip": "192.168.1.37",
  "firmwareVersion": "1.3.8",
  "uptimeSeconds": 12345
}
```

### What the backend does with them

1. **Tolerates gaps.** At QoS 0 there are no duplicates, but a reading can be lost.
2. **`device_status_log` keeps transitions.** While a device stays online each report only
   refreshes `last_seen` and `signal_strength` of the current row.
3. **`OFFLINE` on last will or on silence**: immediately with the broker's last will, or after
   `device.connectivity.offline-after` (150 s) without news.
4. **No downlink.** The backend never sends commands to the device (`scope.md:38`).

### Broker authentication

Mosquitto stores no users: the `mosquitto-go-auth` plugin asks the backend on every connection
and every topic access (cached for 5 min).

| Call | Body | 200 = allowed, 403 = denied |
|---|---|---|
| `POST /internal/mqtt/auth/user` | `{username, password, clientid}` | device: `device_code` + `api_key`; backend: `MQTT_BACKEND_USER` + `MQTT_BACKEND_PASS` |
| `POST /internal/mqtt/auth/acl` | `{username, clientid, topic, acc}` | device: write on its own topics only; backend: subscribe and read only |

- **Registering a device in the backend is enough** for it to connect; the broker is never touched.
- Revoking a device (unlinking it rotates its `api_key`) takes at most the 5 min of cache.
- `/internal/**` only answers the networks in `MQTT_AUTH_ALLOWED_NETWORKS` (loopback and the Docker
  networks by default). A public reverse proxy must **not** forward it.

### Alerts and notifications

Raised by the backend from what the devices report, and delivered to every member of the home in
the web app, by mail and by browser push (per user, in **Settings → Notifications**):

| Alert | `message_key` | Raised | Closed |
|---|---|---|---|
| Device linked (notice) | `alert.device.linked` | first reading after the module is linked from the web | the user **marks it as read** |
| Limit reached | `alert.limit.daily` / `.monthly` | the home's energy of the day / month reaches the limit in the thresholds | **by itself**, in a new period or with a higher limit |
| Device disconnected | `alert.connectivity.offline` | last will (~22 s after the cut) or 150 s without data | **by itself**, when the module reports again |
| High / critical consumption | `alert.threshold.high` / `.critical` | a reading in the HIGH or CRITICAL level | **by itself**, with the first LOW or MEDIUM reading |

---

## 4. Bluetooth linking protocol

GATT service `8f2a0001-5e8c-4a7b-9c3d-2f1e0b7a6c51` (UUIDs in `config.h` and in the web app,
`src/services/devices/bleProvisioning.js`). Advertised name: `EnergyMonitor-<deviceCode>`.

| Characteristic | Operation | Content |
|---|---|---|
| `...0002` INFO | read | `{"code","id","fw","cfg"}` |
| `...0003` NETWORKS | read / write | `[{"s":ssid,"r":rssi,"e":0\|1}]`; write anything = scan again |
| `...0004` CONFIG | write | JSON `{ssid,pass,host,port,id,key}` in chunks: `+`chunk, `=`last chunk, `!`reset |
| `...0005` STATUS | read / notify | `scan_done`, `wifi_connecting`, `wifi_ok`, `wifi_failed[:auth\|:notfound]`, `mqtt_connecting`, `mqtt_ok`, `mqtt_failed:<rc>`, `bad_config` |

Setup mode starts on boot without configuration, when **BOOT** is held for 3 s, or when the saved
network is missing (Bluetooth only, see "Failure behaviour"). It lasts up to 10 min, without
stopping measuring. It does not require pairing: anyone nearby could write a configuration **only
while it is active**.

---

## 5. Hardware

### Wiring

| PZEM-004T V3 | ESP32 | Note |
|---|---|---|
| TX | GPIO16 (RX2) | Cross TX/RX |
| RX | GPIO17 (TX2) | |
| VCC | 5V | see note |
| GND | GND | common with the PZEM |

Modbus RTU, 9600 8N1, default address `0xF8`. Configurable in `config.h`
(`PZEM_RX_PIN`, `PZEM_TX_PIN`, `PZEM_BAUD`, `PZEM_ADDRESS`). The **BOOT** button (GPIO0) opens setup
mode when held for 3 s.

> **Power:** some PZEM-004T v3 clones work at 3.3 V, others drop the UART. If it does not answer,
> power the module at 5 V from the ESP32 VIN.
>
> **Isolation:** the PZEM-004T is not galvanically isolated and its load terminal must never be
> touched with the low-voltage side. Do not handle the mains wiring with the prototype powered.

| Component | Role |
|---|---|
| ESP32 DevKit v1 | Wi-Fi connectivity and processing |
| PZEM-004T V3 | Measurement of the real electrical magnitudes |
| HLK-PM03 | AC/DC supply from the same circuit it measures |

---

## 6. Failure behaviour

| Situation | Firmware response |
|---|---|
| PZEM does not answer | 3 retries, then the sample is discarded. **Zeros are never published** |
| Wi-Fi drops | Automatic reconnection; samples keep being queued |
| Saved network does not answer at boot (e.g. the router is slower than the ESP32 after a power cut) | Retries the saved network every 30 s with Bluetooth available; leaves setup mode on its own once it reaches the broker. The portal is not opened: it would shut the station connection down |
| Wi-Fi stuck | Every 30 s the attempt is dropped and restarted; every 2 min the radio is reset; after 10 min without network the ESP32 restarts (the queue is lost, it never stays disconnected) |
| No Wi-Fi for 5 min | Bluetooth setup mode to set another network, while still retrying. BOOT 3 s also opens the portal |
| Broker or backend down | Keeps sampling; up to 30 samples (30 min) are queued; backoff 2 s → 30 s |
| Queue full | The oldest sample is dropped, recent data wins |
| No NTP time | No TLS connection (the certificate cannot be validated); keeps sampling and queueing. Samples are dated when the time arrives |
| Not linked | Setup mode on boot |

The offline queue is mandatory: `docs/en-monitor-docs/01-context/scope.md:80`.

---

## 7. Deployment

The server deployment (MySQL, backend, broker with a Let's Encrypt certificate and web app) is
`infra/docker-compose.prod.yml`; see "Production" in `infra/README.md`. The firmware needs no
change: it trusts the Let's Encrypt roots, and each device gets the broker domain when it is linked
from the web app.

---

## 8. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `[MQTT] connection failed, rc=-2` + a `[TLS]` line | Broker unreachable or certificate not valid for the current IP: check `DEVICE_MQTT_PUBLIC_HOST` in `infra/.env`, re-run `infra/prepare.sh`, restart the broker |
| `[TLS] X509 - Certificate verification failed` and the broker logs `tlsv1 alert unknown ca` | The firmware was built with another development CA: build and flash it again from the PC whose `infra/mosquitto/certs/ca.crt` matches `dev_ca.h` |
| `rc=4` / `rc=5` | `device_code` / `api_key` do not match the `device` table, or the backend is down: link the module again |
| Broker logs `context deadline exceeded` calling `backend:8080` | Backend stopped; with the backend on the host (`docker-compose.dev.yml`), the ufw rule in `infra/README.md` is missing |
| Broker container exits with a permission error on `server.key` | The container reads it as uid 1000: `sudo chown 1000 infra/mosquitto/certs/server.key` |
| The web app does not find the module | Hold BOOT 3 s (LED blinking); on Linux enable the Chrome flag of 2.8; open the web app on `localhost`, not the LAN IP |
| `wifi_failed:auth` while linking | Wrong Wi-Fi password |
| Upload fails: port busy | Close the Arduino IDE serial monitor or any other program using `/dev/ttyUSB0` |
| Sketch too big | Partition scheme is not Minimal SPIFFS |

---

## 9. Known limitations

- **QoS 0 only.** PubSubClient always publishes at QoS 0 (the last will too). There are no
  duplicates but a reading can be lost. Move to `espMqttClient` if guaranteed delivery is required.
- **No Web Bluetooth on Safari or Firefox**: the Wi-Fi portal is used there.
- **Setup mode is not authenticated over Bluetooth**: limited to 10 min and to a physical gesture (BOOT).
- **`api_key` stored in clear text** in the `device` table (compared in constant time).
- **No downlink or OTA**: updating the firmware needs a USB cable.
- **One PZEM per board** (`Serial2`).

## 10. Next steps

1. Record this contract as `ADR-008-mqtt-device-contract.md` in
   `docs/en-monitor-docs/05-architecture/decisions/records/`.
2. Evaluate `espMqttClient` for QoS 1.
3. Hash `device.api_key` (the broker already asks the backend, only the comparison changes).
4. OTA updates.
