buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.21"))
    }
}

plugins {
    kotlin("jvm") version "1.4.21"
}

group = "world.gregs.rs2.file"
group = "1.0-SNAPSHOP"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.displee:rs-cache-library:6.7")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger-jvm:1.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("io.mockk:mockk:1.10.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}