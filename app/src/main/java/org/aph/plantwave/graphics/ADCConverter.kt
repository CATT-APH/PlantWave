package org.aph.plantwave.graphics

/** Converts the Arduino's raw 10-bit ADC reading to millivolts against its 5V reference. */
object ADCConverter {
    fun toMillivolts(adcValue: Int): Double = (adcValue / 1024.0) * 5000.0
}
