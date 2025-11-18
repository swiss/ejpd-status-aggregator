package ch.ejpd.servicecheck.actuatoraggregation.restclient;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class RestTemplateRegistry {

    private final Map<String,RestTemplate> restTemplates = new HashMap<>();

    public void setRestTemplateForService(String serviceId, RestTemplate restTemplate) {
        this.restTemplates.put(serviceId.toLowerCase(), restTemplate);
    }


    public RestTemplate getRestTemplateForService(String serviceId) {
        return restTemplates.get(serviceId.toLowerCase());
    }
}
