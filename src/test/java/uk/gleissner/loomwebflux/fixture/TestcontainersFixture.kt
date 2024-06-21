package uk.gleissner.loomwebflux.fixture

import org.testcontainers.containers.PostgreSQLContainer

object TestcontainersFixture {
    val postgres = PostgreSQLContainer("postgres:latest")
}