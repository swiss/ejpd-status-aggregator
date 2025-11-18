package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.Status;

public interface StatusList {

    Status UP = Status.UP;
    Status DOWN = Status.DOWN;
    Status WARNING = new Status("WARNING");
    Status OUT_OF_SERVICE = Status.OUT_OF_SERVICE;
    Status UNKNOWN = Status.UNKNOWN;

}
