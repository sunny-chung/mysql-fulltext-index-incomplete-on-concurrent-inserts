import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.9.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.mysql:mysql-connector-j:8.0.32")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events = TestLogEvent.values().toSet()
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
kotlin {
    jvmToolchain(17)
}