package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SteeringControllerTest {

    private static final double DELTA = 1e-9;
    private static final double CENTER_ANGLE = 0.0;
    private static final double LOW_SPEED_KMH = 0.0;
    private static final double HIGH_SPEED_KMH = 130.0;     // above the 120 km/h threshold
    private static final double MAX_CHANGE_PER_TICK = 60.0;
    private static final double PHYSICAL_LIMIT = 450.0;
    private static final double BEYOND_ANY_LIMIT = 500.0;
    private static final double ANGLE_BEYOND_HIGH_SPEED_LIMIT = 180.0;

    private SteeringController steering;

    @BeforeEach
    void setUp() {
        steering = new SteeringController();
    }

    @ParameterizedTest(name = "request {0} at {1} km/h after {2} ticks -> {3}")
    @CsvFileSource(resources = "/steering_update_cases.csv", numLinesToSkip = 1)
    void update_repeatedRequests_respectsLimitsAndRateOfChange(double requestedAngle, double speedKmh,
                                                               int ticks, double expectedAngle) {
        double angle = CENTER_ANGLE;
        for (int tick = 0; tick < ticks; tick++) {
            angle = steering.update(requestedAngle, speedKmh);
        }

        assertEquals(expectedAngle, angle, DELTA);
    }

    @Test
    void update_requestBeyondLimit_clampsTargetImmediatelyButMovesGradually() {
        double angle = steering.update(BEYOND_ANY_LIMIT, LOW_SPEED_KMH);

        assertAll(
                () -> assertEquals(MAX_CHANGE_PER_TICK, angle, DELTA),
                () -> assertEquals(PHYSICAL_LIMIT, steering.getTargetAngleDegrees(), DELTA));
    }

    @Test
    void update_speedRisesAboveThresholdWhileBeyondHighSpeedLimit_reducesAngleGradually() {
        for (int tick = 0; tick < 3; tick++) {
            steering.update(ANGLE_BEYOND_HIGH_SPEED_LIMIT, LOW_SPEED_KMH);   // reaches 180
        }

        double angle = steering.update(BEYOND_ANY_LIMIT, HIGH_SPEED_KMH);    // target 90, one step down

        assertEquals(ANGLE_BEYOND_HIGH_SPEED_LIMIT - MAX_CHANGE_PER_TICK, angle, DELTA);
    }

    @ParameterizedTest(name = "request {0} -> after selfCenter {1}")
    @CsvSource({
            "30.0, 0.0",
            "-30.0, 0.0",
            "100.0, 40.0",
            "-100.0, -40.0",
            "120.0, 60.0",
            "0.0, 0.0"
    })
    void selfCenter_afterReachingRequestedAngle_movesTowardCenterByAtMostOneStep(double requestedAngle,
                                                                                double expectedAngle) {
        steering.update(requestedAngle, LOW_SPEED_KMH);
        steering.update(requestedAngle, LOW_SPEED_KMH);

        assertEquals(expectedAngle, steering.selfCenter(), DELTA);
    }

    @ParameterizedTest(name = "invalid angle = {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void update_nonFiniteAngle_throwsIllegalArgument(double invalidAngle) {
        assertThrows(IllegalArgumentException.class, () -> steering.update(invalidAngle, LOW_SPEED_KMH));
    }

    @ParameterizedTest(name = "invalid speed = {0}")
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY})
    void update_invalidSpeed_throwsIllegalArgument(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> steering.update(CENTER_ANGLE, invalidSpeed));
    }
}