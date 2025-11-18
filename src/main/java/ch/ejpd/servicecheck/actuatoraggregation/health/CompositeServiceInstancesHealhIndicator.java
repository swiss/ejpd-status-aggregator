package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.InstancesThreshold;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CompositeServiceInstancesHealhIndicator extends CompositeHealthIndicator {


    CompositeServiceInstancesHealhIndicator(List<ServiceInstance> serviceInstances, HealthInfoClient healthInfoClient, InstancesThreshold instancesThreshold) {
        super(new ServiceInstancesHealthAggregator(instancesThreshold), createMap(serviceInstances, healthInfoClient));
    }

    private static Map<String ,HealthIndicator> createMap(List<ServiceInstance> serviceInstances, HealthInfoClient healthInfoClient) {
        Map<String,HealthIndicator> map = new HashMap<>();
        int index = 0;
        for (ServiceInstance serviceInstance : serviceInstances) {
            map.put("instance-"+ index++, new EurekaServiceInstanceHealthIndicator(serviceInstance, healthInfoClient));
        }
        return map;
    }


}
