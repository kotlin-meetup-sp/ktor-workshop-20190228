package com.meetup.kotlin.ktor.repository

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway

object DatabaseMigration {

    fun migrate() {
        val dbType = ConfigFactory.load().getString("db_type")
        val config = ConfigFactory.load().getConfig(dbType)
        val flyway = Flyway.configure()
            .dataSource(config.getString("dataSource.url"),
                config.getString("dataSource.user"),
                config.getString("dataSource.password"))
            .schemas("library")
            .locations("db/migration")
            .load()
        flyway.migrate()
    }
}