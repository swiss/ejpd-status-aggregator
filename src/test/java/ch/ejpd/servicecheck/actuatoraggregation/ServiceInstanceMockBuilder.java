package ch.ejpd.servicecheck.actuatoraggregation;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.HashMap;
import java.util.Map;

public class ServiceInstanceMockBuilder {

    private ServiceInstanceMockBuilder() {
    }

    public static ServiceInstance instance(String serviceId, String instanceId) {
        return  new DefaultServiceInstance(
                instanceId,
                serviceId,
                serviceId + instanceId,
                8080,
                false,
                createInstanceMetadataMap("defaultZone"));
    }

    public static ServiceInstance instance(String serviceId, String instanceId, String zone) {
        return new DefaultServiceInstance(
                instanceId,
                serviceId,
                serviceId + instanceId,
                8080,
                false,
                createInstanceMetadataMap(zone));
    }


    private static Map<String, String> createInstanceMetadataMap(String zone) {
        Map<String, String> metaDataMap = new HashMap<>();
        metaDataMap.put("zone", zone);
        metaDataMap.put("management.port", "8081");
        return metaDataMap;
    }

}
