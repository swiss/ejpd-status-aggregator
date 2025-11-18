package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.HealthAggregatorProperties;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


// class CompositeServiceInstancesHealhIndicator extends CompositeHealthIndicator {
class CompositeServiceInstancesHealhIndicator implements CompositeHealthContributor {


    private final Map<String,HealthContributor> healthIndicatorMap;
    private final NeededService neededService;
    private final ServiceStatusAggregator serviceStatusAggregator;

    CompositeServiceInstancesHealhIndicator(NeededService neededService,
                                            List<ServiceInstance> serviceInstances,
                                            HealthInfoClient healthInfoClient,
                                            HealthAggregatorProperties.HttpServiceSettings serviceSettings,
                                            ServiceStatusAggregator serviceStatusAggregator
    ) {
        this.neededService = neededService;
        this.serviceStatusAggregator = serviceStatusAggregator;
        // super(new ServiceInstancesHealthAggregator(neededService, serviceStatusAggregator), createMap(serviceInstances, healthInfoClient, serviceSettings));
        healthIndicatorMap = CompositeServiceInstancesHealhIndicator.createMap(serviceInstances, healthInfoClient, serviceSettings);
    }


    private static Map<String,HealthContributor> createMap(List<ServiceInstance> serviceInstances, HealthInfoClient healthInfoClient, HealthAggregatorProperties.HttpServiceSettings serviceSettings) {
        Map<String,HealthContributor> map = new HashMap<>();
        int index = 0;
        for (ServiceInstance serviceInstance : serviceInstances) {
            map.put("instance-" + index++, new DefaultServiceInstanceHealthIndicator(serviceInstance, healthInfoClient, serviceSettings));
        }
        return map;
    }

    public Map<String,HealthContributor> getMap() {
        return healthIndicatorMap;
    }


    /**
     * @return HealthContributor is just a tagging Interface, but by definition, is must be either a HealthIndicator or a CompositeHealthContributor
     */
    @Override
    public HealthContributor getContributor(String name) {
        return healthIndicatorMap.get(name);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return healthIndicatorMap.entrySet().stream()
                .map(contr -> NamedContributor.of(contr.getKey(), contr.getValue()))
                .iterator();
    }

    public Status getAggregatedStatus(Status... statuses) {
        //        healthIndicatorMap.entrySet().stream()
        //                .map(contr -> ((DefaultServiceInstanceHealthIndicator) contr.getValue()).)
        //                .iterator();
        return new ServiceInstancesHealthAggregator(neededService, serviceStatusAggregator).getAggregateStatus(statuses);
    }

}
