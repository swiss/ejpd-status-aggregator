package ch.ejpd.servicecheck.actuatoraggregation.sampleapp;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.EnableServicesHealthAggregation;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootApplication
@EnableServicesHealthAggregation
public class HealthAggregatorSampleApp {

    public static void main(String[] args) {
        new SpringApplicationBuilder(HealthAggregatorSampleApp.class).listeners(new ApplicationPidFileWriter()).run(args);
    }


    /**
     * Diese Methode definiert einen gemockten DiscoveryClient, welcher die Healthchecks für die konfigurierten Services FOO und BAR
     * auf statische JSON-Files umlenkt. Damit kann der HealthAggregator ohne Spring Cloud Applikation getestet werden.
     * <p>
     * NUR FÜR TESTZWECKE.
     */
    @Bean
    public DiscoveryClient mockedDiscoveryClient() {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "Mocked discovery client";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {

                final DefaultServiceInstance mock = mock(DefaultServiceInstance.class, RETURNS_DEEP_STUBS);

                when(mock.getServiceId()).thenReturn(serviceId);
                when(mock.getUri()).thenReturn(URI.create("http://localhost:8080/"));
                when(mock.getMetadata()).then(invocation -> {
                    final HashMap<String, String> stringStringHashMap = new HashMap<>();
                    stringStringHashMap.put("zone", "default");
                    stringStringHashMap.put("management.port", "49152");
                    stringStringHashMap.put("health.path", serviceId + ".mocked-healthcheck.json");
                    return stringStringHashMap;
                });

                return Collections.singletonList(mock);
            }

            @Override
            public List<String> getServices() {
                return asList("FOO", "BAR");
            }
        };
    }

}
