package ch.ejpd.servicecheck.actuatoraggregation.restclient;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.springframework.util.StringUtils.isEmpty;

public class ServiceInstanceURIResolver {
    private static final String KEY_HEALTH_PATH = "health.path";
    private static final String DEFAULT_HEALTH_PATH = "health";
    private static final String KEY_MANAGEMENT_PORT = "management.port";
    private static final String KEY_MANAGEMENT_PATH = "management.context-path";
    private static final String KEY_MANAGEMENT_ADDRESS = "management.address";
    private static final String DEFAULT_MANAGEMENT_PATH = "/actuator";

    public static URI getHealthUrl(ServiceInstance instance) {
        String healthPath = instance.getMetadata().get(KEY_HEALTH_PATH);
        if (isEmpty(healthPath)) {
            healthPath = DEFAULT_HEALTH_PATH;
        }

        return UriComponentsBuilder.fromUri(getManagementUrl(instance)).path("/").path(healthPath).build().toUri();
    }

    protected static URI getManagementUrl(ServiceInstance instance) {
        String managementPath = instance.getMetadata().get(KEY_MANAGEMENT_PATH);
        if (isEmpty(managementPath)) {
            managementPath = DEFAULT_MANAGEMENT_PATH;
        }

        URI serviceUrl = UriComponentsBuilder.fromUri(instance.getUri()).path("/").build().toUri();

        String managementServerAddress = instance.getMetadata().get(KEY_MANAGEMENT_ADDRESS);
        if (isEmpty(managementServerAddress)) {
            managementServerAddress = serviceUrl.getHost();
        }

        String managementPort = instance.getMetadata().get(KEY_MANAGEMENT_PORT);
        if (isEmpty(managementPort)) {
            managementPort = String.valueOf(serviceUrl.getPort());
        }

        return UriComponentsBuilder.fromUri(serviceUrl)
                .host(managementServerAddress)
                .port(managementPort)
                .path("/")
                .path(managementPath)
                .build()
                .toUri();
    }
}
