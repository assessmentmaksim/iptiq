package com.iptiq.infrastructure.loadbalancer

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Testing the Load Balancer")
internal class LoadBalancerTest {

    @Test
    fun `Make sure that the request cannot be executed without configured providers`(): Unit = runBlocking {
        assertThrows<Exception> {
            val loadBalancer = LoadBalancer(
                LoadBalancerConfiguration(
                    maxProvidersCapacity = 5,
                    selectionAlgorithm = ProviderPool.SelectionAlgorithm.RANDOM
                )
            )

            loadBalancer.handle { }
        }
    }

    @Test
    fun `Make sure the health check works correctly`(): Unit = runBlocking {
        val provider = TestProvider(
            id = "Test",
            maxNumberOfRequests = 10,
            healthCheckMode = Provider.HealthCheckMode.DEFAULT,
            reviveAfter = 5000
        )

        val loadBalancer = LoadBalancer(
            LoadBalancerConfiguration(
                maxProvidersCapacity = 5,
                selectionAlgorithm = ProviderPool.SelectionAlgorithm.RANDOM
            )
        )

        loadBalancer.register(provider)

        loadBalancer.handle { }
    }

    @Test
    fun `Make sure the extended health check works correctly`(): Unit = runBlocking {
        val provider = TestProvider(
            id = "Test",
            maxNumberOfRequests = 10,
            healthCheckMode = Provider.HealthCheckMode.EXTENDED,
            reviveAfter = 5000
        )

        val loadBalancer = LoadBalancer(
            LoadBalancerConfiguration(
                maxProvidersCapacity = 5,
                selectionAlgorithm = ProviderPool.SelectionAlgorithm.RANDOM
            )
        )

        loadBalancer.register(provider)

        loadBalancer.handle { }
    }
}