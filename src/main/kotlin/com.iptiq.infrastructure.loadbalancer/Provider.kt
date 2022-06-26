package com.iptiq.infrastructure.loadbalancer

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

typealias ProviderId = String

interface Provider {

    data class Load(
        val maxNumberOfRequests: Int,
        val currentLoad: Int
    )

    enum class HealthCheckMode {
        NONE,
        DEFAULT,
        EXTENDED
    }

    fun id(): ProviderId
    fun load(): Load
    fun alive(): Boolean
    fun healthCheckMode(): HealthCheckMode

    /**
     * @todo: possible race condition
     */
    fun canHandle(): Boolean
    suspend fun handle(code: suspend Provider.() -> Unit)
}

/**
 * @todo: AtomicInt for counter
 */
class SimpleProvider(
    private val id: ProviderId,
    private val maxNumberOfRequests: Int,
    private val healthCheckMode: Provider.HealthCheckMode = Provider.HealthCheckMode.NONE
) : Provider {

    private var currentRequestsCount: Int = 0
    private var alive = true

    init {
        if (maxNumberOfRequests < 1) {
            error("The maximum number of processed requests cannot be less than 1")
        }

        if (id.isEmpty()) {
            error("Provider id cannot be empty")
        }
    }

    override fun id(): ProviderId = id

    override fun load(): Provider.Load = Provider.Load(
        maxNumberOfRequests = maxNumberOfRequests,
        currentLoad = currentRequestsCount
    )

    override fun alive(): Boolean = alive

    override fun healthCheckMode(): Provider.HealthCheckMode = healthCheckMode

    override fun canHandle(): Boolean = alive && currentRequestsCount <= maxNumberOfRequests

    override suspend fun handle(code: suspend Provider.() -> Unit) {
        currentRequestsCount++

        try {
            code(this)
        } finally {
            currentRequestsCount--
        }
    }
}

/**
 * Provider for displaying the work of the health check function
 */
@OptIn(DelicateCoroutinesApi::class)
class TestProvider(
    private val id: ProviderId,
    private val maxNumberOfRequests: Int,
    private val healthCheckMode: Provider.HealthCheckMode = Provider.HealthCheckMode.NONE,
    private val reviveAfter: Int
) : Provider {

    private var currentRequestsCount: Int = 0
    private var alive = true

    init {
        alive = false

        GlobalScope.launch {
            delay(reviveAfter.toLong())

            alive = true
        }
    }

    override fun id(): ProviderId = id

    override fun load(): Provider.Load = Provider.Load(
        maxNumberOfRequests = maxNumberOfRequests,
        currentLoad = currentRequestsCount
    )

    override fun alive(): Boolean = alive

    override fun healthCheckMode(): Provider.HealthCheckMode = healthCheckMode

    override fun canHandle(): Boolean = alive && currentRequestsCount <= maxNumberOfRequests

    override suspend fun handle(code: suspend Provider.() -> Unit) {
        currentRequestsCount++

        try {
            code(this)
        } finally {
            currentRequestsCount--
        }
    }
}