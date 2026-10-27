package autopilot.core;

import autopilot.core.AdaptiveCruise.Decision;
import autopilot.core.AdaptiveCruise.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveCruiseTest {

    private static final double TARGET_SPEED_KMH = 100.0;
    private static final double DELTA = 1e-9;

    private static final double SPEED_KMH = 60.0;
    private static final double SAFE_DISTANCE = 30.0;
    private static final double RELEASE_DISTANCE = 36.0;
    private static final double EMERGENCY_DISTANCE = 10.0;
    private static final double BRAKING_DISTANCE = 24.0;            
    private static final double DISTANCE_IN_RELEASE_BAND = 33.0;   
    private static final double HYSTERESIS_BRAKE_PRESSURE = 10.0;
    private static final double MAX_ACCELERATION_STEP_KMH = 5.0;

    private AdaptiveCruise cruise;

    @BeforeEach
    void setUp() {
        cruise = new AdaptiveCruise(TARGET_SPEED_KMH);
    }

    @Test
    void safeDistanceFor_validSpeed_returnsHalfOfSpeed() {
        assertEquals(SAFE_DISTANCE, cruise.safeDistanceFor(SPEED_KMH), DELTA);
    }

    @ParameterizedTest(name = "invalid speed = {0}")
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void safeDistanceFor_invalidSpeed_throwsIllegalArgument(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> cruise.safeDistanceFor(invalidSpeed));
    }

    @ParameterizedTest(name = "invalid speed = {0}")
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY})
    void update_invalidSpeed_throwsIllegalArgument(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> cruise.update(invalidSpeed, SAFE_DISTANCE));
    }

    @ParameterizedTest(name = "invalid distance = {0}")
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY})
    void update_invalidDistance_throwsIllegalArgument(double invalidDistance) {
        assertThrows(IllegalArgumentException.class, () -> cruise.update(SPEED_KMH, invalidDistance));
    }

    @ParameterizedTest(name = "speed={0}, distance={1} -> {2}, amount={3}")
    @CsvSource({
            "60.0, 14.9, EMERGENCY_BRAKE, 100.0",
            "60.0, 15.0, EMERGENCY_BRAKE, 100.0",
            "60.0, 24.0, BRAKE, 20.0",
            "60.0, 30.0, HOLD, 0.0",
            "60.0, 30.1, ACCELERATE, 5.0",
            "98.0, 100.0, ACCELERATE, 2.0",
            "100.0, 100.0, HOLD, 0.0",
            "120.0, 100.0, HOLD, 0.0",
            "0.0, 0.0, EMERGENCY_BRAKE, 100.0",
            "0.0, 5.0, ACCELERATE, 5.0"
    })
    void update_firstCall_returnsExpectedDecision(double speed, double distance,
                                                  Mode expectedMode, double expectedAmount) {
        Decision decision = cruise.update(speed, distance);

        assertAll(
                () -> assertEquals(expectedMode, decision.mode()),
                () -> assertEquals(expectedAmount, decision.amount(), DELTA));
    }

    @Test
    void update_afterBrakingAndDistanceInReleaseBand_keepsLightBrake() {
        cruise.update(SPEED_KMH, BRAKING_DISTANCE);

        Decision decision = cruise.update(SPEED_KMH, DISTANCE_IN_RELEASE_BAND);

        assertEquals(new Decision(Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE), decision);
    }

    @Test
    void update_afterBrakingAndDistanceEqualsSafeDistance_keepsLightBrake() {
        cruise.update(SPEED_KMH, BRAKING_DISTANCE);

        Decision decision = cruise.update(SPEED_KMH, SAFE_DISTANCE);

        assertEquals(new Decision(Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE), decision);
    }

    @Test
    void update_afterBrakingAndDistanceReachesReleaseDistance_stopsBrakingAndAccelerates() {
        cruise.update(SPEED_KMH, BRAKING_DISTANCE);

        Decision decision = cruise.update(SPEED_KMH, RELEASE_DISTANCE);

        assertEquals(new Decision(Mode.ACCELERATE, MAX_ACCELERATION_STEP_KMH), decision);
    }

    @Test
    void update_withoutPriorBrakingAndDistanceInReleaseBand_accelerates() {
        Decision decision = cruise.update(SPEED_KMH, DISTANCE_IN_RELEASE_BAND);

        assertEquals(new Decision(Mode.ACCELERATE, MAX_ACCELERATION_STEP_KMH), decision);
    }

    @Test
    void update_afterEmergencyBrakeAndDistanceInReleaseBand_keepsLightBrake() {
        cruise.update(SPEED_KMH, EMERGENCY_DISTANCE);

        Decision decision = cruise.update(SPEED_KMH, DISTANCE_IN_RELEASE_BAND);

        assertEquals(new Decision(Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE), decision);
    }
}
