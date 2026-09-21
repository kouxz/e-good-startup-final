package com.projeto.egoodapp.views.dealer;

import org.junit.Test;
import static org.junit.Assert.*;

public class VehicleSpecificationsTest {
    @Test public void consumptionAcceptsBrazilianAndInternationalDecimalSeparators() {
        assertEquals(12.9, VehicleSpecifications.consumption("12,9"), 0);
        assertEquals(12.9, VehicleSpecifications.consumption(" 12.9 "), 0);
    }
    @Test public void validPowerAndChargingAreNormalized() {
        assertEquals(204, VehicleSpecifications.power("204"));
        assertEquals("AC 11 kW / DC 80 kW", VehicleSpecifications.charging("  AC 11 kW / DC 80 kW  "));
    }
    @Test(expected = IllegalArgumentException.class) public void zeroConsumptionIsRejected() {
        VehicleSpecifications.consumption("0");
    }
    @Test(expected = IllegalArgumentException.class) public void nonFiniteConsumptionIsRejected() {
        VehicleSpecifications.consumption("NaN");
    }
    @Test(expected = IllegalArgumentException.class) public void zeroPowerIsRejected() {
        VehicleSpecifications.power("0");
    }
    @Test(expected = IllegalArgumentException.class) public void blankChargingIsRejected() {
        VehicleSpecifications.charging("  ");
    }
}
