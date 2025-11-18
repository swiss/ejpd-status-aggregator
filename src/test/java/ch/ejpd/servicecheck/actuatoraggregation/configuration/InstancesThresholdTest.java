package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.junit.Test;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.actuate.health.Status.*;

@SuppressWarnings("LawOfDemeter")
public class InstancesThresholdTest {


    @Test
    public void up2Instances() {
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(2, UP)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(UP, instancesThreshold.evaluateForUpInstances(2));
    }

    @Test
    public void up2Instances_butGot1() {
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(2, UP)
                .build();


        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(Status.DOWN, instancesThreshold.evaluateForUpInstances(1));
    }


    @Test
    public void up2_OutOfService1Instances_Got1() {
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(2, UP)
                .withThreshold(1, OUT_OF_SERVICE)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(Status.OUT_OF_SERVICE, instancesThreshold.evaluateForUpInstances(1));
    }

    @Test
    public void up2_OutOfService1Instances_Got0() {
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(1, OUT_OF_SERVICE)
                .withThreshold(2, UP)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(Status.DOWN, instancesThreshold.evaluateForUpInstances(0));
    }

    @Test
    public void up2_OutOfService1Instances_Got2() {
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(1, OUT_OF_SERVICE)
                .withThreshold(2, UP)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(UP, instancesThreshold.evaluateForUpInstances(2));
    }

    @Test
    public void niceToHaveService_neverDown() {
        final Status warningState = new Status("WARNING");
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(0, warningState)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(warningState, instancesThreshold.evaluateForUpInstances(0));
    }

    @Test
    public void niceToHaveService_neverDown_isUpOnAtLeast1() {
        final Status warningState = new Status("WARNING");
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(0, warningState)
                .withThreshold(1, UP)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

        assertEquals(UP, instancesThreshold.evaluateForUpInstances(1));
        assertEquals(UP, instancesThreshold.evaluateForUpInstances(42));
    }


    @Test
    public void atLeast1Up_meansUp_ifNoThresholdsAreSet() {

        final InstancesThreshold instancesThreshold = InstancesThreshold.createEmptyThreshold();

        assertEquals(UP, instancesThreshold.evaluateForUpInstances(1));
        assertEquals(UP, instancesThreshold.evaluateForUpInstances(42));
        assertEquals(DOWN, instancesThreshold.evaluateForUpInstances(0));
    }



    @Test(expected = IllegalArgumentException.class)
    public void exception_onContradiction() {
        final Map<Status,Integer> thresholdValues = ThresholdMapBuilder.thresholdMap()
                .withThreshold(1, DOWN)
                .withThreshold(1, UP)
                .build();

        final InstancesThreshold instancesThreshold = InstancesThreshold.createInstancesThreshold(thresholdValues);

    }




}