plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.jetbrains:annotations:26.0.2-1")
    implementation("joda-time:joda-time:2.14.0")

    implementation("org.apache.logging.log4j:log4j-1.2-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-core:2.25.2")

    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("it.unimi.dsi:fastutil-core:8.5.18")
    implementation("de.siegmar:fastcsv:4.1.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}