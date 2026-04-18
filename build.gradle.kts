@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.HttpURLConnection
import java.net.URI

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin") version "0.8.1" apply false
    id("systems.manifold.manifold-gradle-plugin") version "0.0.2-alpha" apply false
}

group = "coffee.axle.blahaj"
version = "3.0.4"

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
    implementation("com.google.code.gson:gson:2.12.1")
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

tasks.register("updateVersions") {
    group = "blahaj"
    description = "Fetch latest upstream versions and update VersionInfo.kt defaults"

    doLast {
        val versionInfoFile = file("src/main/kotlin/coffee/axle/blahaj/data/VersionInfo.kt")
        var content = versionInfoFile.readText()

        fun fetch(url: String): String? {
            return try {
                val conn = URI(url).toURL().openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "Blahaj-Gradle-Plugin/3.0")
                if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
            } catch (e: Exception) {
                logger.warn("[Blahaj] Fetch failed for $url: ${e.message}")
                null
            }
        }

        fun parseXmlTag(xml: String, tag: String): String? {
            val start = xml.indexOf("<$tag>")
            if (start == -1) return null
            val end = xml.indexOf("</$tag>", start)
            if (end == -1) return null
            return xml.substring(start + tag.length + 2, end).trim()
        }

        fun updateEntry(sectionKey: String, versionKey: String, newValue: String) {
            val sectionHeader = """"$sectionKey" to mutableMapOf("""
            val sectionStart = content.indexOf(sectionHeader)
            if (sectionStart == -1) return

            val blockStart = sectionStart + sectionHeader.length
            val blockEnd = content.indexOf(")", blockStart)
            if (blockEnd == -1) return

            val block = content.substring(blockStart, blockEnd)
            val escaped = Regex.escape(versionKey)
            val pattern = Regex("""("$escaped"\s*to\s*)"[^"]*"""")
            if (pattern.containsMatchIn(block)) {
                val updated = pattern.replace(block) { "${it.groupValues[1]}\"$newValue\"" }
                content = content.substring(0, blockStart) + updated + content.substring(blockEnd)
                logger.lifecycle("  Updated $sectionKey[$versionKey] -> $newValue")
            }
        }

        val gson = com.google.gson.Gson()

        logger.lifecycle("[Blahaj] Fetching latest FLK...")
        fetch("https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/maven-metadata.xml")?.let { xml ->
            val latest = parseXmlTag(xml, "release") ?: parseXmlTag(xml, "latest")
            if (latest != null) {
                logger.lifecycle("  Latest FLK: $latest")
                updateEntry("deps.flk", "*", latest)
            }
        }

        logger.lifecycle("[Blahaj] Fetching latest FAPI versions...")
        fetch("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml")?.let { xml ->
            val versions = mutableListOf<String>()
            var searchFrom = 0
            while (true) {
                val start = xml.indexOf("<version>", searchFrom)
                if (start == -1) break
                val end = xml.indexOf("</version>", start)
                if (end == -1) break
                versions.add(xml.substring(start + 9, end).trim())
                searchFrom = end + 10
            }

            val mcToFapi = mutableMapOf<String, String>()
            for (v in versions) {
                if (!v.contains("+") || v.contains("+build")) continue
                val mc = v.substringAfter("+")
                if (mc.isBlank()) continue
                mcToFapi[mc] = v
            }

            for ((mc, fapi) in mcToFapi) {
                updateEntry("deps.fapi", "$mc-fabric", fapi)
            }
        }

        logger.lifecycle("[Blahaj] Fetching latest ModMenu versions...")
        fetch("https://api.modrinth.com/v2/project/mOgUt4GM/version")?.let { json ->
            val array = gson.fromJson(json, com.google.gson.JsonArray::class.java)
            val mcToMod = mutableMapOf<String, String>()
            for (element in array) {
                val obj = element.asJsonObject
                val versionNumber = obj.get("version_number")?.asString ?: continue
                val versionType = obj.get("version_type")?.asString ?: "release"
                val gameVersions = obj.getAsJsonArray("game_versions") ?: continue
                for (gv in gameVersions) {
                    val mc = gv.asString
                    val existing = mcToMod[mc]
                    if (existing == null || (versionType == "release" && (existing.contains("alpha") || existing.contains("beta")))) {
                        mcToMod[mc] = versionNumber
                    }
                }
            }
            for ((mc, modmenu) in mcToMod) {
                updateEntry("deps.modmenu", "$mc-fabric", modmenu)
            }
        }

        logger.lifecycle("[Blahaj] Fetching latest NeoForge versions...")
        fetch("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge")?.let { json ->
            val obj = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val versions = obj.getAsJsonArray("versions") ?: return@let
            val mcToNeo = mutableMapOf<String, String>()
            for (element in versions) {
                val v = element.asString
                val clean = v.removeSuffix("-beta")
                val parts = clean.split(".")
                if (parts.size < 3) continue
                val major = parts[0].toIntOrNull() ?: continue
                val mc = when {
                    major >= 26 -> if (parts.size >= 4) parts.subList(0, 3).joinToString(".") else "${parts[0]}.${parts[1]}"
                    major >= 20 -> {
                        val mcMinor = parts.getOrNull(1)?.toIntOrNull() ?: continue
                        if (mcMinor == 0) continue
                        "1.$major.$mcMinor"
                    }
                    else -> continue
                }
                mcToNeo[mc] = v
            }
            for ((mc, neo) in mcToNeo) {
                updateEntry("deps.fml", "$mc-neoforge", neo)
            }
        }

        versionInfoFile.writeText(content)
        logger.lifecycle("[Blahaj] VersionInfo.kt updated.")
    }
}