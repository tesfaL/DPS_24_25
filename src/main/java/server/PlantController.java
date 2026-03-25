package server;

import model.PlantInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for thermal power plant initialization.
 *
 * POST /plants  – register a new plant; returns the list of existing plants.
 * GET  /plants  – return all registered plants.
 */
@RestController
@RequestMapping("/plants")
public class PlantController {

    @Autowired
    private PlantRegistry registry;

    /**
     * Register a new thermal power plant.
     * Body: { "id": <int>, "address": "<string>", "port": <int> }
     * Response 200: list of plants already in network (before this registration).
     * Response 409: a plant with the same ID already exists.
     */
    @PostMapping
    public ResponseEntity<?> registerPlant(@RequestBody PlantInfo newPlant) {
        if (newPlant.getId() <= 0 || newPlant.getAddress() == null || newPlant.getPort() <= 0) {
            return ResponseEntity.badRequest().body("Invalid plant information.");
        }

        List<PlantInfo> existing = registry.addPlant(newPlant);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A plant with ID " + newPlant.getId() + " is already registered.");
        }

        System.out.println("[Server] Plant " + newPlant.getId() + " registered. "
                + "Network now has " + (existing.size() + 1) + " plant(s).");
        return ResponseEntity.ok(existing);
    }

    /**
     * Return the list of all currently registered plants.
     */
    @GetMapping
    public ResponseEntity<List<PlantInfo>> getPlants() {
        return ResponseEntity.ok(registry.getAllPlants());
    }
}
