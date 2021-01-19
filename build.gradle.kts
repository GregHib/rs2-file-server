buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.21"))
        classpath("me.champeau.gradle:jmh-gradle-plugin:0.5.0")
    }
}

plugins {
    kotlin("jvm") version "1.4.21"
    id("me.champeau.gradle.jmh") version "0.5.0"
    kotlin("plugin.allopen") version "1.4.21"
}

group = "world.gregs.rs2.file"
group = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.displee:rs-cache-library:6.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("io.ktor:ktor-server-core:1.5.0")
    implementation("io.ktor:ktor-network:1.5.0")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger-jvm:1.0.2")

    jmh("org.openjdk.jmh:jmh-core:1.21")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.21")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.2")
    testImplementation("io.mockk:mockk:1.10.0")
}

sourceSets {
    named("jmh") {
        compileClasspath += main.get().runtimeClasspath
    }
}

jmh {
    duplicateClassesStrategy = DuplicatesStrategy.WARN
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}