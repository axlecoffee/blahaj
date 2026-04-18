// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj.setup

import coffee.axle.blahaj.tasks.RenameExampleMod
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import coffee.axle.blahaj.BlahajBuild
import coffee.axle.blahaj.tasks.ManifoldMC

fun tasks(template : BlahajBuild) : TaskContainer.() -> Unit = { template.apply {
    named<ProcessResources>("processResources") {
        val map = mapOf(
            "modversion" to mod.version,
            "mcVersion" to mod.mcVersion,
            "mc" to mod.mcDep,
            "id" to mod.id,
            "group" to mod.group,
            "author" to mod.author,
            "namespace" to mod.namespace,
            "description" to mod.description,
            "discord" to mod.discord,
            "name" to mod.name,
            "license" to mod.license,
            "github" to mod.github,
            "display_name" to mod.displayName,
            "depends" to mod.getDependsBlock(),
            "mixinid" to if (template.config.platformSpecificMixins) "${mod.id}_${mod.loader}_${mod.mcVersion}" else if (template.config.versionedMixins) "${mod.id}_${mod.mcVersion}" else mod.id
        )

        map.forEach { (key, value) -> inputs.property(key, value) }

        filesMatching("fabric.mod.json") { expand(map) }
        filesMatching("META-INF/mods.toml") { expand(map) }
        filesMatching("META-INF/neoforge.mods.toml") { expand(map) }

        if (!mod.isFabric)
            exclude("fabric.mod.json")
    }

    // Clean build libs directory because for some reason Arch is stupid (go figure)
    project.tasks.register<DefaultTask>("cleanJar") {
        val libsDir = project.layout.buildDirectory.dir("libs")
        doLast {
            val libs = libsDir.get().asFile
            if (libs.exists())
                libs.deleteRecursively()
        }
    }

    project.tasks.named("jar") {
        dependsOn("cleanJar")
    }

    project.tasks.register("setupManifoldPreprocessors") {
        group = "build"
        ManifoldMC.setupPreprocessor(ArrayList(), mod.loader, project.projectDir, mod.mcVersion, sc.active?.project == sc.current.project, true)
    }

    if (project.tasks.findByName("remapJar") != null) {
        named<RemapJarTask>("remapJar") {
            if (mod.isNeo) {
                atAccessWideners.add("${mod.id}.accesswidener")
            }
        }
    }

    withType<Tar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    withType<Zip> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    named<JavaCompile>("compileJava") {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xplugin:Manifold")

        // modify the JavaCompile task and inject our auto-generated Manifold symbols
        if(!this.name.startsWith("_")) { // check the name, so we don't inject into Forge internal compilation
            ManifoldMC.setupPreprocessor(
                options.compilerArgs,
                mod.loader,
                project.projectDir,
                mod.mcVersion,
                sc.active?.project == sc.current.project,
                false
            )
        }
    }

    register<RenameExampleMod>("renameExampleMod", project.rootDir, mod.id, mod.name, mod.displayName, mod.namespace, mod.group).configure {
        group = "blahaj"
        description = "Renames the example mod to match the mod ID, name, and display name in gradle.properties"
    }


    register ("cleanModrinth") {
        group = "build"

        val modsDir = File(modrinthDir, "${modrinthPath}/mods/")
        val serverModsDir = File(modrinthDir, "${modrinthPath}/server/mods/")

        File(modrinthDir, "${modrinthPath}/server/").mkdir()
        serverModsDir.mkdir()

        modsDir.walk().filter { it.name.contains(mod.id) }.forEach { it.delete() }
        serverModsDir.walk().filter { it.name.contains(mod.id) }.forEach { it.delete() }
    }

    val outputJar = if (project.tasks.findByName("remapJar") != null)
        named<RemapJarTask>("remapJar").get().archiveFile
    else
        named<org.gradle.jvm.tasks.Jar>("jar").get().archiveFile

    register("buildAndCopyToModrinth") {
        group = "build"

        File(modrinthDir, "${modrinthPath}/server/").mkdir()
        File(modrinthDir, "${modrinthPath}/server/mods/").mkdir()

        val outputDirs = listOf(
            File(modrinthDir, "${modrinthPath}/mods/"),
            File(modrinthDir, "${modrinthPath}/server/mods/")
        )

        doLast {
            outputDirs.forEach { dir ->
                project.copy {
                    from(outputJar)
                    into(dir)
                }
            }
        }

        dependsOn("build")
        dependsOn("cleanModrinth")
    }

    val buildAndCollect = register<Copy>("buildAndCollect") {
        group = "build"
        from(outputJar)
        into(project.rootProject.layout.buildDirectory.file("libs/${mod.version}"))
        dependsOn("build")
    }

    val buildAndCollectLatest = register<Copy>("buildAndCollectLatest") {
        group = "build"
        from(outputJar)
        into(project.rootProject.layout.buildDirectory.file("libs/latest"))
        dependsOn("buildAndCollect")
    }

    if (sc.current.isActive) {
        project.rootProject.tasks.register("buildActive") {
            group = "project"
            dependsOn(buildAndCollectLatest)
        }

        project.rootProject.tasks.register("runActive") {
            group = "project"
            dependsOn(named("runClient"))
        }
    }
}}