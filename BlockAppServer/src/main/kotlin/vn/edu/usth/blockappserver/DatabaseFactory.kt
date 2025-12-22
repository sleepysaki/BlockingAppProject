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

        // 1. URL kết nối (Host + Port + Database Name + SSL)
        config.jdbcUrl = "jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=require"

        // 2. Username (Lấy từ ảnh bạn gửi)
        config.username = "postgres.wtyalqkxbtqwirxyhwus"

        // 3. Password (QUAN TRỌNG: Thay mật khẩu DB của bạn vào đây)
        config.password = "groupproject_3667"

        // Các cấu hình tối ưu giữ nguyên
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.connectionTimeout = 30000
        config.validate()

        return HikariDataSource(config)
    }
}