package autopilot.core;

import autopilot.core.GearBox.Gear;
import autopilot.exception.IllegalGearChangeException;
import autopilot.exception.SensorFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GearBoxTest {

    private static final double STANDSTILL_KMH = 0.0;
    private static final String SENSOR_FAILURE_MESSAGE = "speed sensor failed";

    private double currentSpeedKmh;
    private GearBox gearBox;

    @BeforeEach
    void setUp() {
        currentSpeedKmh = STANDSTILL_KMH;
        gearBox = new GearBox(() -> currentSpeedKmh);
    }

    private void moveTo(Gear gear) {
        currentSpeedKmh = STANDSTILL_KMH;
        switch (gear) {
            case P -> { }
            case R -> gearBox.changeTo(Gear.R);
            case N -> {
                gearBox.changeTo(Gear.R);
                gearBox.changeTo(Gear.N);
            }
            case D -> {
                gearBox.changeTo(Gear.R);
                gearBox.changeTo(Gear.N);
                gearBox.changeTo(Gear.D);
            }
        }
    }

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(Gear.P, Gear.R, 0.0),
                Arguments.of(Gear.P, Gear.R, 4.9),    
                Arguments.of(Gear.R, Gear.N, 0.0),
                Arguments.of(Gear.R, Gear.N, 50.0),    
                Arguments.of(Gear.N, Gear.D, 50.0),    
                Arguments.of(Gear.R, Gear.D, 0.0),
                Arguments.of(Gear.D, Gear.R, 0.0));
    }

    static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(Gear.P, Gear.R, 5.0),     
                Arguments.of(Gear.P, Gear.R, 20.0),
                Arguments.of(Gear.R, Gear.D, 0.1),
                Arguments.of(Gear.D, Gear.R, 0.1),
                Arguments.of(Gear.P, Gear.N, 0.0),
                Arguments.of(Gear.P, Gear.D, 0.0),
                Arguments.of(Gear.R, Gear.P, 0.0),
                Arguments.of(Gear.N, Gear.P, 0.0),
                Arguments.of(Gear.N, Gear.R, 0.0),
                Arguments.of(Gear.D, Gear.P, 0.0),
                Arguments.of(Gear.D, Gear.N, 0.0));
    }

    @ParameterizedTest(name = "{0} -> {1} at {2} km/h")
    @MethodSource("allowedTransitions")
    void changeTo_allowedTransition_changesGear(Gear from, Gear to, double speedKmh) {
        moveTo(from);
        currentSpeedKmh = speedKmh;

        gearBox.changeTo(to);

        assertEquals(to, gearBox.getCurrentGear());
    }

    @ParameterizedTest(name = "{0} -> {1} at {2} km/h")
    @MethodSource("forbiddenTransitions")
    void changeTo_forbiddenTransition_throwsAndKeepsCurrentGear(Gear from, Gear to, double speedKmh) {
        moveTo(from);
        currentSpeedKmh = speedKmh;

        assertThrows(IllegalGearChangeException.class, () -> gearBox.changeTo(to));
        assertEquals(from, gearBox.getCurrentGear());
    }

    @ParameterizedTest(name = "gear {0}")
    @EnumSource(Gear.class)
    void changeTo_sameGear_doesNothingEvenIfSensorFails(Gear gear) {
        moveTo(gear);
        gearBox.setSpeedSensor(() -> {
            throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
        });

        gearBox.changeTo(gear);

        assertEquals(gear, gearBox.getCurrentGear());
    }

    @ParameterizedTest(name = "invalid speed = {0}")
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY})
    void changeTo_invalidSpeedReading_throwsIllegalArgument(double invalidSpeed) {
        currentSpeedKmh = invalidSpeed;

        assertThrows(IllegalArgumentException.class, () -> gearBox.changeTo(Gear.R));
    }

    @Test
    void changeTo_sensorFails_propagatesFailureAndKeepsGear() {
        gearBox.setSpeedSensor(() -> {
            throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
        });

        assertThrows(SensorFailureException.class, () -> gearBox.changeTo(Gear.R));
        assertEquals(Gear.P, gearBox.getCurrentGear());
    }

    @Test
    void changeTo_nullGear_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> gearBox.changeTo(null));
    }
}
