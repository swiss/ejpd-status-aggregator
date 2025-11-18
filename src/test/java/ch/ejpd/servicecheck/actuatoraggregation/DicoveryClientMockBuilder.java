package ch.ejpd.servicecheck.actuatoraggregation;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DicoveryClientMockBuilder {


    private DicoveryClientMockBuilder() {
    }

    public static DiscoveryClient discoveryClientWith(ServiceInstance... instances) {
        Set<String> serviceIds = Arrays.stream(instances).map(ServiceInstance::getServiceId).collect(Collectors.toSet());
        final DiscoveryClient mock = mock(DiscoveryClient.class);
        when(mock.getServices()).thenReturn(new ArrayList<>(serviceIds));
        for (ServiceInstance instance : instances) {
            final List<ServiceInstance> instancesWithSameServiceId = Arrays.stream(instances).filter(i -> i.getServiceId().equals(instance.getServiceId())).collect(Collectors.toList());
            when(mock.getInstances(instance.getServiceId())).thenReturn(instancesWithSameServiceId);
        }
        return mock;
    }


}
