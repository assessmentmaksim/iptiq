package com.iptiq.infrastructure.loadbalancer

interface ProviderPool {

    enum class SelectionAlgorithm {
        RANDOM,
        ROUND_ROBIN
    }

    fun get(): Provider?
    fun attach(vararg providers: Provider)
    fun detach(provider: Provider)
}

class RandomSelectionProviderPool(
    private val maxCapacity: Int
) : ProviderPool {

    private val providerCollection: MutableList<Provider> = mutableListOf()

    override fun get(): Provider? = if (providerCollection.isNotEmpty()) {
        providerCollection.random()
    } else {
        null
    }

    override fun attach(vararg providers: Provider) {
        if ((providerCollection.size + providers.size) > maxCapacity) {
            error("Maximum number of providers reached")
        }

        providerCollection.addAll(providers)
    }

    override fun detach(provider: Provider) {
        providerCollection.remove(provider)
    }
}

class RoundRobinSelectionProviderPool(
    private val maxCapacity: Int
) : ProviderPool {
    private val providerCollection: MutableList<Provider> = mutableListOf()
    private var nextHostIndex: Int = 0

    override fun get(): Provider? {
        if (providerCollection.isEmpty()) {
            return null
        }

        val provider = providerCollection[nextHostIndex]

        this.nextHostIndex++
        this.nextHostIndex %= providerCollection.count()

        return provider
    }

    override fun attach(vararg providers: Provider) {
        if ((providerCollection.size + providers.size) > maxCapacity) {
            error("Maximum number of providers reached")
        }

        providerCollection.addAll(providers).also {
            resetIndex()
        }
    }

    override fun detach(provider: Provider) {
        providerCollection.remove(provider).also {
            resetIndex()
        }
    }

    private fun resetIndex() {
        this.nextHostIndex = if (providerCollection.isNotEmpty()) {
            providerCollection.indexOf(providerCollection.first())
        } else {
            0
        }
    }
}