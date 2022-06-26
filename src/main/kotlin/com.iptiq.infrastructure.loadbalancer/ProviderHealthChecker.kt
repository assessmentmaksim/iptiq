package com.iptiq.infrastructure.loadbalancer

import mu.KotlinLogging
import java.util.*
import kotlin.concurrent.timer

private val logger = KotlinLogging.logger {}

interface ProviderHealthChecker {
    fun start()
    fun cancel()
}

class DefaultProviderHealthChecker(
    private val provider: Provider,
    private val onUnavailable: (Provider) -> Unit,
    private val onRevived: (Provider) -> Unit
) : ProviderHealthChecker {

    private var timer: Timer? = null
    private var wasRemoved: Boolean = false

    companion object {
        private const val HEALTH_CHECK_INTERVAL = 500L
    }

    override fun start() {
        if (timer != null) {
            error("Health checker already started")
        }

        wasRemoved = false
        timer = timer(
            name = "Health check for `${provider.id()}` provider",
            daemon = false,
            startAt = Date(),
            period = HEALTH_CHECK_INTERVAL
        ) {
            val active = provider.alive()

            if (active && wasRemoved) {
                onRevived(provider)
                wasRemoved = false

                logger.info { "`${provider.id()}` provider is now available to process requests again" }
            }

            if (!active && !wasRemoved) {
                onUnavailable(provider)
                wasRemoved = true

                logger.info { "The `${provider.id()}` provider is no longer available to process requests" }
            }
        }

        logger.info { "Started status check for `${provider.id()}` provider" }
    }

    override fun cancel() {
        timer?.cancel()
        timer = null

        logger.info { "Canceled status check for `${provider.id()}` provider" }
    }
}

class ExtendedProviderHealthChecker(
    private val provider: Provider,
    private val onUnavailable: (Provider) -> Unit,
    private val onRevived: (Provider) -> Unit
) : ProviderHealthChecker {

    private var timer: Timer? = null
    private var successfulChecksCount = 0
    private var wasRemoved: Boolean = false

    companion object {
        private const val HEALTH_CHECK_INTERVAL = 500L
        private const val EXPECTED_POSITIVE_TESTS_COUNT = 2
    }

    override fun start() {
        if (timer != null) {
            error("Health checker already started")
        }

        successfulChecksCount = 0
        wasRemoved = false

        timer = timer(
            name = "Health check for `${provider.id()}` provider",
            daemon = false,
            startAt = Date(),
            period = HEALTH_CHECK_INTERVAL
        ) {
            val active = provider.alive()

            if (active && wasRemoved) {
                if (++successfulChecksCount == EXPECTED_POSITIVE_TESTS_COUNT) {
                    onRevived(provider)
                    wasRemoved = false
                    successfulChecksCount = 0

                    logger.info { "`${provider.id()}` provider is now available to process requests again" }
                } else {
                    logger.info {
                        "The provider `${provider.id()}` has changed its status to working, await for re-confirmation"
                    }
                }
            }

            if (!active && !wasRemoved) {
                onUnavailable(provider)
                wasRemoved = true
                successfulChecksCount = 0

                logger.info { "The `${provider.id()}` provider is no longer available to process requests" }
            }
        }

        logger.info { "Started status check for `${provider.id()}` provider" }
    }

    override fun cancel() {
        timer?.cancel()
        timer = null

        logger.info { "Canceled status check for `${provider.id()}` provider" }
    }
}