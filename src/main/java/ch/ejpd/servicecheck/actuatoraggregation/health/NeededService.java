package ch.ejpd.servicecheck.actuatoraggregation.health;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NeededService {

    @NotNull
    private String serviceName;

    @NotNull
    private Integer neededInstances = 1;

    public static NeededService create(String serviceName) {
        NeededService neededService = new NeededService();
        neededService.setServiceName(serviceName);
        return neededService;
    }

    public static NeededService create(String serviceName, int neededInstances) {
        NeededService neededService = new NeededService();
        neededService.setServiceName(serviceName);
        neededService.setNeededInstances(neededInstances);
        return neededService;
    }

    public static List<NeededService> createFrom(Map<String,Integer> neededServicesMap) {
        return neededServicesMap.entrySet().stream()
                .map(entry -> create(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public String getServiceName() {
        return serviceName;
    }

    public Integer getNeededInstances() {
        return neededInstances;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setNeededInstances(Integer neededInstances) {
        this.neededInstances = neededInstances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NeededService that = (NeededService) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(neededInstances, that.neededInstances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, neededInstances);
    }
}
