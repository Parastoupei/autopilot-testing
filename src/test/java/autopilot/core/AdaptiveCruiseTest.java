package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class AdaptiveCruiseTest {

    private AdaptiveCruise adaptiveCruise;

    @BeforeEach
    void setUp() {

        adaptiveCruise = new AdaptiveCruise(100.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.NEGATIVE_INFINITY})
    @DisplayName("Constructor throws IllegalArgumentException for invalid target speed")
    void constructor_InvalidTargetSpeed_ThrowsIllegalArgumentException(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> new AdaptiveCruise(invalidSpeed));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 50.0, 120.0, 200.0})
    @DisplayName("safeDistanceFor returns correct safe distance for valid speeds")
    void safeDistanceFor_ValidSpeed_ReturnsCorrectDistance(double speed) {
        double expected = speed * 0.5; // METERS_PER_KMH = 0.5
        assertEquals(expected, adaptiveCruise.safeDistanceFor(speed), 0.001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-5.0, Double.NaN})
    @DisplayName("safeDistanceFor throws IllegalArgumentException for invalid speeds")
    void safeDistanceFor_InvalidSpeed_ThrowsIllegalArgumentException(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> adaptiveCruise.safeDistanceFor(invalidSpeed));
    }

    @Test
    @DisplayName("Update returns EMERGENCY_BRAKE when distance is extremely close")
    void update_EmergencyBrakeCondition_ReturnsEmergencyBrakeDecision() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(60.0, 10.0);
        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, decision.mode());
        assertEquals(100.0, decision.amount(), 0.001);
    }

    @Test
    @DisplayName("Update returns ACCELERATE when distance is safe and speed is below target")
    void update_SafeDistanceAndLowSpeed_ReturnsAccelerateDecision() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(50.0, 40.0);
        assertEquals(AdaptiveCruise.Mode.ACCELERATE, decision.mode());
        assertEquals(5.0, decision.amount(), 0.001);
    }

    @Test
    @DisplayName("Update returns HOLD when distance is safe and speed equals target")
    void update_SafeDistanceAndTargetSpeed_ReturnsHoldDecision() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(100.0, 60.0);
        assertEquals(AdaptiveCruise.Mode.HOLD, decision.mode());
        assertEquals(0.0, decision.amount(), 0.001);
    }

    @Test
    @DisplayName("Update returns BRAKE when distance is less than safe distance")
    void update_BelowSafeDistance_ReturnsBrakeDecision() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(60.0, 20.0);
        assertEquals(AdaptiveCruise.Mode.BRAKE, decision.mode());
        assertTrue(decision.amount() > 0.0);
    }

    @ParameterizedTest
    @CsvSource({
        "-1.0, 50.0",
        "60.0, -10.0",
        "NaN, 50.0",
        "60.0, NaN"
    })
    @DisplayName("Update throws IllegalArgumentException for invalid speed or distance arguments")
    void update_InvalidArguments_ThrowsIllegalArgumentException(double speed, double distance) {
        assertThrows(IllegalArgumentException.class, () -> adaptiveCruise.update(speed, distance));
    }
}