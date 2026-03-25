package server;

import model.PollutionData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe storage for CO2 pollution data received from power plants.
 * Only synchronized methods are used (no java.util.concurrent).
 */
@Service
public class PollutionDataStore {

    private final List<PollutionData> records = new ArrayList<>();

    public synchronized void addRecord(PollutionData data) {
        records.add(data);
    }

    /**
     * Return the overall average of all CO2 values whose report timestamp
     * falls in [t1, t2] (inclusive). Returns null if no data is found.
     */
    public synchronized Double computeAverage(long t1, long t2) {
        double sum = 0;
        int count = 0;
        for (PollutionData record : records) {
            if (record.getTimestamp() >= t1 && record.getTimestamp() <= t2) {
                for (double avg : record.getAverages()) {
                    sum += avg;
                    count++;
                }
            }
        }
        return count == 0 ? null : sum / count;
    }
}
