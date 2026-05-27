// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
import me.modmuss50.mpp.platforms.curseforge.CurseforgeDependencyContainer
import me.modmuss50.mpp.platforms.modrinth.ModrinthDependencyContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import coffee.axle.blahaj.BlahajBuild
import coffee.axle.blahaj.api.ModData
import coffee.axle.blahaj.setup.*

open class BlahajSettings {
    public lateinit var build : BlahajBuild
    public lateinit var mod : ModData
    public lateinit var loader : String
    public lateinit var mc : String
    public lateinit var deps: DependencyHandler
    public var curseforge: CurseforgeDependencyContainer? = null
    public var modrinth: ModrinthDependencyContainer? = null

    public var blahajDependencies : MutableList<BlahajDependency> = mutableListOf()
    public var publishCallbacks : MutableList<() -> Unit?> = mutableListOf()

    public var isConfigured: Boolean = false
    public var customConfigure:  (BlahajSettings.() -> Unit)? = null

    fun addMod(modID: String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        blahajDependencies.add(dep)
        return dep
    }

    fun addMod(modID: String, version : String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        dep.setVersion(version)
        blahajDependencies.add(dep)
        return dep
    }

    fun addRequiredMod(modID: String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        dep.required()
        blahajDependencies.add(dep)
        return dep
    }

    fun addRequiredMod(modID: String, version : String) : BlahajDependency {
        val dep = BlahajDependency(this, modID)
        dep.setVersion(version)
        dep.required()
        blahajDependencies.add(dep)
        return dep
    }

    fun modrinth(name: String, dep: Any?) = "maven.modrinth:$name:$dep"

    fun txnilib(version: String) {
        deps.modImplementation(modloaderRequired("toni.%s:${mod.loader}-${mod.mcVersion}:%s", "txnilib", version))
    }

    fun mocha(version: String, include: Boolean = false) {
        build.project.extensions.extraProperties["options.mocha"] = "true"
        build.project.extensions.extraProperties["options.mocha_version"] = version
        build.project.extensions.extraProperties["options.mocha_include"] = include.toString()
    }

    fun latte(version: String, include: Boolean = false) {
        build.project.extensions.extraProperties["options.latte"] = "true"
        build.project.extensions.extraProperties["options.latte_version"] = version
        build.project.extensions.extraProperties["options.latte_include"] = include.toString()
    }

    fun sodium(version: String? = null) {
        val v = version ?: build.getVersion("deps.sodium")?.toString()?.takeIf { it.isNotEmpty() }
            ?: error("[Blahaj] No sodium version found for $mc. Pass an explicit version.")
        if (isNewSodiumMc(mc)) {
            // 0.8.x+ on CaffeineMC's immutable maven
            deps.modCompileOnlyNonTransitive("net.caffeinemc:sodium-fabric:$v")
        } else {
            // pre-0.8 — not on CaffeineMC maven, use Modrinth with raw version_number
            deps.modCompileOnlyNonTransitive("maven.modrinth:sodium:$v")
        }
    }

    fun iris(version: String? = null) {
        val v = version ?: build.getVersion("deps.iris")?.toString()?.takeIf { it.isNotEmpty() }
            ?: error("[Blahaj] No iris version found for ${mod.mcVersion}. Pass an explicit version: iris(\"mc1.21.11-1.8.1-fabric\")")
        deps.modCompileOnly("maven.modrinth:iris:$v")
    }

    fun devauth(version: String? = null) {
        val v = version ?: build.getVersion("deps.devauth")?.toString()?.takeIf { it.isNotEmpty() }
            ?: error("[Blahaj] No devauth version found. Pass an explicit version: devauth(\"1.2.2\")")
        deps.modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:$v")
    }

    fun mixinExtras(version: String? = null) {
        val v = version ?: build.getVersion("deps.mixinextras")?.toString()?.takeIf { it.isNotEmpty() }
            ?: error("[Blahaj] No mixinextras version found. Pass an explicit version: mixinExtras(\"0.5.4\")")
        deps.annotationProcessor("io.github.llamalad7:mixinextras-fabric:$v")
        deps.implementation("io.github.llamalad7:mixinextras-fabric:$v")
        deps.include("io.github.llamalad7:mixinextras-fabric:$v")
    }

    fun hypixel(modrinthFabricVersion: String? = null, modApiVersion: String? = null) {
        val modrinth = modrinthFabricVersion
            ?: build.getVersion("deps.hypixel")?.toString()?.takeIf { it.isNotEmpty() }
            ?: error("[Blahaj] No hypixel-mod-api version found for $mc. Pass an explicit version.")
        val api = modApiVersion ?: modrinth.substringBefore("+build")
        deps.modImplementation("net.hypixel:mod-api:$api")
        deps.include("maven.modrinth:hypixel-mod-api:$modrinth")
    }

    fun forgeConfig() {
        if (!mod.isFabric)
            return

        deps.include(
            when (mod.mcVersion) {
                "1.19.2" -> deps.modApi("net.minecraftforge:forgeconfigapiport-fabric:${build.getVersion("deps.forgeconfigapi")}")
                else -> deps.modApi("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${build.getVersion("deps.forgeconfigapi")}")
            }!!
        )
    }

    fun modloaderRequired(format : String, modID: String, version: String) : String {
        build.mod.depends.putIfAbsent(modID, version)
        return String.format(format, modID, version)
    }

    fun modloaderRequired(modID : String, version: String) {
        build.mod.depends.putIfAbsent(modID, version)
    }

