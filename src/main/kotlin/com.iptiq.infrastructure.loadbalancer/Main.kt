package com.iptiq.infrastructure.loadbalancer

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    embeddedServer(Netty, port = 9090, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

/**
 * @todo: Koin
 */
fun Application.configureRouting() {
    install(StatusPages) {
        exception<Exception> { call, cause ->
            logger.error { cause.printStackTrace() }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    routing {
        get("/success") {
            val loadBalancer = LoadBalancer(
                LoadBalancerConfiguration(
                    maxProvidersCapacity = 10,
                    selectionAlgorithm = ProviderPool.SelectionAlgorithm.ROUND_ROBIN
                )
            )

            loadBalancer.register(
                SimpleProvider(
                    id = "first",
                    maxNumberOfRequests = 5,
                    healthCheckMode = Provider.HealthCheckMode.DEFAULT
                ),
                SimpleProvider(
                    id = "second",
                    maxNumberOfRequests = 3,
                    healthCheckMode = Provider.HealthCheckMode.EXTENDED
                ),
                SimpleProvider(
                    id = "third",
                    maxNumberOfRequests = 10,
                    healthCheckMode = Provider.HealthCheckMode.EXTENDED
                ),
                SimpleProvider(
                    id = "fourth",
                    maxNumberOfRequests = 4,
                    healthCheckMode = Provider.HealthCheckMode.EXTENDED
                ),
            )

            (1..100)
                .map {
                    async {
                        loadBalancer.handle {
                            delay((500..1500).random().toLong())
                        }
                    }
                }
                .awaitAll()

            logger.info { "All requests processed" }

            call.respondText("Check console")
        }

        get("/with-retry") {
            val provider = TestProvider(
                id = "first",
                maxNumberOfRequests = 5,
                healthCheckMode = Provider.HealthCheckMode.EXTENDED,
                reviveAfter = 10000
            )

            val loadBalancer = LoadBalancer(
                LoadBalancerConfiguration(
                    maxProvidersCapacity = 10,
                    selectionAlgorithm = ProviderPool.SelectionAlgorithm.ROUND_ROBIN
                )
            )

            loadBalancer.register(provider)

            repeat(2) {
                loadBalancer.handle {

                }
            }

            call.respondText("Check console")
        }
    }
}