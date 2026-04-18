// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj

import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.settings.tree.TreeBuilder
import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.plugins
import java.io.File

class BlahajPlugin : Plugin<Any> {

    override fun apply(target: Any) {
        when (target) {
            is Settings -> {
                target.extensions.create("blahaj", BlahajSettings::class.java, target)
            }
            is Project -> {
                if (target.isStonecutterController()) {
                    target.extensions.create("blahaj", BlahajController::class.java, target)
                    addStonecutterTasks(target)
                    return
                }

                if (target.rootProject.name == target.name)
                    return

                val subprojectName = target.name
                val platform = if (subprojectName.contains("fabric")) "fabric" else if (subprojectName.contains("neoforge")) "neoforge" else "forge"

                target.extensions.extraProperties["loom.platform"] = platform

                val mcVersion = subprojectName.substringBefore('-')
                val isUnobfuscated = mcVersion.startsWith("26.")
                val loomPlugin = if (isUnobfuscated && platform != "fabric") {
                    "dev.architectury.loom-no-remap"
                } else {
                    "dev.architectury.loom"
                }

                with(target.plugins) {
                    apply("maven-publish")
                    apply("application")
                    apply("org.jetbrains.kotlin.jvm")
                    apply("org.jetbrains.kotlin.plugin.serialization")
                    apply(loomPlugin)
                    apply("me.modmuss50.mod-publish-plugin")
                    apply("systems.manifold.manifold-gradle-plugin")
                }

                target.extensions.create("blahaj", BlahajBuild::class.java, target)
            }
            else -> {
                throw IllegalArgumentException("Unsupported target type: ${target::class.java}")
            }
        }

    }

    fun addStonecutterTasks(target: Project) {
        val sc = target.extensions.findByType<StonecutterControllerExtension>()!!

        target.tasks.register("buildAll") {
            group = "blahaj"
            dependsOn(sc.tasks.named("buildAndCollectLatest"))
        }

        target.tasks.register("copyToModrinthLauncher") {
            group = "blahaj"
            dependsOn(sc.tasks.named("buildAndCopyToModrinth"))
        }

        target.tasks.register("publishAllRelease") {
            group = "blahaj"
            dependsOn(sc.tasks.named("publishMods"))
        }

        target.tasks.register("publishAllMaven") {
            group = "blahaj"
            dependsOn(sc.tasks.named("publish"))
        }

        target.tasks.register("chiseledBuild") {
            group = "project"
            dependsOn(sc.tasks.named("build"))
        }


        target.tasks.register("bumpVersionAndChangelog") {
            group = "blahaj"
            doLast {
                val gradleProperties = target.file("gradle.properties")
                val gradlePropertiesContent = gradleProperties.readText()

                val versionRegex = Regex("""mod\.version=(\d+)\.(\d+)\.(\d+)""")
                val matchResult = versionRegex.find(gradlePropertiesContent)
                if (matchResult == null) {
                    println("Error: mod.version not found in gradle.properties.")
                    return@doLast
                }

                val (major, minor, patch) = matchResult.destructured

                println("Update type? (major, minor, patch):")
                val updateInput = readlnOrNull() ?: "patch"

                val newVersion = when (updateInput) {
                    "major" -> "${major.toInt() + 1}.$minor.$patch"
                    "minor" -> "$major.${minor.toInt() + 1}.$patch"
                    "patch" -> "$major.$minor.${patch.toInt() + 1}"
                    else -> "$major.$minor.${patch.toInt() + 1}"
                }

                val updatedPropertiesContent = gradlePropertiesContent.replace(
                    versionRegex,
                    "mod.version=$newVersion"
                )

                gradleProperties.writeText(updatedPropertiesContent)

                println("Enter the changelog for version $newVersion (separate entries with semicolons):")
                val changelogInput = readLine() ?: ""
                val changelogEntries = changelogInput.split(";").map { "- ${it.trim()}" }

                val changelogFile = target.file("CHANGELOG.md")
                val changelogContent = changelogFile.takeIf { it.exists() }?.readText() ?: ""

                val newChangelogContent = buildString {
                    append("## $newVersion\n")
                    append(changelogEntries.joinToString("\n"))
                    append("\n\n")
                    append(changelogContent)
                }

                changelogFile.writeText(newChangelogContent)

                println("Version bumped to $newVersion in gradle.properties.")
                println("Changelog updated with the following entries:")
                changelogEntries.forEach { println(it) }
            }
        }

    }

     fun Project.isStonecutterController() = when (buildFile.name) {
         "stonecutter.gradle" -> true
         "stonecutter.gradle.kts" -> true
         else -> false
    }

}


@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BlahajSettings internal constructor(val settings: Settings) {

    fun TreeBuilder.mc(version: String, vararg loaders: String) {
        for (it in loaders) {
            val versStr = "$version-$it"

            val dir = File(settings.rootDir, "versions/$versStr")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val props = File(settings.rootDir, "versions/$versStr/gradle.properties")
            if (!props.exists()){
                props.writeText("loom.platform=$it")
            }

            version(versStr, version)
        }
    }

    fun init(rootProject: ProjectDescriptor, configure: TreeBuilder.() -> Unit) {
        val stonecutter = settings.extensions.findByType<StonecutterSettingsExtension>()!!
        stonecutter.apply {
            kotlinController.set(true)
            centralScript.set("build.gradle.kts")

            create(rootProject) {
                configure()
            }
        }
    }
}

open class BlahajController internal constructor(val project: Project) {

}