    fun modloaderRequired(modID : String) {
        build.mod.depends.putIfAbsent(modID, "*")
    }

    fun requiredWith(vararg slugs : String) {
        requiredWithCurseforge(*slugs)
        requiredWithModrinth(*slugs)
    }

    fun requiredWithCurseforge(vararg slugs : String)  {
        publishCallbacks.add { curseforge?.requires(*slugs) }
    }

    fun requiredWithModrinth(vararg slugs : String) {
        publishCallbacks.add { modrinth?.requires(*slugs)}
    }

    fun incompatibleWith(vararg slugs : String) {
        incompatibleWithCurseforge(*slugs)
        incompatibleWithModrinth(*slugs)
    }

    fun incompatibleWithCurseforge(vararg slugs : String)  {
        publishCallbacks.add { curseforge?.incompatible(*slugs) }
    }

    fun incompatibleWithModrinth(vararg slugs : String) {
        publishCallbacks.add { modrinth?.incompatible(*slugs)}
    }


    private fun isNewSodiumMc(mcVersion: String): Boolean {
        val parts = mcVersion.split(".")
        val major = parts[0].toIntOrNull() ?: return false
        if (major > 1) return true // 26.x and beyond
        if (parts.size < 3) return false
        val minor = parts[1].toIntOrNull() ?: return false
        val patch = parts[2].toIntOrNull() ?: return false
        return minor > 21 || (minor == 21 && patch >= 11)
    }

    open fun configure() {}
    open fun addGlobal() {}
    open fun addFabric() {}
    open fun addForge() {}
    open fun addNeo() {}
}

class BlahajDependency(val parent: BlahajSettings, val modID: String) {
    private var version : String? = null
    private var versions : MutableMap<String, String> = mutableMapOf()
    private var binding : ((id: String, version: String) -> Dependency?)? = null
    public var publishCallbacks : MutableList<() -> Unit?> = mutableListOf()

    fun internalApply() {
        val target = version ?: versions.getOrDefault(parent.build.projectName, null);
        if (target == null) {
            throw Exception("Could not find version ${parent.build.projectName} for mod $modID among configured platforms!")
        }

        binding?.invoke(modID, target)
    }

    fun bind(function: (id: String, version: String) -> Dependency? ) : BlahajDependency
    {
        binding = function
        return this
    }

    fun modrinth(slug: String? = null) : BlahajDependency {
        binding = { id, version -> parent.deps.modImplementation(parent.modrinth(slug ?: id, version)) }
        return this
    }

    fun modrinthCompile(slug: String? = null) : BlahajDependency {
        binding = { id, version -> parent.deps.modCompileOnly(parent.modrinth(slug ?: id, version)) }
        return this
    }

    fun modrinthRuntime(slug: String? = null) : BlahajDependency {
        binding = { id, version -> parent.deps.modRuntimeOnly(parent.modrinth(slug ?: id, version)) }
        return this
    }

    fun modImplementation(format: (id: String, version: String) -> String ) : BlahajDependency {
        binding = { id, version -> parent.deps.modImplementation(format(id, version)) }
        return this
    }

    fun setVersion(version: String) : BlahajDependency {
        this.version = version
        return this
    }

    fun modloaderRequired(vers: String = "*") : BlahajDependency {
        parent.modloaderRequired(modID, vers)
        return this
    }

    fun modloaderRequiredFabric(vers: String = "*") : BlahajDependency {
        if (parent.mod.isFabric)
            parent.modloaderRequired(modID, vers)

        return this
    }

    fun modloaderRequiredForge(vers: String = "*") : BlahajDependency {
        if (parent.mod.isForge)
            parent.modloaderRequired(modID, vers)

        return this
    }

    fun modloaderRequiredNeo(vers: String = "*") : BlahajDependency {
        if (parent.mod.isNeo)
            parent.modloaderRequired(modID, vers)

        return this
    }

    fun addPlatform(platform: String, version: String) : BlahajDependency {
        versions[platform] = version
        return this
    }

    fun addPlatform(platform: String, version: String, function: BlahajDependency.(String) -> Unit = {}) : BlahajDependency {
        addPlatform(platform, version)

        if (parent.build.projectName == version)
            function(version)

        return this
    }


    fun required()  : BlahajDependency {
        requiredCurseforgeAndModrinth(modID)
        modloaderRequired()
        return this
    }


    fun requiredCurseforgeAndModrinth()  : BlahajDependency {
        requiredCurseforge(modID)
        requiredModrinth(modID)
        return this
    }

    fun requiredCurseforgeAndModrinth(vararg slugs : String)  : BlahajDependency {
        requiredCurseforge(*slugs)
        requiredModrinth(*slugs)
        return this
    }

    fun requiredCurseforge(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add { parent.curseforge?.requires(*slugs) }
        return this
    }

    fun requiredModrinth(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add { parent.modrinth?.requires(*slugs)}
        return this
    }


    fun optionalCurseforgeAndModrinth()  : BlahajDependency {
        optionalCurseforge(modID)
        optionalModrinth(modID)
        return this
    }

    fun optionalCurseforgeAndModrinth(vararg slugs : String)  : BlahajDependency {
        optionalCurseforge(*slugs)
        optionalModrinth(*slugs)
        return this
    }

    fun optionalCurseforge(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add { parent.curseforge?.optional(*slugs) }
        return this
    }

    fun optionalModrinth(vararg slugs : String)  : BlahajDependency {
        publishCallbacks.add {  parent.modrinth?.optional(*slugs) }
        return this
    }
}