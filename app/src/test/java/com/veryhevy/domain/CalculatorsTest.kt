package com.veryhevy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorsTest {

    @Test
    fun oneRepMax_epley() {
        assertEquals(100.0, Calculators.oneRepMax(100.0, 1), 0.001)
        assertEquals(0.0, Calculators.oneRepMax(100.0, 0), 0.001)
        assertEquals(150.0, Calculators.oneRepMax(100.0, 15), 0.001)
    }

    @Test
    fun setVolume() {
        assertEquals(0.0, Calculators.setVolume(100.0, 0), 0.001)
        assertEquals(500.0, Calculators.setVolume(50.0, 10), 0.001)
    }

    @Test
    fun platesFor_standardBar() {
        val plates = Calculators.platesFor(100.0, 20.0, listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25))
        // 80 kg on the bar => 40 kg per side = 25 + 15
        assertEquals(listOf(25.0, 15.0), plates)
    }

    @Test
    fun platesFor_exactSplit() {
        val plates = Calculators.platesFor(60.0, 20.0, listOf(25.0, 10.0, 5.0, 2.5))
        // 20 kg per side; a 25 overshoots, so two 10s
        assertEquals(listOf(10.0, 10.0), plates)
    }

    @Test
    fun platesFor_belowBar() {
        assertEquals(emptyList<Double>(), Calculators.platesFor(20.0, 20.0, listOf(10.0)))
    }

    @Test
    fun unitConversion() {
        assertEquals(220.46, Calculators.kgToLb(100.0), 0.01)
        assertEquals(100.0, Calculators.lbToKg(Calculators.kgToLb(100.0)), 0.001)
    }

}
