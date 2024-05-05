package uk.gleissner.loomwebflux.fixture

import org.junitpioneer.jupiter.cartesian.CartesianTest
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
@CartesianTest.MethodFactory("approachesAndDelayCallDepths")
annotation class CartesianTestApproachesAndDelayCallDepths