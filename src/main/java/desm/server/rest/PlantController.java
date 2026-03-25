package desm.server.rest;

import desm.server.PlantRegistry;
import desm.server.model.PlantRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints used by the thermal power plants to register and by the
 * administration client to query active plants.
 *
 * <pre>
 *  POST /plants/register    — plant calls this at startup
 *  GET  /plants             — returns all active plants
 * </pre>
 */
@RestController
@RequestMapping("/plants")
public class PlantController {

    private final PlantRegistry registry;

    public PlantController(PlantRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register a new plant.
     *
     * <p>Request body (JSON):
     * <pre>{ "id": 1, "address": "localhost", "port": 5001 }</pre>
     *
     * <p>Returns 201 with the list of already-registered peers on success,
     * or 409 if the ID is already taken.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        int    id      = ((Number) body.get("id")).intValue();
        String address = (String) body.get("address");
        int    port    = ((Number) body.get("port")).intValue();

        List<PlantRecord> existing = registry.register(new PlantRecord(id, address, port));
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Plant ID " + id + " already registered");
        }
        System.out.printf("[Server] ✅ Plant #%d registered (%s:%d). Peers: %d%n",
                id, address, port, existing.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(existing);
    }

    /**
     * Return the list of all currently active plants.
     */
    @GetMapping
    public ResponseEntity<List<PlantRecord>> listPlants() {
        return ResponseEntity.ok(registry.getAll());
    }
}
