package com.iptiq.infrastructure.loadbalancer

import kotlinx.coroutines.delay
import mu.KotlinLogging

data class LoadBalancerConfiguration(
    val maxProvidersCapacity: Int,
    val selectionAlgorithm: ProviderPool.SelectionAlgorithm
)

private const val AWAIT_PROVIDER_DELAY = 300L
private val logger = KotlinLogging.logger {}

/**
 * @todo: concurrent structures
 * @todo: remove recursion calls
 * @todo: AtomicInt for counters
 */
class LoadBalancer(
    configuration: LoadBalancerConfiguration
) {
    private val pool: ProviderPool = when (configuration.selectionAlgorithm) {
        ProviderPool.SelectionAlgorithm.RANDOM -> RandomSelectionProviderPool(configuration.maxProvidersCapacity)
        ProviderPool.SelectionAlgorithm.ROUND_ROBIN -> RoundRobinSelectionProviderPool(configuration.maxProvidersCapacity)
    }

    private val healthCheckers: MutableMap<ProviderId, ProviderHealthChecker> = mutableMapOf()

    private var maximumSupportedRequestsCount = 0
    private var currentProcessingRequestCount = 0

    fun register(vararg providers: Provider) {
        pool.attach(*providers)

        providers.forEach { provider ->
            maximumSupportedRequestsCount += provider.load().maxNumberOfRequests
            provider.withHealthCheck()
        }
    }

    fun remove(provider: Provider) {
        provider.cancelHealthCheck().also {
            pool.detach(provider)
        }

        maximumSupportedRequestsCount -= provider.load().maxNumberOfRequests
    }

    suspend fun handle(code: suspend Provider.() -> Unit) {
        if (maximumSupportedRequestsCount == 0) {
            error("No providers registered to process requests")
        }

        /** No handlers available */
        if (maximumSupportedRequestsCount == currentProcessingRequestCount) {
            logger.info { "There are no handlers available to process the request" }
            delay(AWAIT_PROVIDER_DELAY)

            return handle(code)
        }

        val provider = pool.next()

        try {
            currentProcessingRequestCount++

            provider.handle(code)

            logger.info { "The request was successfully handled using the `${provider.id()}` provider" }
        } finally {
            currentProcessingRequestCount--
        }
    }

    private suspend fun ProviderPool.next(): Provider {
        val provider = get()

        if (provider != null && provider.canHandle()) {
            return provider
        }

        logger.info { "There are no handlers available to process the request" }

        delay(AWAIT_PROVIDER_DELAY)

        return this.next()
    }

    private fun Provider.cancelHealthCheck() {
        healthCheckers[id()]?.let { checker ->
            checker.cancel()
            healthCheckers.remove(id())
        }
    }

    private fun Provider.withHealthCheck() {
        val healthChecker = when (healthCheckMode()) {
            Provider.HealthCheckMode.DEFAULT -> DefaultProviderHealthChecker(
                provider = this,
                onUnavailable = {
                    pool.detach(it)
                },
                onRevived = {
                    pool.attach(it)
                }
            )
            Provider.HealthCheckMode.EXTENDED -> ExtendedProviderHealthChecker(
                provider = this,
                onUnavailable = {
                    pool.detach(it)
                },
                onRevived = {
                    pool.attach(it)
                }
            )
            else -> null
        }

        if (healthChecker != null && !healthCheckers.containsKey(this.id())) {
            healthCheckers[this.id()] = healthChecker

            healthChecker.start()
        }
    }
}
