package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.boot.actuate.health.Status;

import java.util.HashMap;
import java.util.Map;

class ThresholdMapBuilder {

    private Map<Status, Integer> thresholdMap = new HashMap<>();

    static ThresholdMapBuilder thresholdMap() {
        return new ThresholdMapBuilder();
    }

    ThresholdMapBuilder withThreshold(int i, Status up) {
        thresholdMap.put(up,i);
        return this;
    }

    Map<Status, Integer> build() {
        return new HashMap<>(thresholdMap);
    }

}
