package ch.ejpd.servicecheck.actuatoraggregation;

import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiscoveryClientMockBuilder {


    private Map<String,List<ServiceInstance>> services = new HashMap<>();

    public ServiceMockBuilder service(String service1) {
        return new ServiceMockBuilder(service1, this);

    }

    public DiscoveryClient build() {
        final DiscoveryClient mock = mock(DiscoveryClient.class);
        when(mock.getServices()).thenReturn(new ArrayList<>(services.keySet()));
        for (Map.Entry<String,List<ServiceInstance>> stringListEntry : services.entrySet()) {
            final List<ServiceInstance> value = stringListEntry.getValue();
            when(mock.getInstances(stringListEntry.getKey())).thenReturn(value);
        }
        return mock;
    }


    public static class ServiceMockBuilder {
        private String serviceId;
        private DiscoveryClientMockBuilder discoveryClientMockBuilder;
        private List<String> instanceIds = new ArrayList<>();
        private Map<String,String> instancesToZonesMap = new HashMap<>();


        ServiceMockBuilder(String serviceId, DiscoveryClientMockBuilder discoveryClientMockBuilder) {
            this.serviceId = serviceId;
            this.discoveryClientMockBuilder = discoveryClientMockBuilder;
        }

        public ServiceMockBuilder withInstance(String instanceId) {
            this.instanceIds.add(instanceId);
            this.instancesToZonesMap.put(instanceId, "defaultZone");
            return this;
        }

        public DiscoveryClientMockBuilder add() {
            for (String instanceId : instanceIds) {
                final List<ServiceInstance> orDefault = discoveryClientMockBuilder.services.getOrDefault(serviceId, new ArrayList<>());
                orDefault.add(new EurekaDiscoveryClient.EurekaServiceInstance(InstanceInfo.Builder.newBuilder()
                        .enablePort(InstanceInfo.PortType.UNSECURE, true)
                        .setPort(8080)
                        .setAppName(serviceId)
                        .setHostName("theHost")
                        .setHealthCheckUrls("/health", MessageFormat.format("http://{0}{1}/health", serviceId, instanceId), null)
                        .setMetadata(createInstanceMetadataMap(instanceId))
                        .build()));

                discoveryClientMockBuilder.services.put(serviceId, orDefault);
            }

            return discoveryClientMockBuilder;
        }

        private Map<String,String> createInstanceMetadataMap(String instanceId) {
            Map<String,String> metaDataMap = new HashMap<>();
            metaDataMap.put("zone", this.instancesToZonesMap.get(instanceId));
            return metaDataMap;
        }

        public ServiceMockBuilder inZone(String zone1) {
            final String lastInstanceId = this.instanceIds.get(this.instanceIds.size() - 1);
            this.instancesToZonesMap.put(lastInstanceId, zone1);
            return this;
        }
    }

}
