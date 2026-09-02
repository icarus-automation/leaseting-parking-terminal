package com.leaseting.parkingterminal.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AudienceTest {

    @Test
    fun `maps the wire values leaseting-api reports`() {
        assertEquals(Audience.PARKING, Audience.fromWireValue("parking"))
        assertEquals(Audience.STAFF, Audience.fromWireValue("staff"))
        assertEquals(Audience.TENANT, Audience.fromWireValue("tenant"))
    }

    @Test
    fun `treats an unrecognised or absent audience as unknown`() {
        assertEquals(Audience.UNKNOWN, Audience.fromWireValue(null))
        assertEquals(Audience.UNKNOWN, Audience.fromWireValue(""))
        assertEquals(Audience.UNKNOWN, Audience.fromWireValue("something_new"))
    }
}
