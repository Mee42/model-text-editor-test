plugins {
    java
    kotlin("jvm") version "1.4.21"
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "dev.mee42"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testCompile("junit", "junit", "4.12")
    implementation("com.googlecode.lanterna:lanterna:3.1.1")
}


application {
    mainClassName = "dev.mee42.MainKt"
}