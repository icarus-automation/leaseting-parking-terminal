package com.leaseting.parkingterminal.domain.parking

/**
 * The outcome of one call to leaseting-api.
 *
 * Screens never see an HTTP status. They see a [ParkingFailure] they can decide
 * what to say about, plus the API's own sentence when it wrote one — a rejected
 * exit explains itself far better than any message the terminal could invent
 * ("Hourly has no price for Truck", "Cash tendered is less than the 150.00
 * fee"), and those sentences are the ones a guard has to act on.
 */
sealed interface ParkingResult<out T> {
    data class Success<T>(val value: T) : ParkingResult<T>

    data class Failure(
        val reason: ParkingFailure,
        /** The API's message, when it sent one worth showing. */
        val message: String? = null,
    ) : ParkingResult<Nothing>
}

enum class ParkingFailure {
    /** The API could not be reached. Retrying may work; the gate is offline. */
    Unreachable,

    /** The stub, shift or vehicle no longer exists, or never did. */
    NotFound,

    /**
     * The API refused on the current state of things: a stub already paid, a
     * shift already declared, a terminal another guard holds.
     */
    Conflict,

    /** The request was understood and turned down. [ParkingResult.Failure.message] says why. */
    Rejected,

    /** The API answered, but not with anything usable. */
    ServerError,
}

inline fun <T, R> ParkingResult<T>.map(transform: (T) -> R): ParkingResult<R> = when (this) {
    is ParkingResult.Success -> ParkingResult.Success(transform(value))
    is ParkingResult.Failure -> this
}
