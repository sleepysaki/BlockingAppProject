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

        get("/users/{id}/groups") {
            try {
                val userIdStr = call.parameters["id"]
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing User ID"))
                    return@get
                }

                val userUuid = UUID.fromString(userIdStr)

                val groups = transaction {
                    (Groups innerJoin GroupMembers)
                        .select(Groups.groupId, Groups.name, Groups.joinCode, GroupMembers.role)
                        .where { GroupMembers.userId eq userUuid }
                        .map {
                            mapOf(
                                "groupId" to it[Groups.groupId].toString(),
                                "groupName" to it[Groups.name],
                                "joinCode" to it[Groups.joinCode],
                                "role" to it[GroupMembers.role]
                            )
                        }
                }
                call.respond(groups)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

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

        post("/groups/create") {
            try {
                val req = call.receive<CreateGroupRequest>()
                val newJoinCode = (1..6).map { ('A'..'Z').random() }.joinToString("")
                val adminUuid = UUID.fromString(req.adminId)

                val newGroupId = transaction {
                    val gId = Groups.insert {
                        it[name] = req.name
                        it[createdBy] = adminUuid
                        it[joinCode] = newJoinCode
                        it[type] = "FAMILY"
                    } get Groups.groupId

                    GroupMembers.insert {
                        it[groupId] = gId
                        it[userId] = adminUuid
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

        post("/groups/join") {
            try {
                val req = call.receive<JoinGroupRequest>()
                val userUuid = UUID.fromString(req.userId)

                val resultMessage = transaction {
                    val groupRow = Groups.selectAll().where { Groups.joinCode eq req.joinCode }.singleOrNull()
                    if (groupRow == null) return@transaction "Group not found"

                    val gId = groupRow[Groups.groupId]

                    val exists = GroupMembers.selectAll()
                        .where { (GroupMembers.groupId eq gId) and (GroupMembers.userId eq userUuid) }
                        .count() > 0

                    if (exists) return@transaction "Already joined"

                    GroupMembers.insert {
                        it[groupId] = gId
                        it[userId] = userUuid
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
                            GroupMembers.role
                        )
                        .where { GroupMembers.groupId eq UUID.fromString(groupId) }
                        .map { row ->
                            GroupMemberResponse(
                                userId = row[Users.userId].toString(),
                                fullName = row[Users.fullName],
                                email = row[Users.email],
                                role = row[GroupMembers.role]
                            )
                        }
                }
                call.respond(members)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }

            // 4. Leave Group
            post("/groups/leave") {
                try {
                    val req = call.receive<LeaveGroupRequest>()
                    val gId = UUID.fromString(req.groupId)
                    val uId = UUID.fromString(req.userId)

                    val deletedCount = transaction {
                        GroupMembers.deleteWhere {
                            (GroupMembers.groupId eq gId) and (GroupMembers.userId eq uId)
                        }
                    }

                    if (deletedCount > 0) {
                        call.respond(mapOf("status" to "success", "message" to "Left group successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Member not found in group"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            // 5. Remove Member
            post("/groups/remove") {
                try {
                    val req = call.receive<RemoveMemberRequest>()
                    val gId = UUID.fromString(req.groupId)
                    val adminUuid = UUID.fromString(req.adminId)
                    val targetUuid = UUID.fromString(req.targetUserId)

                    val result = transaction {
                        val adminRole = GroupMembers
                            .select(GroupMembers.role)
                            .where { (GroupMembers.groupId eq gId) and (GroupMembers.userId eq adminUuid) }
                            .map { it[GroupMembers.role] }
                            .singleOrNull()

                        if (adminRole != "ADMIN") {
                            return@transaction "Permission Denied: Only Admin can remove members"
                        }

                        if (adminUuid == targetUuid) {
                            return@transaction "Cannot remove yourself. Use 'Leave Group' instead."
                        }

                        val deleted = GroupMembers.deleteWhere {
                            (GroupMembers.groupId eq gId) and (GroupMembers.userId eq targetUuid)
                        }

                        if (deleted > 0) "Success" else "Target member not found"
                    }

                    if (result == "Success") {
                        call.respond(mapOf("status" to "success", "message" to "Member removed"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to result))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
            // 6. Get Rules of a Group (Member call)
            get("/groups/{id}/rules") {
                try {
                    val gIdStr = call.parameters["id"]
                    if (gIdStr == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Group ID"))
                        return@get
                    }

                    val rules = transaction {
                        GroupRules.select(GroupRules.packageName, GroupRules.isBlocked)
                            .where { GroupRules.groupId eq UUID.fromString(gIdStr) }
                            .map {
                                GroupRuleDTO(
                                    groupId = gIdStr,
                                    packageName = it[GroupRules.packageName],
                                    isBlocked = it[GroupRules.isBlocked]
                                )
                            }
                    }
                    call.respond(rules)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            // 7. Add/Update Group Rule (Admin call)
            post("/groups/rules") {
                try {
                    val req = call.receive<GroupRuleDTO>()
                    val gId = UUID.fromString(req.groupId)

                    transaction {
                        val existing = GroupRules.selectAll()
                            .where { (GroupRules.groupId eq gId) and (GroupRules.packageName eq req.packageName) }
                            .singleOrNull()

                        if (existing != null) {
                            GroupRules.update({ (GroupRules.groupId eq gId) and (GroupRules.packageName eq req.packageName) }) {
                                it[isBlocked] = req.isBlocked
                            }
                        } else {
                            GroupRules.insert {
                                it[GroupRules.groupId] = gId
                                it[GroupRules.packageName] = req.packageName
                                it[GroupRules.isBlocked] = req.isBlocked
                            }
                        }
                    }
                    call.respond(mapOf("status" to "success", "message" to "Rule updated"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }
        }
    }
}