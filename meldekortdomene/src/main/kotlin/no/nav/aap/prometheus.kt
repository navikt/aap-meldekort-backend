package no.nav.aap

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
