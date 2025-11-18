package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.HealthAggregatorProperties;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.*;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * This is where the magic happens
 */
public class CompositeServicesHealthIndicator implements HealthIndicator {


    private final String ownZone;
    private final boolean ignoreOtherRegistryZones;
    private final HealthAggregatorProperties.HttpSettings httpSettings;
    private List<NeededService> neededServices;
    private DiscoveryClient discoveryClient;
    private HealthInfoClient healthInfoClient;
    private ApplicationStatusHealthAggregator applicationStatusHealthAggregator;
    private ServiceStatusAggregator serviceStatusAggregator;
    private Logger logger = LoggerFactory.getLogger(CompositeServicesHealthIndicator.class);


    public CompositeServicesHealthIndicator(String ownZone,
                                            boolean ignoreOtherRegistryZones,
                                            Map<String,Integer> neededServicesMap,
                                            DiscoveryClient servicesToInstances,
                                            HealthInfoClient healthInfoClient,
                                            HealthAggregatorProperties.HttpSettings httpSettings,
                                            ApplicationStatusHealthAggregator applicationStatusHealthAggregator,
                                            ServiceStatusAggregator serviceStatusAggregator) {
        this.ownZone = ownZone;
        this.ignoreOtherRegistryZones = ignoreOtherRegistryZones;
        this.neededServices = NeededService.createFrom(neededServicesMap);
        this.discoveryClient = servicesToInstances;
        this.healthInfoClient = healthInfoClient;
        this.httpSettings = httpSettings;
        this.applicationStatusHealthAggregator = applicationStatusHealthAggregator;
        this.serviceStatusAggregator = serviceStatusAggregator;
    }


    @Override
    public Health health() {
        final Map<String,List<ServiceInstance>> stringListMap = fetchServiceInstancesForOwnZoneByDiscoveryClient();
        final Map<String,HealthContributor> serviceNameToHealthIndicatorMap = createServiceNameToHealthIndicatorMap(stringListMap, healthInfoClient);

        // register healthindicators as contributors in temporary DefaultHealthContributorRegistry
        // e.g. CompositeServiceInstancesHealhIndicator and AbsentServiceHealthIndicator that handles needed Services
        final var defaultHealthIndicatorRegistry = new DefaultHealthContributorRegistry();
        for (Map.Entry<String,HealthContributor> stringHealthIndicatorEntry : serviceNameToHealthIndicatorMap.entrySet()) {
            defaultHealthIndicatorRegistry.registerContributor(stringHealthIndicatorEntry.getKey(), stringHealthIndicatorEntry.getValue());
        }


        final var parentHealth = new Health.Builder();
        Set<Status> compositeHealthStatuses = new HashSet<>();

        defaultHealthIndicatorRegistry.stream().forEach(entry -> {
            final var contributor = entry.getContributor();
            if (contributor instanceof HealthIndicator) {
                final var health = ((HealthIndicator) contributor).health();  // <== /health aufrufen
                compositeHealthStatuses.add(health.getStatus());
                parentHealth.withDetail(entry.getName(), health);
            }
            if (contributor instanceof CompositeServiceInstancesHealhIndicator) {
                CompositeServiceInstancesHealhIndicator composite = (CompositeServiceInstancesHealhIndicator) contributor;
                final var compositeHealthBuilder = new Health.Builder();
                List<Status> childStatusList = new ArrayList<>();

                composite.stream().forEach(child -> {
                    final var childContributor = child.getContributor();
                    if (childContributor instanceof HealthIndicator) {
                        final var childHealth = ((HealthIndicator) childContributor).health(); // <== /health aufrufen
                        childStatusList.add(childHealth.getStatus());
                        compositeHealthBuilder.withDetail(child.getName(), childHealth);
                    }
                });
                final var aggregatedStatus = composite.getAggregatedStatus(childStatusList.toArray(Status[]::new));
                compositeHealthStatuses.add(aggregatedStatus);
                compositeHealthBuilder.status(aggregatedStatus);
                parentHealth.withDetail(entry.getName(), compositeHealthBuilder.build());
            }

        });
        parentHealth.status(applicationStatusHealthAggregator.aggregateApplicationStatus(compositeHealthStatuses));
        return parentHealth.build();
    }

