plugins {
    // 1. Dùng Kotlin bản mới nhất để tránh lỗi version
    kotlin("jvm") version "2.0.21"

    // 2. Nâng lên Ktor 3.0.1 (Bản này fix lỗi 'convention' bạn vừa gặp)
    id("io.ktor.plugin") version "3.0.1"

    // 3. Serialization khớp version với Kotlin
    kotlin("plugin.serialization") version "2.0.21"
}

group = "vn.edu.usth"
version = "0.0.1"

application {
    mainClass.set("vn.edu.usth.blockappserver.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // KTOR SERVER 3.0.1
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.jetbrains.exposed:exposed-java-time:0.56.0")

    // DATABASE (PostgreSQL + Exposed)
    implementation("org.jetbrains.exposed:exposed-core:0.56.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.56.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.56.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.56.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.zaxxer:HikariCP:5.1.0")

}