@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin") version "0.8.1" apply false
    id("systems.manifold.manifold-gradle-plugin") version "0.0.2-alpha" apply false
}

group = "coffee.axle.blahaj"
version = "3.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev")
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.kikugie.dev/snapshots")
    maven("https://maven.kikugie.dev/releases")
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.shedaniel.me/")
    maven("https://www.cursemaven.com")
    maven("https://api.modrinth.com/maven")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("me.modmuss50:mod-publish-plugin:0.8.1")
    implementation("dev.architectury.loom:dev.architectury.loom.gradle.plugin:1.14.473") {
        exclude("com.mojang")
    }
    implementation("systems.manifold:manifold-gradle-plugin:0.0.2-alpha")
    implementation("dev.kikugie:stonecutter:0.9.1")
}

gradlePlugin {
    website = "https://github.com/axlecoffee/blahaj"
    vcsUrl = "https://github.com/axlecoffee/blahaj"

    plugins {
        create("blahaj") {
            id = "coffee.axle.blahaj"
            implementationClass = "coffee.axle.blahaj.BlahajPlugin"
            displayName = "Blahaj"
            description = "Minecraft multiversion plugin, with full mod project management, built on Stonecutter"
            tags = setOf("minecraft", "mods")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "blahaj"
            version = project.version.toString()
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.axle.coffee/releases")
            credentials {
                username = findProperty("MAVEN_USERNAME") as String? ?: System.getenv("MAVEN_USERNAME")
                password = findProperty("MAVEN_PASSWORD") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}