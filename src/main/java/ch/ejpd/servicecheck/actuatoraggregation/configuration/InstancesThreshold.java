package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.boot.actuate.health.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class InstancesThreshold {


    private Map<Integer,Status> thresholdValues;

    private InstancesThreshold(Map<Integer,Status> thresholdValues) {
        this.thresholdValues = thresholdValues;
    }


    public static InstancesThreshold createEmptyThreshold() {
        return createInstancesThreshold(new HashMap<>());
    }

    static InstancesThreshold createInstancesThreshold(Map<Status,Integer> thresholds) {

        SortedMap<Integer,Status> thresholdValues = new TreeMap<>((o1, o2) -> o1.compareTo(o2) * -1);

        for (Map.Entry<Status, Integer> statusIntegerEntry : thresholds.entrySet()) {
            final Status previous = thresholdValues.put(statusIntegerEntry.getValue(), statusIntegerEntry.getKey());
            if (previous != null) {
                throw new IllegalArgumentException("Contradicting thresholds: For " + statusIntegerEntry.getValue() + " instances both " + statusIntegerEntry.getKey() + " and " + previous + " states are configured");
            }
        }
        if (thresholdValues.isEmpty()) {
            thresholdValues.put(1, Status.UP);
        }

        return new InstancesThreshold(thresholdValues);
    }


    public Status evaluateForUpInstances(Integer numberOfInstances) {
        for (Map.Entry<Integer,Status> statusIntegerEntry : thresholdValues.entrySet()) {
            final Integer statusThreshold = statusIntegerEntry.getKey();
            if (numberOfInstances >= statusThreshold) {
                return statusIntegerEntry.getValue();
            }
        }
        return Status.DOWN;
    }
}