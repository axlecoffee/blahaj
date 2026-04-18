// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj

import BlahajSettings
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.ModPublishExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import systems.manifold.ManifoldExtension
import coffee.axle.blahaj.api.BlahajConfigContainer
import coffee.axle.blahaj.api.ModData
import coffee.axle.blahaj.data.VersionInfo
import coffee.axle.blahaj.setup.dependencies
import coffee.axle.blahaj.setup.loomSetup
import coffee.axle.blahaj.setup.mavenPublish
import coffee.axle.blahaj.setup.tasks
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BlahajBuild internal constructor(val project: Project)  {
    lateinit var loom: LoomGradleExtensionAPI
    lateinit var projectName : String
    lateinit var loader : String
    lateinit var sc : StonecutterBuildExtension
    lateinit var mod : ModData

    lateinit var modrinthPath: String
    lateinit var modrinthDir: File

    private var isInitialized = false

    var settings : BlahajSettings = BlahajSettings()
    val config: BlahajConfigContainer = BlahajConfigContainer()

    fun setting(prop : String) : Boolean = project.properties.containsKey(prop) && project.properties[prop] == "true"
    fun property(prop : String) : Any? = if (project.properties.containsKey(prop)) project.properties[prop] else null
    fun getVersion(prop : String) : Any? = VersionInfo.getVersion(project.properties, prop, projectName)

    fun initInternal() {
        if (!isInitialized) {
            init()
        }
    }

    fun config(configure: BlahajConfigContainer.() -> Unit) {
        config.apply(configure)
    }

    fun setup(configure: BlahajSettings.() -> Unit) {
        settings.customConfigure = configure
        initInternal()
    }

    fun init() {
        System.out.println("Blahaj Initializing build for project ${project.name}")
        val stonecutter = project.extensions.findByType<StonecutterBuildExtension>()
        if (stonecutter == null) {
            System.out.println("[Blahaj] Could not find Stonecutter for project ${project.name}")
            return
        }

        sc = stonecutter

        isInitialized = true
        loom = project.extensions.findByType<LoomGradleExtensionAPI>() ?: throw Exception("Could not find Loom!")

        loader = if (project.extensions.extraProperties.has("loom.platform")) {
            project.extensions.extraProperties["loom.platform"].toString().lowercase()
        } else if (loom.platform != null) {
            loom.platform.get().name.lowercase()
        } else {
            throw Exception("Could not determine loader for project ${project.name}")
        }

        projectName = sc.current.project

        mod = ModData.from(this)

        modrinthDir = File(project.properties["client.modrinth_profiles_dir"].toString())
        modrinthPath = when (loader) {
            "fabric" -> "Fabric ${mod.mcVersion}"
            "neoforge" -> "NeoForge ${mod.mcVersion}"
            "forge" -> "Forge ${mod.mcVersion}"
            else -> ""
        }

        // Versioning Setup
        project.run {
            version = "${mod.version}-${mod.mcVersion}"
            group = mod.group

            val baseExtension = project.extensions.getByType(BasePluginExtension::class.java)
            baseExtension.archivesName.set("${mod.id}-${mod.loader}")
        }

        project.repositories {
            maven("https://www.cursemaven.com")
            maven("https://api.modrinth.com/maven")
            maven("https://thedarkcolour.github.io/KotlinForForge/")
            maven("https://maven.kikugie.dev/releases")
            maven("https://jitpack.io")
            maven("https://maven.neoforged.net/releases/")
            maven("https://maven.terraformersmc.com/releases/")
            maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
            maven("https://maven.parchmentmc.org")
            maven("https://maven.su5ed.dev/releases")
            maven("https://maven.fabricmc.net")
            maven("https://maven.shedaniel.me/")
            maven("https://maven.fallenbreath.me/releases")
        }

        // The manifold Gradle plugin version. Update this if you update your IntelliJ Plugin!
        project.extensions.getByType<ManifoldExtension>().apply { manifoldVersion = "2026.1.6" }


        // Loom config
        loom.apply(loomSetup(this))

        // Dependencies
        DependencyHandlerScope.of(project.dependencies).apply(dependencies(this))

        // Tasks
        project.tasks.apply(tasks(this))


        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        mainSourceSet.resources {
            srcDir("src/main/generated")
            exclude(".cache/")
        }

        sc.apply {
            val j25 = mod.mcVersion.startsWith("26.")
            val j21 = !j25 && eval(mod.mcVersion, ">=1.20.6")
            val javaTarget = when {
                j25 -> 25
                j21 -> 21
                else -> 17
            }
            val javaVersion = JavaVersion.toVersion(javaTarget)
            project.extensions.getByType(JavaPluginExtension::class.java).apply {
                withSourcesJar()
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
                toolchain.languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(javaTarget))
            }
        }

        // this won't let me move it to a different class so fuck it, it goes here
        val noRemap = mod.mcVersion.startsWith("26.")
        project.extensions.getByType<ModPublishExtension>().apply(fun ModPublishExtension.() {
            if (noRemap) {
                file = project.tasks.named("jar", org.gradle.jvm.tasks.Jar::class.java).get().archiveFile
            } else {
                file = project.tasks.named("remapJar", RemapJarTask::class.java).get().archiveFile
                additionalFiles.from(project.tasks.named("remapSourcesJar", RemapSourcesJarTask::class.java).get().archiveFile)
            }
            displayName =
                "${mod.name} ${mod.loader.replaceFirstChar { it.uppercase() }} ${mod.version} for ${mod.mcVersion}"
            version = mod.version
            changelog = project.rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: ""
            type = STABLE
            modLoaders.add(mod.loader)

            val targets = getVersion("mod.mc_targets").toString().split(' ')

            dryRun = project.providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null ||
                    project.providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

            modrinth {
                projectId = property("publish.modrinth").toString()
                version = "${mod.loader}-${mod.mcVersion}-${mod.version}"
                accessToken = project.providers.environmentVariable("MODRINTH_TOKEN")
                targets.forEach(minecraftVersions::add)

                settings.modrinth = this
                if (!settings.isConfigured)
                {
                    if (settings.customConfigure != null) settings.customConfigure?.let { it(settings) } else settings.configure()
                    settings.isConfigured = true
                }

                settings.blahajDependencies.forEach {
                    it.publishCallbacks.forEach { it() }
                }

                if (mod.isFabric)
                    requires("fabric-api")

                if (setting("options.txnilib"))
                    requires("txnilib")
            }

            curseforge {
                projectId = property("publish.curseforge").toString()
                version = "${mod.loader}-${mod.mcVersion}-${mod.version}"
                accessToken = project.providers.environmentVariable("CURSEFORGE_TOKEN")
                targets.forEach(minecraftVersions::add)

                settings.curseforge = this
                if (!settings.isConfigured)
                {
                    if (settings.customConfigure != null) settings.customConfigure?.let { it(settings) } else settings.configure()
                    settings.isConfigured = true
                }

                settings.blahajDependencies.forEach {
                    it.publishCallbacks.forEach { it() }
                }

                if (mod.isFabric)
                    requires("fabric-api")

                if (setting("options.txnilib"))
                    requires("txnilib")
            }
        })

        project.extensions.getByType<PublishingExtension>().apply(mavenPublish(this))
    }



}
