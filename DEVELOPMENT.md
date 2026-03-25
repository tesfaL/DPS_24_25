# DEVELOPMENT — Implementation Roadmap & Creative Enhancements

This document is the development companion to `README.md`. It explains design decisions, lists creative enhancements beyond the minimum spec, and provides a phased roadmap for implementing DESM.

---

## Design Philosophy

> **"Clarity over cleverness, correctness over performance."**

The project requirement explicitly states that synchronisation structures must be implemented from scratch (no `java.util.concurrent`). The design therefore emphasises:

- **Explicit `synchronized` blocks** — every shared data structure documents which monitor it uses.
- **Short critical sections** — locking is never held across I/O or gRPC calls.
- **Thread naming** — every thread has a descriptive name (`sensor-sim-plant-1`, `grpc-fwd-3`, etc.) to make thread dumps readable.
- **Fail-fast with graceful logging** — exceptions are caught, logged, and never silently swallowed.

---

## Package Responsibilities

```
desm.common        Shared value objects (Measurement, PlantInfo), constants (Config, Topics)
desm.provider      MQTT publisher — standalone process, zero shared state with plants
desm.plant         Thermal plant: sensor thread, gRPC server, MQTT client, election manager
desm.plant.sensor  Pluggable sensor simulator (matches the Buffer interface from the spec)
desm.plant.grpc    Generated stubs + thin service impl that delegates to ElectionManager
desm.plant.election  Chang-Roberts algorithm, fully encapsulated
desm.server        Spring Boot app: REST controllers + MQTT subscriber + in-memory registry
desm.server.model  Immutable data carriers (PlantRecord, PollutionReading)
desm.client        Single-class CLI; no shared state, pure read queries
```

---

## Creative Enhancements

### 1. Colourised Console Output
Each plant is assigned a unique ANSI colour (cycling for > 8 plants) and a code-name from a list of Italian cities. This transforms a wall of identical-looking log lines into easily-distinguishable streams when running 4–5 plant processes in tiled terminal windows.

```
[Plant #1 Alba       ] ⚡ Energy request received: 12340 kWh
[Plant #2 Bergamo    ] ⚡ Energy request received: 12340 kWh
[Plant #1 Alba       ] 🏆 WON election! Producing 12340 kWh at $0.2341/kWh…
```

### 2. CO2 Traffic-Light in the Admin Client
The administration client colour-codes the reported CO2 average:
- 🟢 **< 220 g** — Normal
- 🟡 **220–300 g** — Elevated
- 🔴 **> 300 g** — High

### 3. Plant Code-Names
Plants are assigned names (`Alba`, `Bergamo`, `Como`, …) based on `id % 8`. The code-name appears in all log messages, making it effortless to cross-reference process output with the ID used in gRPC messages.

### 4. Self-documenting `Main` launcher
Running the project JAR with no arguments prints a colour-coded startup guide listing the correct component start order — useful during the project presentation.

### 5. Per-plant CO2 baselines
Each sensor simulator uses a plant-specific baseline CO2 value (`200 + id × 15 g`) with Gaussian noise. This makes the statistics query more interesting during demos (plants have visibly different emission profiles).

---

## Phased Implementation Roadmap

### Phase 0 — Infrastructure ✅
- [x] Gradle project setup (Java 11, gRPC, Spring Boot, MQTT)
- [x] `.gitignore` and repository cleanup
- [x] Shared constants (`Config`, `Topics`)
- [x] Protobuf/gRPC service definition (`desm.proto`)

### Phase 1 — Core Data Structures ✅
- [x] `Measurement` — immutable CO2 reading
- [x] `Buffer` interface (as provided by the course)
- [x] `SlidingWindowBuffer` — thread-safe, 8-measurement window, 50 % overlap

### Phase 2 — Administration Server ✅
- [x] `PlantRegistry` — synchronized in-memory store
- [x] `PlantController` — POST `/plants/register`, GET `/plants`
- [x] `StatsController` — GET `/stats/co2?t1=&t2=`
- [x] `ServerMqttSubscriber` — subscribes to `desm/pollution/+`

### Phase 3 — Renewable Energy Provider ✅
- [x] Publishes random kWh requests (5 000–15 000) every 10 s on MQTT

### Phase 4 — Thermal Power Plant ✅
- [x] REST registration at startup (receives peer list)
- [x] gRPC server starts on the plant's port
- [x] Sensor simulator thread + `SlidingWindowBuffer`
- [x] Parallel gRPC introductions to existing peers
- [x] MQTT subscription to energy requests
- [x] Pollution reporter thread (every 10 s)
- [x] `ElectionManager` — Chang-Roberts ring election
- [x] Production simulation (`Thread.sleep(kwhAmount)`)

### Phase 5 — Administration Client ✅
- [x] Menu-driven CLI
- [x] Colourised plant table
- [x] CO2 query with traffic-light colour coding

### Phase 6 — Integration Testing (recommended before presentation)
- [ ] Launch 4–5 plants and observe elections in terminal output
- [ ] Confirm only one plant wins per request (check `🏆` appears exactly once)
- [ ] Verify pollution data arrives at the server (check admin client CO2 query)
- [ ] Add a 6th plant mid-session and verify it joins the ring and wins elections

---

## Known Limitations (by design)

| Limitation | Reason |
|------------|--------|
| No plant exit (optional part 1 & 2) | Explicitly excluded from scope |
| All addresses hardcoded to `localhost` | Per project specification |
| No TLS on gRPC or REST | Out of scope for this lab course |
| In-memory registry lost on server restart | Persistence not required by the spec |

---

## Synchronisation Diagram

```
MQTT thread             ElectionManager (synchronized)        gRPC server thread
     │                          │                                    │
     │── startElection() ──────▶│                                    │
     │                          │─── forwardTokenAsync() ──────────▶ peer's gRPC
     │                          │                                    │
     │                          │◀── receiveToken() ─────────────────│
     │                          │                                    │
     │                          │ (if token completes ring)          │
     │                          │─── broadcastCoordinator() ────────▶ all peers
     │                          │─── callback.onWin() ──────────────▶ production thread
```

All `ElectionManager` methods are `synchronized` on the same monitor, so token receipt and election start are mutually exclusive. The production thread releases the `busy` flag after sleeping, allowing the plant to participate in subsequent elections.
