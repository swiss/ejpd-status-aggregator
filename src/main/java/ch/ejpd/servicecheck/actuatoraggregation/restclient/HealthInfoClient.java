package ch.ejpd.servicecheck.actuatoraggregation.restclient;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("unchecked")
public class HealthInfoClient {
    public Map<String, Object> getHealthInfoMap(String healthCheckUrl) {
        final Map<String,Serializable> healthInfo;
        final RestTemplate restTemplate =
                new RestTemplateBuilder().setConnectTimeout(500).setReadTimeout(500).errorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return false;
                    }

                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {

                    }
                }).build();

        return restTemplate.getForObject(healthCheckUrl, Map.class);
    }
}
