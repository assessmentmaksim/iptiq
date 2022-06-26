package com.iptiq.infrastructure.loadbalancer

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertSame

@DisplayName("Default provider test")
internal class SimpleProviderTest {

    @Test
    fun `Make sure we can't create a provider without specifying a id`() {
        assertThrows<Exception> {
            SimpleProvider(
                id = "",
                maxNumberOfRequests = 100
            )
        }
    }

    @Test
    fun `Make sure that we cannot create a provider without the ability to process the request`() {
        assertThrows<Exception> {
            SimpleProvider(
                id = "example",
                maxNumberOfRequests = 0
            )
        }
    }

    @Test
    fun `Success creation`() {
        val provider = SimpleProvider(
            id = "example",
            maxNumberOfRequests = 50
        )

        assertSame("example", provider.id())
    }
}
