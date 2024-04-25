package uk.gleissner.loomwebflux.fixture

import org.junit.jupiter.params.provider.MethodSource
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
@MethodSource("approaches")
annotation class ApproachesMethodSource