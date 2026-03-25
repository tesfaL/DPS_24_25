package server;

import model.PlantInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe registry of thermal power plants.
 * Only synchronized blocks / methods are used for concurrency control.
 */
@Service
public class PlantRegistry {

    // plantId -> PlantInfo
    private final Map<Integer, PlantInfo> plants = new HashMap<>();

    /**
     * Register a new plant.
     *
     * @return the list of plants already present before this registration,
     *         or null if a plant with the same ID already exists.
     */
    public synchronized List<PlantInfo> addPlant(PlantInfo newPlant) {
        if (plants.containsKey(newPlant.getId())) {
            return null; // duplicate ID
        }
        List<PlantInfo> existing = new ArrayList<>(plants.values());
        plants.put(newPlant.getId(), newPlant);
        return existing;
    }

    public synchronized List<PlantInfo> getAllPlants() {
        return new ArrayList<>(plants.values());
    }

    public synchronized boolean removePlant(int id) {
        return plants.remove(id) != null;
    }
}
