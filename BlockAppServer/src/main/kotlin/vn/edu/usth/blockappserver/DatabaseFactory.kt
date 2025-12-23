package vn.edu.usth.blockappserver

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        Database.connect(hikari())
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"

        // --- SỬA ĐÚNG 1 DÒNG NÀY ---
        // Giữ nguyên địa chỉ aws-1-ap-southeast-2 của bác
        // Thêm đoạn đuôi: &prepareThreshold=0
        config.jdbcUrl = "jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"

        // Giữ nguyên User của bác
        config.username = "postgres.wtyalqkxbtqwirxyhwus"

        // Giữ nguyên Password của bác
        config.password = "groupproject_3667"

        // Các cấu hình khác giữ nguyên
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.connectionTimeout = 30000
        config.validate()

        return HikariDataSource(config)
    }
}