    private Map<String,List<ServiceInstance>> fetchServiceInstancesForOwnZoneByDiscoveryClient() {

        final List<String> neededServiceNames = neededServices.stream().map(NeededService::getServiceName).toList();
        List<String> services =  discoveryClient.getServices().stream().filter(neededServiceNames::contains).toList();

        Map<String,List<ServiceInstance>> serviceIdToInstancesMap = new HashMap<>();

        final List<ServiceInstance> serviceInstances = fetchServiceInstancesForOwnZoneByDiscoveryClient(services);

        for (ServiceInstance serviceInstance : serviceInstances) {
            final List<ServiceInstance> orDefault = serviceIdToInstancesMap.getOrDefault(serviceInstance.getServiceId(), new ArrayList<>());
            orDefault.add(serviceInstance);
            serviceIdToInstancesMap.put(serviceInstance.getServiceId(), orDefault);
        }

        return serviceIdToInstancesMap;
    }

    private List<ServiceInstance> fetchServiceInstancesForOwnZoneByDiscoveryClient(List<String> services) {
        return services.stream()
                .map(discoveryClient::getInstances)
                .flatMap(Collection::stream)
                .filter(serviceInstance -> !ignoreOtherRegistryZones || isInOwnZone(serviceInstance))
                .collect(toList());
    }

    private boolean isInOwnZone(ServiceInstance serviceInstance) {
        final String host = serviceInstance.getHost();
        final String verifiedOwnZone = Optional.ofNullable(this.ownZone).orElseThrow(() -> new IllegalArgumentException("Cannot determine my own zone - make sure the property 'eureka.instance.metadataMap.zone' is set!"));
        final Map<String,String> metadata = serviceInstance.getMetadata();
        String instancesZone = metadata.get("zone");
        if (instancesZone == null) {
            throw new IllegalArgumentException("Cannot obtain zone info of service instance: " + serviceInstance.getServiceId() + "[@" + host + "]. Make sure there is an entry in the instance's metadata map with key 'zone', see https://cloud.spring.io/spring-cloud-static/spring-cloud-netflix/2.0.0.RELEASE/single/spring-cloud-netflix.html#_zones");
        }
        if (!instancesZone.trim().equals(verifiedOwnZone)) {
            logger.debug("Ignoring check for service instance {}[@{}]: it is in zone {}, but i am only interested in zone {}",
                    serviceInstance.getServiceId(), host, instancesZone, verifiedOwnZone);
            return false;
        }
        return true;
    }

    private Map<String,HealthContributor> createServiceNameToHealthIndicatorMap(Map<String,List<ServiceInstance>> servicesToInstances, HealthInfoClient healthInfoClient) {
        Map<String,HealthContributor> healthIndicators = new HashMap<>();

        final Set<Map.Entry<String,List<ServiceInstance>>> entries = servicesToInstances.entrySet();
        for (Map.Entry<String,List<ServiceInstance>> entry : entries) {
            final String serviceId = entry.getKey();
            final Optional<NeededService> any = neededServices.stream().filter(it -> it.getServiceName().equalsIgnoreCase(serviceId)).findAny();
            if (any.isPresent()) {
                HealthAggregatorProperties.HttpServiceSettings serviceSettings = httpSettings.getTimeoutsForService(serviceId);
                healthIndicators.put(any.get().getServiceName(), new CompositeServiceInstancesHealhIndicator(any.get(), entry.getValue(), healthInfoClient, serviceSettings, this.serviceStatusAggregator));
            }
        }
        // check if all needed services actually are in serviceToInstancesMap
        for (NeededService neededService : neededServices) {
            final Optional<String> any = servicesToInstances.keySet().stream()
                    .filter(serviceIdFromRegistry -> serviceIdFromRegistry.equalsIgnoreCase(neededService.getServiceName()))
                    .findAny();
            if (any.isEmpty()) {
                healthIndicators.put(neededService.getServiceName(), new AbsentServiceHealthIndicator(neededService, this.serviceStatusAggregator));
            }
        }

        return healthIndicators;
    }


}
