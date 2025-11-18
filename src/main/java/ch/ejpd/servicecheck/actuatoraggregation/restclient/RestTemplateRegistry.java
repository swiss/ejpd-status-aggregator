package ch.ejpd.servicecheck.actuatoraggregation.restclient;

import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class RestTemplateRegistry {

    private Map<String,RestTemplate> restTemplates;

    public RestTemplateRegistry(Map<String,RestTemplate> restTemplates) {
        this.restTemplates = restTemplates;
    }

    public RestTemplate getRestTemplateForService(String serviceId) {
        return restTemplates.get(serviceId);
    }
}
