package com.meetup.kotlin.ktor

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.FieldNamingPolicy
import com.meetup.kotlin.ktor.repository.BookRepository
import com.meetup.kotlin.ktor.repository.DatabaseMigration
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database
import org.slf4j.event.Level
import java.lang.reflect.Modifier
import java.util.*

open class SimpleJwt(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    var verifier = JWT.require(algorithm).build()

    fun sign(name: String): String = JWT.create()
        .withClaim("name", name).sign(algorithm)
}

fun initConfig() {
    ConfigFactory.defaultApplication()
}

fun initDatabase() {
    val dbType = ConfigFactory.load().getString("db_type")
    val config = ConfigFactory.load().getConfig(dbType)

    val properties = Properties()
    config.entrySet().forEach { entry ->
        properties.setProperty(entry.key,
            config.getString(entry.key))
    }
    val hikariConfig = HikariConfig(properties)
    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)
}

fun migrateDatabase() {
    DatabaseMigration.migrate()
}

fun Application.module() {
    install(CallLogging) {
        level = Level.TRACE
        mdc("executionId") {
            UUID.randomUUID().toString()
        }
    }

    install(ContentNegotiation) {
        gson {
            setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            excludeFieldsWithModifiers(Modifier.TRANSIENT)
        }
    }

    val simpleJwt = SimpleJwt("odeio-comer-bolo-com-guardanapo")
    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }


    initConfig()
    initDatabase()
    migrateDatabase()

    install(Routing) {
        book(BookRepository(), simpleJwt)
    }
}

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test")
        ).associateBy { it.name }
    .toMutableMap()
)

fun main() {
    embeddedServer(Netty, 8080,
        module = Application::module).start(wait = true)
}