package autopilot.core;

import autopilot.core.AutoDriver.DriveResult;
import autopilot.exception.SensorFailureException;
import autopilot.sensor.LaneSensor;
import autopilot.sensor.ObstacleSensor;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoDriverTest {

    private static final double DELTA = 1e-9;
    private static final GearBox.Gear DRIVE = GearBox.Gear.D;
    private static final String SENSOR_FAILURE_MESSAGE = "sensor failed";

    private static final double TARGET_SPEED_KMH = 80.0;
    private static final double CRUISING_SPEED_KMH = 80.0;
    private static final double CLEAR_ROAD_DISTANCE_METERS = 100.0;
    private static final double CENTERED_LANE_OFFSET_METERS = 0.0;

    private static final double NO_STEERING = 0.0;
    private static final double NO_BRAKE = 0.0;
    private static final double SAFE_MODE_BRAKE_PRESSURE = 30.0;

    private static final SpeedSensor CRUISING_SPEED_SENSOR = () -> CRUISING_SPEED_KMH;
    private static final ObstacleSensor CLEAR_ROAD_SENSOR = () -> CLEAR_ROAD_DISTANCE_METERS;
    private static final LaneSensor CENTERED_LANE_SENSOR = () -> CENTERED_LANE_OFFSET_METERS;

    private static final SpeedSensor FAILING_SPEED_SENSOR = () -> {
        throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
    };
    private static final ObstacleSensor FAILING_OBSTACLE_SENSOR = () -> {
        throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
    };
    private static final LaneSensor FAILING_LANE_SENSOR = () -> {
        throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
    };

    private static AutoDriver driverWith(SpeedSensor speed, ObstacleSensor obstacle, LaneSensor lane) {
        return new AutoDriver(speed, obstacle, lane, TARGET_SPEED_KMH);
    }

    static Stream<Arguments> driversWithOneFailingSensor() {
        return Stream.of(
                Arguments.of("speed",
                        driverWith(FAILING_SPEED_SENSOR, CLEAR_ROAD_SENSOR, CENTERED_LANE_SENSOR)),
                Arguments.of("obstacle",
                        driverWith(CRUISING_SPEED_SENSOR, FAILING_OBSTACLE_SENSOR, CENTERED_LANE_SENSOR)),
                Arguments.of("lane",
                        driverWith(CRUISING_SPEED_SENSOR, CLEAR_ROAD_SENSOR, FAILING_LANE_SENSOR)));
    }

    @Test
    void tick_cruisingOnClearRoadWithCenteredLane_holdsCourseWithoutBraking() {
        AutoDriver driver = driverWith(CRUISING_SPEED_SENSOR, CLEAR_ROAD_SENSOR, CENTERED_LANE_SENSOR);

        DriveResult result = driver.tick(DRIVE);

        assertEquals(
                new DriveResult(NO_STEERING, NO_BRAKE, AdaptiveCruise.Mode.HOLD, false, false),
                result);
    }

    @ParameterizedTest(name = "lane offset {0} m -> steering {1} deg")
    @CsvSource({
            "0.51, -45.0",
            "1.0, -45.0",
            "-0.51, 45.0",
            "-1.0, 45.0",
            "0.5, 0.0",
            "-0.5, 0.0",
            "0.0, 0.0"
    })
    void tick_laneOffset_steersAgainstOffsetOnlyBeyondThreshold(double laneOffset, double expectedSteering) {
        AutoDriver driver = driverWith(CRUISING_SPEED_SENSOR, CLEAR_ROAD_SENSOR, () -> laneOffset);

        DriveResult result = driver.tick(DRIVE);

        assertEquals(expectedSteering, result.steeringAngleDegrees(), DELTA);
    }

    @ParameterizedTest(name = "{0} sensor failure")
    @MethodSource("driversWithOneFailingSensor")
    void tick_anySensorFails_entersSafeMode(String failedSensor, AutoDriver driver) {
        DriveResult result = driver.tick(DRIVE);

        assertEquals(
                new DriveResult(NO_STEERING, SAFE_MODE_BRAKE_PRESSURE, AdaptiveCruise.Mode.BRAKE, false, true),
                result);
    }
}