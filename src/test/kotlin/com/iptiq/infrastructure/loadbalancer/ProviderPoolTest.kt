package com.iptiq.infrastructure.loadbalancer

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertSame

@DisplayName("Testing a collection of providers")
internal class ProviderPoolTest {

    @Test
    fun `Make sure that limiting the number of active providers works for random selection`() {
        assertThrows<Exception> {
            val pool = RandomSelectionProviderPool(5)

            repeat(6) {
                pool.attach(SimpleProvider("provider_$it", 10))
            }
        }
    }

    @Test
    fun `Make sure that limiting the number of active providers works for round robin selection`() {
        assertThrows<Exception> {
            val pool = RoundRobinSelectionProviderPool(5)

            repeat(6) {
                pool.attach(SimpleProvider("provider_$it", 10))
            }
        }
    }

    @Test
    fun `Make sure that we get providers strictly sequentially for round robin selection`() {
        val pool = RoundRobinSelectionProviderPool(5)

        pool.attach(
            SimpleProvider("first", 1),
            SimpleProvider("second", 1),
            SimpleProvider("third", 1)
        )

        assertSame("first", pool.get()?.id())
        assertSame("second", pool.get()?.id())
        assertSame("third", pool.get()?.id())
        assertSame("first", pool.get()?.id())
    }

    @Test
    fun `Make sure that when adding a new provider, we reset the index for round robin selection`() {
        val pool = RoundRobinSelectionProviderPool(5)

        pool.attach(
            SimpleProvider("first", 1),
            SimpleProvider("second", 1),
            SimpleProvider("third", 1)
        )

        pool.get()

        pool.attach(SimpleProvider("fourth", 1))

        assertSame("first", pool.get()?.id())
    }

    @Test
    fun `Make sure that when we delete the provider, we reset the index for round robin`() {
        val pool = RoundRobinSelectionProviderPool(5)

        val provider = SimpleProvider("first", 1)

        pool.attach(
            provider,
            SimpleProvider("second", 1),
            SimpleProvider("third", 1)
        )

        pool.get()

        pool.detach(provider)

        assertSame("second", pool.get()?.id())
    }

    @Test
    fun `Make sure that the pool for a random selection of the provider works correctly with an empty list`() {
        val pool = RandomSelectionProviderPool(5)

        assertSame(null, pool.get())
    }

    @Test
    fun `Make sure that the pool for a round robin selection of the provider works correctly with an empty list`() {
        val pool = RoundRobinSelectionProviderPool(5)

        assertSame(null, pool.get())
    }
}
