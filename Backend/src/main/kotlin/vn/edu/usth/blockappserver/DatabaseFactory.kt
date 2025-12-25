package com.usth

import com.usth.model.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        Database.connect(hikari())
        transaction {
            // Tự động tạo bảng nếu chưa có
            SchemaUtils.createMissingTablesAndColumns(Users, Groups, GroupMembers, UsageLimits, GroupRules)
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        // URL Database của bạn
        config.jdbcUrl = "jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
        config.username = "postgres.wtyalqkxbtqwirxyhwus"
        config.password = "groupproject_3667"

        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }
}