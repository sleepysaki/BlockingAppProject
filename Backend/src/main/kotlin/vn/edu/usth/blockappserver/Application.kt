package vn.edu.usth.blockappserver

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import vn.edu.usth.blockappserver.model.*
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    try {
        DatabaseFactory.init()
        println("Database connected successfully")
    } catch (e: Exception) {
        println("Database connection failed: ${e.message}")
    }

    routing {
        get("/") {
            call.respondText("Block App Server is running!")
        }

        // GET: Fetch rules
        get("/rules") {
            try {
                val rules = transaction {
                    UsageLimits.selectAll().map { row ->
                        BlockRuleDTO(
                            packageName = row[UsageLimits.packageName],
                            isBlocked = row[UsageLimits.isBlocked],
                            limitMinutes = row[UsageLimits.dailyLimitMinutes],
                            startTime = row[UsageLimits.startTime]?.toString(),
                            endTime = row[UsageLimits.endTime]?.toString(),
                            latitude = row[UsageLimits.latitude],
                            longitude = row[UsageLimits.longitude],
                            radius = row[UsageLimits.radius]
                        )
                    }
                }
                call.respond(rules)
            } catch (e: Exception) {
                call.respondText("Server error: ${e.message}")
                e.printStackTrace()
            }
        }

        // Get Groups for a User
        get("/users/{id}/groups") {
            try {
                val userIdStr = call.parameters["id"]
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing User ID"))
                    return@get
                }

                val userUuid = UUID.fromString(userIdStr)

                val groups = transaction {
                    // Join Groups and GroupMembers tables
                    (Groups innerJoin GroupMembers)
                        .select(Groups.groupId, Groups.name, Groups.joinCode, GroupMembers.role) // Lấy role từ DB
                        .where { GroupMembers.userId eq userUuid }
                        .map {
                            mapOf(
                                "groupId" to it[Groups.groupId].toString(),
                                "groupName" to it[Groups.name],
                                "joinCode" to it[Groups.joinCode],
                                "role" to it[GroupMembers.role] // Trả về role (ADMIN/MEMBER)
                            )
                        }
                }
                call.respond(groups)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // POST: Save rules
        post("/rules") {
            try {
                val rule = call.receive<BlockRuleDTO>()

                transaction {
                    val existing = UsageLimits.selectAll()
                        .where { UsageLimits.packageName eq rule.packageName }
                        .singleOrNull()

                    if (existing != null) {
                        UsageLimits.update({ UsageLimits.packageName eq rule.packageName }) {
                            it[isBlocked] = true
                            it[latitude] = rule.latitude ?: 0.0
                            it[longitude] = rule.longitude ?: 0.0
                            it[radius] = rule.radius ?: 100.0
                        }
                    } else {
                        UsageLimits.insert {
                            it[packageName] = rule.packageName
                            it[isBlocked] = true
                            it[dailyLimitMinutes] = 0
                            it[latitude] = rule.latitude ?: 0.0
                            it[longitude] = rule.longitude ?: 0.0
                            it[radius] = rule.radius ?: 100.0
                        }
                    }
                }
                call.respond(mapOf("status" to "success", "message" to "Saved successfully"))

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Register API
        post("/auth/register") {
            try {
                val request = call.receive<RegisterRequest>()
                transaction {
                    Users.insert {
                        it[email] = request.email
                        it[passwordHash] = request.password
                        it[fullName] = request.fullName
                        it[role] = request.role
                    }
                }
                call.respond(mapOf("message" to "Registration successful!"))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Registration failed: ${e.message}"))
            }
        }

        // Login API
        post("/auth/login") {
            try {
                val request = call.receive<LoginRequest>()
                val user = transaction {
                    Users.selectAll()
                        .where { (Users.email eq request.email) and (Users.passwordHash eq request.password) }
                        .map {
                            UserDTO(
                                id = it[Users.userId].toString(),
                                email = it[Users.email],
                                fullName = it[Users.fullName],
                                role = it[Users.role]
                            )
                        }
                        .singleOrNull()
                }

                if (user != null) {
                    val dynamicToken = UUID.randomUUID().toString()
                    call.respond(LoginResponse(token = dynamicToken, user = user))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Incorrect email or password!"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid request format: ${e.message}"))
            }
        }

        // --- GROUP MANAGEMENT APIs ---

        // 1. Create Group
        post("/groups/create") {
            try {
                val req = call.receive<CreateGroupRequest>()
                val newJoinCode = (1..6).map { ('A'..'Z').random() }.joinToString("")
                val adminUuid = UUID.fromString(req.adminId)

                val newGroupId = transaction {
                    // Create Group
                    val gId = Groups.insert {
                        it[name] = req.name
                        it[createdBy] = adminUuid
                        it[joinCode] = newJoinCode
                        it[type] = "FAMILY"
                    } get Groups.groupId

                    // Add Admin to GroupMembers
                    GroupMembers.insert {
                        it[groupId] = gId
                        it[userId] = adminUuid
                        // 👇 QUAN TRỌNG: Phải set Role là ADMIN
                        it[role] = "ADMIN"
                        it[isGroupAdmin] = true
                    }
                    gId
                }

                call.respond(
                    CreateGroupResponse(
                        status = "success",
                        message = "Group created successfully",
                        groupId = newGroupId.toString(),
                        joinCode = newJoinCode
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // 2. Join Group
        post("/groups/join") {
            try {
                val req = call.receive<JoinGroupRequest>()
                val userUuid = UUID.fromString(req.userId)

                val resultMessage = transaction {
                    val groupRow = Groups.selectAll().where { Groups.joinCode eq req.joinCode }.singleOrNull()
                    if (groupRow == null) return@transaction "Group not found"

                    val gId = groupRow[Groups.groupId]

                    // Check duplicate
                    val exists = GroupMembers.selectAll()
                        .where { (GroupMembers.groupId eq gId) and (GroupMembers.userId eq userUuid) }
                        .count() > 0

                    if (exists) return@transaction "Already joined"

                    GroupMembers.insert {
                        it[groupId] = gId
                        it[userId] = userUuid
                        // 👇 QUAN TRỌNG: Phải set Role là MEMBER
                        it[role] = "MEMBER"
                        it[isGroupAdmin] = false
                    }
                    "Success"
                }

                if (resultMessage == "Group not found") {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to resultMessage))
                } else {
                    call.respond(mapOf("status" to "success", "message" to resultMessage))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // 3. Get Members
        get("/groups/{id}/members") {
            val groupId = call.parameters["id"]

            try {
                if (groupId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Group ID"))
                    return@get
                }

                val members = transaction {
                    (GroupMembers innerJoin Users)
                        .select(
                            Users.userId,
                            Users.fullName,
                            Users.email,
                            GroupMembers.role // Lấy cột role
                        )
                        .where { GroupMembers.groupId eq UUID.fromString(groupId) }
                        .map { row ->
                            GroupMemberResponse(
                                userId = row[Users.userId].toString(),
                                fullName = row[Users.fullName],
                                email = row[Users.email],
                                role = row[GroupMembers.role] // Map cột role vào JSON
                            )
                        }
                }
                call.respond(members)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}