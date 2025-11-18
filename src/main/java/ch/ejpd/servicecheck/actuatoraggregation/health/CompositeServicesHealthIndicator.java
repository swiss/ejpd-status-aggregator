package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.InstancesThreshold;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class CompositeServicesHealthIndicator extends CompositeHealthIndicator {


    private final Optional<String> ownZone;
    private final boolean ignoreOtherRegistryZones;
    private List<String> neededServices;
    private DiscoveryClient discoveryClient;
    private HealthInfoClient healthInfoClient;
    private Map<String,InstancesThreshold> thresholds;
    private Logger logger = LoggerFactory.getLogger(CompositeServicesHealthIndicator.class);




    public CompositeServicesHealthIndicator(String ownZone, boolean ignoreOtherRegistryZones, List<String> neededServices, DiscoveryClient servicesToInstances, HealthInfoClient healthInfoClient, Map<String,InstancesThreshold> thresholds, OrderedHealthAggregator healthAggregator) {
        super(healthAggregator);
        this.ownZone = Optional.ofNullable(ownZone);
        this.ignoreOtherRegistryZones = ignoreOtherRegistryZones;
        this.neededServices = neededServices.stream().map(String::toUpperCase).collect(toList());
        this.discoveryClient = servicesToInstances;
        this.healthInfoClient = healthInfoClient;
        this.thresholds = thresholds;
    }

    @Override
    public Health health() {
        final Map<String,List<ServiceInstance>> stringListMap = fetchServiceInstances();
        final Map<String,HealthIndicator> map = createMap(stringListMap, healthInfoClient);
        for (Map.Entry<String,HealthIndicator> stringHealthIndicatorEntry : map.entrySet()) {
            addHealthIndicator(stringHealthIndicatorEntry.getKey(), stringHealthIndicatorEntry.getValue());
        }
        return super.health();
    }

    private Map<String,List<ServiceInstance>> fetchServiceInstances() {
        final List<String> services = discoveryClient.getServices();
        Map<String,List<ServiceInstance>> serviceInstancesMap = new HashMap<>();
        final List<ServiceInstance> collect = services.stream().map(discoveryClient::getInstances).flatMap(Collection::stream)
                .filter(serviceInstance -> !ignoreOtherRegistryZones || isInOwnZone(serviceInstance))
                .collect(toList());

        for (ServiceInstance serviceInstance : collect) {
            final List<ServiceInstance> orDefault = serviceInstancesMap.getOrDefault(serviceInstance.getServiceId(), new ArrayList<>());
            orDefault.add(serviceInstance);
            serviceInstancesMap.put(serviceInstance.getServiceId(), orDefault);
        }

        return serviceInstancesMap;
    }

    private boolean isInOwnZone(ServiceInstance serviceInstance) {
        final String host = serviceInstance.getHost();
        final String ownZone = this.ownZone.orElseThrow(() -> new IllegalArgumentException("Cannot determine my own zone - make sure the property 'eureka.instance.metadataMap.zone' is set!"));
        final Map<String,String> metadata = serviceInstance.getMetadata();
        String instancesZone = metadata.get("zone");
        if (instancesZone == null) {
            throw new IllegalArgumentException("Cannot obtain zone info of service instance: " + serviceInstance.getServiceId() + "[@" + host + "]. Make sure there is an entry in the instance's metadata map with key 'zone', see https://cloud.spring.io/spring-cloud-static/spring-cloud-netflix/1.3.5.RELEASE/single/spring-cloud-netflix.html#_zones");
        }
        if (! instancesZone.trim().equals(ownZone)) {
            logger.debug("Ignoring check for service instance {}[@{}]: it is in zone {}, but i am only interested in zone {}",
                    serviceInstance.getServiceId(), host, instancesZone, ownZone);
            return false;
        }
        return true;
    }

    private Map<String, HealthIndicator> createMap(Map<String,List<ServiceInstance>> servicesToInstances, HealthInfoClient healthInfoClient) {
        Map<String,HealthIndicator> healthIndicators = new HashMap<>();

        final Set<Map.Entry<String,List<ServiceInstance>>> entries = servicesToInstances.entrySet();
        for (Map.Entry<String,List<ServiceInstance>> entry : entries) {
            final String serviceId = entry.getKey();
            final Optional<String> any = neededServices.stream().filter(serviceId::equalsIgnoreCase).findAny();
            if (any.isPresent()) {
                final InstancesThreshold instancesThreshold = getInstancesThresholdFor(serviceId);
                healthIndicators.put(serviceId, new CompositeServiceInstancesHealhIndicator(entry.getValue(), healthInfoClient, instancesThreshold));
            }
        }
        // check if all needed services actually are in serviceToInstancesMap
        for (String neededService : neededServices) {
            final Optional<String> any = servicesToInstances.keySet().stream().filter(serviceIdFromRegistry -> serviceIdFromRegistry.equalsIgnoreCase(neededService)).findAny();
            if ( ! any.isPresent()) {
                healthIndicators.put(neededService, new AbsentServiceHealthIndicator(neededService, getInstancesThresholdFor(neededService)));
            }
        }

        return healthIndicators;
    }

    private InstancesThreshold getInstancesThresholdFor(String serviceId) {
        return thresholds.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(serviceId))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseGet(InstancesThreshold::createEmptyThreshold);
    }

}
