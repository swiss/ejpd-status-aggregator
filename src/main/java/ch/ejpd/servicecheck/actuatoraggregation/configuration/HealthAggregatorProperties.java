package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "healthaggregator")
public class HealthAggregatorProperties {

    private String registryzone = "${eureka.instance.metadataMap.zone}";

    private List<String> neededServices = new ArrayList<>();

    public List<String> getNeededServices() {
        return neededServices;
    }

    private Map<String,Map<Status,Integer>> thresholds = new HashMap<>();

    public void setNeededServices(List<String> neededServices) {
        this.neededServices = neededServices;
    }

    public Map<String,Map<Status,Integer>> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String,Map<Status,Integer>> thresholds) {
        this.thresholds = thresholds;
    }

    public boolean isIgnoreOtherRegistryZones() {
        // Bewusst kein Property mit Getter- und Setter.
        // Kann einfach umgebaut werden wenn dieses Verhalten mal konfigurierbar sein soll!
        return true;
    }

    public String getRegistryzone() {
        return registryzone;
    }

    public void setRegistryzone(String registryzone) {
        this.registryzone = registryzone;
    }

    Map<String,InstancesThreshold> thresholdMap() {
        final Map<String,InstancesThreshold> thresholds = new HashMap<>();
        for (Map.Entry<String,Map<Status,Integer>> stringMapEntry : getThresholds().entrySet()) {
            Map<Status,Integer> thresholdMap = stringMapEntry.getValue();
            thresholds.put(stringMapEntry.getKey(), InstancesThreshold.createInstancesThreshold(thresholdMap));
        }
        return thresholds;
    }
}
