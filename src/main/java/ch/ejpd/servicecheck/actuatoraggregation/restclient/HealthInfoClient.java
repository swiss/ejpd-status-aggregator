package ch.ejpd.servicecheck.actuatoraggregation.restclient;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@SuppressWarnings("unchecked")
public class HealthInfoClient {

    private final RestTemplateRegistry restTemplateRegistry;

    public HealthInfoClient(RestTemplateRegistry restTemplateRegistry) {
        this.restTemplateRegistry = restTemplateRegistry;
    }


    public Map<String,Object> getHealthInfoMap(ServiceInstance serviceInstance) {
        final RestTemplate restTemplate = restTemplateRegistry.getRestTemplateForService(serviceInstance.getServiceId());
        return restTemplate.getForObject(ServiceInstanceURIResolver.getHealthUrl(serviceInstance), Map.class);
    }

}
