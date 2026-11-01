package com.leaseting.parkingterminal.ui.entry

import com.leaseting.parkingterminal.domain.parking.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which chip is lit when the entry form opens.
 *
 * The default exists to save a tap on the common vehicle, not to guess. A wrong
 * type is a wrong rate board and a wrong fee, so an organization whose catalog
 * does not contain a plain "Car" gets nothing preselected rather than whatever
 * happened to be first.
 */
class EntryDefaultVehicleTypeTest {

    @Test
    fun `starts on the car`() {
        val types = listOf(
            VehicleType("2", "Motorcycle", false),
            VehicleType("1", "Car", false),
            VehicleType("3", "Truck", false),
        )

        assertEquals("1", types.defaultSelection()?.id)
    }

    @Test
    fun `does not care how the catalog was typed`() {
        val types = listOf(
            VehicleType("2", "Motorcycle", false),
            VehicleType("1", " car ", false),
        )

        assertEquals("1", types.defaultSelection()?.id)
    }

    @Test
    fun `preselects the only type when there is only one`() {
        assertEquals("9", listOf(VehicleType("9", "Motorcycle", false)).defaultSelection()?.id)
    }

    @Test
    fun `leaves the choice open when nothing is plainly a car`() {
        val types = listOf(
            VehicleType("1", "Sedan", false),
            VehicleType("2", "Motorcycle", false),
        )

        assertNull(types.defaultSelection())
    }

    @Test
    fun `does not mistake a longer name for the car`() {
        val types = listOf(
            VehicleType("1", "Carpool Van", false),
            VehicleType("2", "Motorcycle", false),
        )

        assertNull(types.defaultSelection())
    }

    @Test
    fun `has nothing to select from an empty catalog`() {
        assertNull(emptyList<VehicleType>().defaultSelection())
    }
}
