package autopilot.core;

import autopilot.exception.IllegalGearChangeException;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class GearBoxTest {

    private static class TestableSpeedSensor implements SpeedSensor {
        private double speedKmh;

        public TestableSpeedSensor(double initialSpeed) {
            this.speedKmh = initialSpeed;
        }

        public void setSpeedKmh(double speedKmh) {
            this.speedKmh = speedKmh;
        }

        @Override
        public double getSpeedKmh() {
            return speedKmh;
        }
    }

    private TestableSpeedSensor speedSensorStub;
    private GearBox gearBox;

    @BeforeEach
    void setUp() {
        speedSensorStub = new TestableSpeedSensor(0.0);
        gearBox = new GearBox(speedSensorStub);
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when speedSensor is null")
    void constructor_NullSpeedSensor_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new GearBox(null));
    }

    @Test
    @DisplayName("Initial gear should be Park (P)")
    void initialGear_IsPark() {
        assertEquals(GearBox.Gear.P, gearBox.getCurrentGear());
    }

    @Test
    @DisplayName("Changing to the exact same gear does nothing")
    void changeTo_SameGear_DoesNothing() {
        gearBox.changeTo(GearBox.Gear.P);
        assertEquals(GearBox.Gear.P, gearBox.getCurrentGear());
    }

    @ParameterizedTest
    @CsvSource({
        "3.0", 
        "0.0"
    })
    @DisplayName("changeTo allows valid transition from P to R at low speed")
    void changeTo_FromPtoR_ValidSpeed_Succeeds(double speed) {
        speedSensorStub.setSpeedKmh(speed);
        gearBox.changeTo(GearBox.Gear.R);
        assertEquals(GearBox.Gear.R, gearBox.getCurrentGear());
    }

    @Test
    @DisplayName("changeTo throws IllegalGearChangeException when entering R from P at high speed")
    void changeTo_FromPtoR_HighSpeed_ThrowsException() {
        speedSensorStub.setSpeedKmh(10.0); 
        assertThrows(IllegalGearChangeException.class, () -> gearBox.changeTo(GearBox.Gear.R));
    }

    @Test
    @DisplayName("changeTo throws IllegalArgumentException when speed is negative")
    void changeTo_NegativeSpeed_ThrowsIllegalArgumentException() {
        speedSensorStub.setSpeedKmh(-5.0);
        assertThrows(IllegalArgumentException.class, () -> gearBox.changeTo(GearBox.Gear.R));
    }

    @Test
    @DisplayName("setSpeedSensor updates sensor successfully and throws exception if null")
    void setSpeedSensor_ValidAndNull_Behavior() {
        SpeedSensor newSensor = new TestableSpeedSensor(0.0);
        gearBox.setSpeedSensor(newSensor);
        
        assertThrows(NullPointerException.class, () -> gearBox.setSpeedSensor(null));
    }
}