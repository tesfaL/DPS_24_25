# DESM — Distributed Energy Supply Management

> A simulated renewable-energy supply network built with **Java 11**, **gRPC**, **Spring Boot**, and **MQTT (Mosquitto)**.
> University project for the *Distributed and Pervasive Systems* lab course — A.Y. 2024/25.

---

## Overview

DESM models a renewable-energy provider that cannot always meet peak demand and therefore relies on a network of **thermal power plants** that compete (via a distributed election algorithm) to fill each shortage request.

```
┌─────────────────────┐          MQTT          ┌──────────────────────┐
│  Renewable Energy   │ ──── energy/request ──▶ │  Thermal Power Plant │
│     Provider        │                         │   (x4 or more)       │
└─────────────────────┘                         └────────┬─────────────┘
                                                         │ gRPC (Chang-Roberts)
                                           ◀─────────────┘
┌─────────────────────┐   REST (Spring Boot)   ┌──────────────────────┐
│  Administration     │ ◀────────────────────▶ │  Administration      │
│     Client (CLI)    │                         │     Server           │
└─────────────────────┘         MQTT            └──────────────────────┘
                           ◀─ pollution/+  ─────────────────────────────
```

### Components

| Component | Class | Description |
|-----------|-------|-------------|
| Administration Server | `desm.server.AdministrationServer` | Spring Boot REST API + MQTT subscriber |
| Thermal Power Plant | `desm.plant.ThermalPowerPlant` | gRPC peer process with sensor & election |
| Renewable Energy Provider | `desm.provider.RenewableEnergyProvider` | MQTT publisher |
| Administration Client | `desm.client.AdministrationClient` | Colourised CLI |

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 11+ | Build & run |
| Gradle | 7.6 (wrapper included) | Build tool |
| Mosquitto | 2.x | MQTT broker |

Install Mosquitto:
```bash
# macOS
brew install mosquitto

# Ubuntu/Debian
sudo apt-get install mosquitto mosquitto-clients
```

---

## Build

```bash
# Clone the repository
git clone https://github.com/tesfaL/DPS_24_25.git
cd DPS_24_25

# Build (this also generates gRPC stubs from src/main/proto/desm.proto)
./gradlew build
```

---

## Running the System

Start components **in this exact order** (each in its own terminal):

### 1 — MQTT Broker
```bash
mosquitto -v
```

### 2 — Administration Server
```bash
./gradlew run --main desm.server.AdministrationServer
# or via the fat-jar:
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.server.AdministrationServer
```
REST API available at `http://localhost:8080`.

### 3 — Thermal Power Plants (at least 4)
```bash
# Each plant needs a unique ID and a unique gRPC port
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.plant.ThermalPowerPlant 1 5001
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.plant.ThermalPowerPlant 2 5002
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.plant.ThermalPowerPlant 3 5003
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.plant.ThermalPowerPlant 4 5004
```

### 4 — Renewable Energy Provider
```bash
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.provider.RenewableEnergyProvider
```
Energy requests are published every **10 seconds**.

### 5 — Administration Client
```bash
java -cp build/libs/DESM-1.0-SNAPSHOT.jar desm.client.AdministrationClient
```

---

## REST API Reference

### Plant Registration
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/plants/register` | Register a new plant (called by plant at startup) |
| `GET`  | `/plants`          | List all active plants |

**POST /plants/register** — request body:
```json
{ "id": 1, "address": "localhost", "port": 5001 }
```
Returns `201` with the list of existing peers, or `409` if the ID is taken.

### Statistics
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/stats/co2?t1=<ms>&t2=<ms>` | Average CO2 in the given time window |

---

## Architecture Details

### Election: Chang-Roberts Ring Algorithm
When a new energy request arrives via MQTT, each non-busy plant:
1. Generates a random bid in **[0.1, 0.9] $/kWh**.
2. Sends an `ElectionToken` to its **clockwise neighbour** in the ring (sorted by plant ID).
3. If the received token has a lower bid (or equal bid + higher ID), it forwards it; otherwise it suppresses it.
4. The token eventually completes the ring — the surviving plant wins and broadcasts a `CoordinatorAnnouncement`.

The winner simulates production by sleeping for `kwhAmount` milliseconds, then releases its busy lock.

### Pollution Sensor — Sliding Window
Each plant's sensor simulator produces a measurement every 500 ms.  
`SlidingWindowBuffer` maintains a window of **8 readings** with **50 % overlap** (step = 4):
- When 8 measurements accumulate → compute average → slide by 4.
- Every 10 s the plant publishes all accumulated averages to `desm/pollution/<id>` via MQTT.

### MQTT Topics
| Topic | Publisher | Subscriber |
|-------|-----------|------------|
| `desm/energy/request` | Renewable Energy Provider | All plants |
| `desm/pollution/<id>` | Each plant | Administration Server |

---

## Project Structure

```
DPS_24_25/
├── src/main/
│   ├── java/desm/
│   │   ├── common/          # Shared models & config
│   │   ├── provider/        # Renewable Energy Provider
│   │   ├── plant/           # Thermal Power Plant
│   │   │   ├── election/    # Chang-Roberts election manager
│   │   │   ├── grpc/        # gRPC service implementation
│   │   │   └── sensor/      # Pollution sensor simulator
│   │   ├── server/          # Administration Server
│   │   │   ├── rest/        # Spring Boot controllers
│   │   │   ├── model/       # Data models
│   │   │   └── mqtt/        # MQTT subscriber
│   │   └── client/          # Administration Client CLI
│   ├── proto/
│   │   └── desm.proto       # gRPC service definition
│   └── resources/
│       └── application.properties
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
└── DPS_Project_2025.pdf
```

---

## Notes

- No optional parts are implemented (controlled/uncontrolled plant exit — see Section 12 of the spec).
- All synchronisation uses only `synchronized`/`wait`/`notify` as required by the course.
- Broadcast gRPC calls (coordinator announcement, introductions) are always sent **in parallel** threads.
