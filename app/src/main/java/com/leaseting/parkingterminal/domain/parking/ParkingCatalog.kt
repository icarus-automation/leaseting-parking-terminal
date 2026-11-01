package com.leaseting.parkingterminal.domain.parking

/**
 * The organization's parking configuration, read once after sign-in.
 *
 * Everything here is owned by staff in the web client and can change between
 * shifts, so the terminal treats it as a snapshot to draw pickers from — never
 * as something to compute money with. Fees come from the API on every exit,
 * which is why no rate plan or rounding rule is modelled on the device.
 */
data class ParkingCatalog(
    val terminals: List<ParkingTerminal>,
    val vehicleTypes: List<VehicleType>,
) {
    /** The only terminals a guard may open a shift on. */
    val selectableTerminals: List<ParkingTerminal>
        get() = terminals.filter { it.isActive && !it.propertyIsArchived }

    /** The only types a guard may record an entry against. */
    val selectableVehicleTypes: List<VehicleType>
        get() = vehicleTypes.filter { !it.isArchived }
}

/** A named handheld bound to one property. */
data class ParkingTerminal(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val propertyId: String,
    val propertyName: String,
    val propertyIsArchived: Boolean,
)

/** A vehicle category the rate board prices separately. */
data class VehicleType(
    val id: String,
    val name: String,
    val isArchived: Boolean,
)
