    package vn.edu.usth.blockappserver

    import io.ktor.server.application.*
    import io.ktor.server.engine.*
    import io.ktor.server.netty.*
    import io.ktor.server.plugins.contentnegotiation.*
    import io.ktor.serialization.kotlinx.json.*
    import io.ktor.server.response.*
    import io.ktor.server.routing.*
    import org.jetbrains.exposed.sql.selectAll
    import org.jetbrains.exposed.sql.transactions.transaction
    import vn.edu.usth.blockappserver.models.UsageLimits
    import vn.edu.usth.blockappserver.models.BlockRuleDTO

    fun main() {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    }

    fun Application.module() {
        // 1. Cài đặt JSON
        install(ContentNegotiation) {
            json()
        }

        // 2. Kết nối Database
        try {
            DatabaseFactory.init()
            println("✅ Kết nối Database thành công!")
        } catch (e: Exception) {
            println("❌ Lỗi kết nối Database: ${e.message}")
        }

        // 3. Routing (API)
        routing {
            get("/") {
                call.respondText("Server Block App đang chạy!")
            }

            // API lấy danh sách chặn từ Database thật
            get("/rules") {
                try {
                    val rules = transaction {
                        UsageLimits.selectAll().map { row ->
                            BlockRuleDTO(
                                packageName = row[UsageLimits.packageName],
                                isBlocked = row[UsageLimits.isBlocked],
                                limitMinutes = row[UsageLimits.dailyLimitMinutes],
                                // Ép kiểu Time sang String để tránh lỗi Serialize
                                startTime = row[UsageLimits.startTime]?.toString(),
                                endTime = row[UsageLimits.endTime]?.toString()
                            )
                        }
                    }
                    call.respond(rules) // Ktor sẽ tự biến list này thành JSON
                } catch (e: Exception) {
                    call.respondText("Lỗi server: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }