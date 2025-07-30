plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

application {
    mainClass.set("io.modelcontextprotocol.sample.server.MainKt")
}

group = "org.example"
version = "0.1.0"

val mcpVersion = "0.6.0"
val slf4jVersion = "2.0.9"
val ktorVersion = "3.1.1"
val googleCalendarVersion = "v3-rev20220715-2.0.0"
val googleAuthVersion = "1.27.0"

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
    implementation("org.slf4j:slf4j-nop:$slf4jVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    
    // Ktor server dependencies for HTTP transport
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    
    // Google Calendar API dependencies
    implementation("com.google.apis:google-api-services-calendar:$googleCalendarVersion")
    implementation("com.google.auth:google-auth-library-oauth2-http:$googleAuthVersion")
    implementation("com.google.http-client:google-http-client-jackson2:1.44.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.oauth-client:google-oauth-client-java6:1.34.1")
    
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("auth") {
    group = "application"
    description = "Set up Google Calendar OAuth2 authentication"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.modelcontextprotocol.sample.server.AuthSetupKt")
}

tasks.register<JavaExec>("runHttp") {
    group = "application"
    description = "Run Google Calendar MCP Server with HTTP transport"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.modelcontextprotocol.sample.server.MainKt")
    systemProperty("server.mode", "http")
}

tasks.register<JavaExec>("runMcp") {
    group = "application"
    description = "Run Google Calendar MCP Server with MCP protocol"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.modelcontextprotocol.sample.server.MainKt")
    systemProperty("server.mode", "mcp")
}

kotlin {
    jvmToolchain(17)
}