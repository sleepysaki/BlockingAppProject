package com.usth

import com.usth.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Application.configureRouting() {
    routing {
        trace { application.log.trace(it.buildText()) }

        get("/") {
            call.respondText("Block App Server is RUNNING (Full Version)!")
        }

        // --- AUTHENTICATION ---
        post("/auth/register") {
            try {
                val req = call.receive<RegisterRequest>()
                transaction {
                    Users.insert {
                        it[email] = req.email
                        it[passwordHash] = req.password
                        it[fullName] = req.fullName
                        it[role] = req.role
                    }
                }
                call.respond(mapOf("message" to "Registration successful!"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Failed: ${e.message}"))
            }
        }

        post("/auth/login") {
            try {
                val req = call.receive<LoginRequest>()
                val user = transaction {
                    Users.selectAll()
                        .where { (Users.email eq req.email) and (Users.passwordHash eq req.password) }
                        .map {
                            UserDTO(it[Users.userId].toString(), it[Users.email], it[Users.fullName], it[Users.role])
                        }
                        .singleOrNull()
                }

                if (user != null) {
                    val token = UUID.randomUUID().toString()
                    call.respond(LoginResponse(token, user))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Wrong credentials"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        // --- ANDROID SYNC ---
        post("/api/devices/sync") {
            try {
                val req = call.receive<SyncAppsRequest>()
                println("Received Sync from ${req.deviceId}: ${req.installedApps.size} apps")
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        get("/api/users/{userId}/groups") {
            try {
                val userIdStr = call.parameters["userId"]
                println("DEBUG: User $userIdStr calling /groups")
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val userUuid = UUID.fromString(userIdStr)
                val groups = transaction {
                    (Groups innerJoin GroupMembers)
                        .select(Groups.groupId, Groups.name, Groups.joinCode, GroupMembers.role)
                        .where { GroupMembers.userId eq userUuid }
                        .map {
                            GroupDTO(
                                groupId = it[Groups.groupId].toString(),
                                groupName = it[Groups.name],
                                role = it[GroupMembers.role]
                            )
                        }
                }
                call.respond(groups)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/api/groups/{groupId}/rules") {
            try {
                val gIdStr = call.parameters["groupId"]
                println("DEBUG: Asking rules for group $gIdStr")
                val rules = transaction {
                    GroupRules.selectAll()
                        .where { GroupRules.groupId eq UUID.fromString(gIdStr) }
                        .map {
                            GroupRuleDTO(
                                groupId = gIdStr ?: "",
                                packageName = it[GroupRules.packageName],
                                isBlocked = it[GroupRules.isBlocked],
                                startTime = it[GroupRules.startTime],
                                endTime = it[GroupRules.endTime]
                            )
                        }
                }
                call.respond(rules)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // --- GLOBAL RULES (Code cũ của bạn) ---
        get("/rules") {
            try {
                val rules = transaction {
                    UsageLimits.selectAll().map { row ->
                        BlockRuleDTO(
                            packageName = row[UsageLimits.packageName],
                            isBlocked = row[UsageLimits.isBlocked],
                            limitMinutes = row[UsageLimits.dailyLimitMinutes],
                            startTime = row[UsageLimits.startTime],
                            endTime = row[UsageLimits.endTime],
                            latitude = row[UsageLimits.latitude],
                            longitude = row[UsageLimits.longitude],
                            radius = row[UsageLimits.radius]
                        )
                    }
                }
                call.respond(rules)
            } catch (e: Exception) {
                call.respondText("Server error: ${e.message}")
            }
        }

        post("/rules") {
            try {
                val rule = call.receive<BlockRuleDTO>()
                transaction {
                    val existing = UsageLimits.selectAll().where { UsageLimits.packageName eq rule.packageName }.singleOrNull()
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
                call.respond(mapOf("status" to "success"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // --- GROUP MANAGEMENT (Code cũ đầy đủ) ---
        post("/groups/create") {
            val req = call.receive<CreateGroupRequest>()
            val newJoinCode = (1..6).map { ('A'..'Z').random() }.joinToString("")
            val adminUuid = UUID.fromString(req.adminId)

            val result = transaction {
                val gId = Groups.insert {
                    it[name] = req.name
                    it[createdBy] = adminUuid
                    it[joinCode] = newJoinCode
                } get Groups.groupId

                GroupMembers.insert {
                    it[groupId] = gId
                    it[userId] = adminUuid
                    it[role] = "ADMIN"
                    it[isGroupAdmin] = true
                }
                gId
            }
            call.respond(CreateGroupResponse("success", "Created", result.toString(), newJoinCode))
        }

        post("/groups/join") {
            try {
                val req = call.receive<JoinGroupRequest>()
                val userUuid = UUID.fromString(req.userId)
                val msg = transaction {
                    val grp = Groups.selectAll().where { Groups.joinCode eq req.joinCode }.singleOrNull()
                    if (grp == null) return@transaction "Group not found"
                    val gId = grp[Groups.groupId]
                    val exists = GroupMembers.selectAll().where { (GroupMembers.groupId eq gId) and (GroupMembers.userId eq userUuid) }.count() > 0
                    if (exists) return@transaction "Already joined"

                    GroupMembers.insert {
                        it[groupId] = gId
                        it[userId] = userUuid
                        it[role] = "MEMBER"
                    }
                    "Success"
                }
                if (msg == "Success") call.respond(mapOf("status" to "success"))
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to msg))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/groups/{id}/members") {
            try {
                val groupId = call.parameters["id"]
                val members = transaction {
                    (GroupMembers innerJoin Users)
                        .select(Users.userId, Users.fullName, Users.email, GroupMembers.role)
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
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/groups/leave") {
            try {
                val req = call.receive<LeaveGroupRequest>()
                transaction {
                    GroupMembers.deleteWhere {
                        (GroupMembers.groupId eq UUID.fromString(req.groupId)) and (GroupMembers.userId eq UUID.fromString(req.userId))
                    }
                }
                call.respond(mapOf("status" to "success"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/groups/remove") {
            try {
                val req = call.receive<RemoveMemberRequest>()
                val gId = UUID.fromString(req.groupId)
                val adminUuid = UUID.fromString(req.adminId)
                val targetUuid = UUID.fromString(req.targetUserId)

                val result = transaction {
                    val role = GroupMembers.select(GroupMembers.role)
                        .where { (GroupMembers.groupId eq gId) and (GroupMembers.userId eq adminUuid) }
                        .map { it[GroupMembers.role] }.singleOrNull()

                    if (role != "ADMIN") return@transaction "Permission Denied"

                    GroupMembers.deleteWhere { (GroupMembers.groupId eq gId) and (GroupMembers.userId eq targetUuid) }
                    "Success"
                }
                call.respond(mapOf("status" to if(result == "Success") "success" else "error", "message" to result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        // Admin tạo luật chặn
        post("/groups/rules") {
            val req = call.receive<GroupRuleDTO>()
            transaction {
                val gId = UUID.fromString(req.groupId)
                val existing = GroupRules.selectAll().where { (GroupRules.groupId eq gId) and (GroupRules.packageName eq req.packageName) }.count()
                if (existing > 0) {
                    GroupRules.update({ (GroupRules.groupId eq gId) and (GroupRules.packageName eq req.packageName) }) {
                        it[isBlocked] = req.isBlocked
                        it[startTime] = req.startTime
                        it[endTime] = req.endTime
                    }
                } else {
                    GroupRules.insert {
                        it[groupId] = gId
                        it[packageName] = req.packageName
                        it[isBlocked] = req.isBlocked
                        it[startTime] = req.startTime
                        it[endTime] = req.endTime
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}