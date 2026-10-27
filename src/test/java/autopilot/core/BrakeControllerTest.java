package autopilot.core;

import autopilot.core.GearBox.Gear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrakeControllerTest {

    private static final double DELTA = 1e-9;
    private static final Gear DRIVE = Gear.D;

    private static final double HIGH_SPEED_KMH = 100.0;
    private static final double LOW_SPEED_KMH = 20.0;
    private static final double AT_ABS_SPEED_KMH = 30.0;          // ABS needs speed > 30
    private static final double ABOVE_ABS_SPEED_KMH = 30.1;

    private static final double MODERATE_PRESSURE = 50.0;
    private static final double AT_ABS_PRESSURE = 80.0;           // ABS needs pressure > 80
    private static final double HARD_PRESSURE = 95.0;             // chosen so it differs from both ABS pulses
    private static final double OVER_MAX_PRESSURE = 150.0;
    private static final double MAX_PRESSURE = 100.0;
    private static final double ABS_HIGH_PULSE = 90.0;
    private static final double ABS_LOW_PULSE = 60.0;

    private BrakeController brake;

    @BeforeEach
    void setUp() {
        brake = new BrakeController();
    }

    @Test
    void apply_moderatePressure_returnsRequestedPressure() {
        assertEquals(MODERATE_PRESSURE, brake.apply(MODERATE_PRESSURE, HIGH_SPEED_KMH, DRIVE), DELTA);
    }

    @Test
    void apply_pressureAboveMaximumAtLowSpeed_capsAtMaximum() {
        assertEquals(MAX_PRESSURE, brake.apply(OVER_MAX_PRESSURE, LOW_SPEED_KMH, DRIVE), DELTA);
    }

    @Test
    void apply_pressureExactlyAtAbsThreshold_doesNotUseAbs() {
        assertEquals(AT_ABS_PRESSURE, brake.apply(AT_ABS_PRESSURE, HIGH_SPEED_KMH, DRIVE), DELTA);
    }

    @Test
    void apply_speedExactlyAtAbsThreshold_doesNotUseAbs() {
        assertEquals(HARD_PRESSURE, brake.apply(HARD_PRESSURE, AT_ABS_SPEED_KMH, DRIVE), DELTA);
    }

    @Test
    void apply_hardBrakingAboveAbsSpeed_alternatesHighAndLowPulses() {
        List<Double> pulses = List.of(
                brake.apply(HARD_PRESSURE, ABOVE_ABS_SPEED_KMH, DRIVE),
                brake.apply(HARD_PRESSURE, ABOVE_ABS_SPEED_KMH, DRIVE),
                brake.apply(HARD_PRESSURE, ABOVE_ABS_SPEED_KMH, DRIVE));

        assertEquals(List.of(ABS_HIGH_PULSE, ABS_LOW_PULSE, ABS_HIGH_PULSE), pulses);
    }

    @Test
    void apply_pressureAboveMaximumAtHighSpeed_usesAbsPulseInsteadOfMaximum() {
        assertEquals(ABS_HIGH_PULSE, brake.apply(OVER_MAX_PRESSURE, HIGH_SPEED_KMH, DRIVE), DELTA);
    }

    @Test
    void apply_nonAbsRequestBetweenAbsRequests_resetsPulseToHigh() {
        brake.apply(HARD_PRESSURE, HIGH_SPEED_KMH, DRIVE);      // high pulse, next would be low
        brake.apply(MODERATE_PRESSURE, HIGH_SPEED_KMH, DRIVE);  // resets the pulse

        assertEquals(ABS_HIGH_PULSE, brake.apply(HARD_PRESSURE, HIGH_SPEED_KMH, DRIVE), DELTA);
    }

    @Test
    void apply_anyPressure_doesNotActivateWarning() {
        brake.apply(MAX_PRESSURE, HIGH_SPEED_KMH, DRIVE);

        assertFalse(brake.isWarningActive());
    }

    @ParameterizedTest(name = "gear {0}")
    @EnumSource(value = Gear.class, mode = EnumSource.Mode.EXCLUDE, names = "P")
    void apply_gearOtherThanPark_appliesRequestedPressure(Gear gear) {
        assertEquals(MODERATE_PRESSURE, brake.apply(MODERATE_PRESSURE, HIGH_SPEED_KMH, gear), DELTA);
    }

    @Test
    void apply_parkGear_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> brake.apply(MODERATE_PRESSURE, HIGH_SPEED_KMH, Gear.P));
    }

    @Test
    void apply_nullGear_throwsNullPointer() {
        assertThrows(NullPointerException.class,
                () -> brake.apply(MODERATE_PRESSURE, HIGH_SPEED_KMH, null));
    }

    @ParameterizedTest(name = "invalid pressure = {0}")
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY})
    void apply_invalidPressure_throwsIllegalArgument(double invalidPressure) {
        assertThrows(IllegalArgumentException.class,
                () -> brake.apply(invalidPressure, HIGH_SPEED_KMH, DRIVE));
    }

    @ParameterizedTest(name = "invalid speed = {0}")
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY})
    void apply_invalidSpeed_throwsIllegalArgument(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class,
                () -> brake.apply(MODERATE_PRESSURE, invalidSpeed, DRIVE));
    }

    @ParameterizedTest(name = "gear {0}")
    @EnumSource(value = Gear.class, mode = EnumSource.Mode.EXCLUDE, names = "P")
    void emergencyBrake_gearOtherThanPark_setsMaxPressureAndWarning(Gear gear) {
        brake.emergencyBrake(gear);

        assertAll(
                () -> assertEquals(MAX_PRESSURE, brake.getPressure(), DELTA),
                () -> assertTrue(brake.isWarningActive()));
    }

    @Test
    void emergencyBrake_parkGear_throwsAndDoesNotActivateWarning() {
        assertThrows(IllegalStateException.class, () -> brake.emergencyBrake(Gear.P));

        assertFalse(brake.isWarningActive());
    }

    @Test
    void emergencyBrake_afterAbsPulse_resetsPulseToHigh() {
        brake.apply(HARD_PRESSURE, HIGH_SPEED_KMH, DRIVE);      // high pulse, next would be low
        brake.emergencyBrake(DRIVE);

        assertEquals(ABS_HIGH_PULSE, brake.apply(HARD_PRESSURE, HIGH_SPEED_KMH, DRIVE), DELTA);
    }
}