package com.gulali.gpos

import org.junit.Test
import java.util.UUID

class HelperUnitTest {
    @Test
    fun generateIdPayment() {
        val randomID = "${UUID.randomUUID()}"
        val uniqueID = randomID.split("-")

    }
}