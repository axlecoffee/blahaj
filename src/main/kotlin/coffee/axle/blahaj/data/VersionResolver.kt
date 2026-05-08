// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.gradle.api.logging.Logging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
// TODO this is so shit
object VersionResolver {
    private val logger = Logging.getLogger(VersionResolver::class.java)
    private val gson = Gson()

    private val client by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    private val resolvedKeys = mutableSetOf<String>()
    private val fapiCache = mutableMapOf<String, String>()
    private val modMenuCache = mutableMapOf<String, String>()
    private val neoForgeCache = mutableMapOf<String, String>()
    private val sodiumCache = mutableMapOf<String, String>()
    private val irisCache = mutableMapOf<String, String>()
    private var mixinExtrasLatest: String? = null

    fun resolve(propertyKey: String, mcVersion: String, loader: String): String? {
        val cacheKey = "$propertyKey:$mcVersion-$loader"
        if (cacheKey in resolvedKeys) return null

        val result = try {
            when (propertyKey) {
                "deps.fabric_loader" -> if (loader == "fabric") resolveLatestStableFabricLoader() else null
                "deps.fapi" -> if (loader == "fabric") resolveFapiForMc(mcVersion) else null
                "deps.flk" -> resolveLatestFlk()
                "deps.modmenu" -> if (loader == "fabric") resolveModMenuForMc(mcVersion) else null
                "deps.fml" -> when (loader) {
                    "neoforge" -> resolveNeoForgeForMc(mcVersion)
                    else -> null
                }
                "mod.mc_dep" -> generateMcDep(mcVersion, loader)
                "mod.mc_targets" -> mcVersion
                "deps.sodium" -> if (loader == "fabric") resolveSodiumForMc(mcVersion) else null
                "deps.iris" -> if (loader == "fabric") resolveIrisForMc(mcVersion) else null
                "deps.mixinextras" -> resolveLatestMixinExtras()
                "deps.hypixel" -> if (loader == "fabric") resolveHypixelForMc(mcVersion) else null
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("[Blahaj] Failed to resolve $propertyKey for $mcVersion-$loader: ${e.message}")
            null
        }

        resolvedKeys.add(cacheKey)
        return result
    }

    private fun resolveLatestStableFabricLoader(): String? {
        val json = fetch("https://meta.fabricmc.net/v2/versions/loader") ?: return null
        val array = gson.fromJson(json, JsonArray::class.java)
        for (element in array) {
            val obj = element.asJsonObject
            if (obj.get("stable")?.asBoolean == true) {
                return obj.get("version")?.asString
            }
        }
        return null
    }

    private fun resolveLatestFlk(): String? {
        val xml = fetch("https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/maven-metadata.xml") ?: return null
        return parseXmlTag(xml, "release") ?: parseXmlTag(xml, "latest")
    }

    private fun resolveFapiForMc(mcVersion: String): String? {
        if (fapiCache.isEmpty()) {
            populateFapiCache()
        }
        return fapiCache[mcVersion]
            ?: findClosestVersion(fapiCache, mcVersion)
    }

    private fun populateFapiCache() {
        val xml = fetch("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml") ?: return
        val versions = parseAllXmlTags(xml, "version")

        val mcToFapi = mutableMapOf<String, String>()
        for (version in versions) {
            if (!version.contains("+") || version.contains("+build")) continue
            val mc = version.substringAfter("+")
            if (mc.isBlank()) continue
            mcToFapi[mc] = version
        }
        fapiCache.putAll(mcToFapi)
    }

    private fun resolveModMenuForMc(mcVersion: String): String? {
        if (modMenuCache.isEmpty()) {
            populateModMenuCache()
        }
        return modMenuCache[mcVersion]
    }

    private fun populateModMenuCache() {
        val json = fetch("https://api.modrinth.com/v2/project/mOgUt4GM/version") ?: return
        val array = gson.fromJson(json, JsonArray::class.java)

        val mcToModMenu = mutableMapOf<String, String>()
        for (element in array) {
            val obj = element.asJsonObject
            val versionNumber = obj.get("version_number")?.asString ?: continue
            val versionType = obj.get("version_type")?.asString ?: "release"
            val gameVersions = obj.getAsJsonArray("game_versions") ?: continue

            for (gv in gameVersions) {
                val mc = gv.asString
                val existing = mcToModMenu[mc]
                if (existing == null || (versionType == "release" && isAlphaOrBeta(existing))) {
                    mcToModMenu[mc] = versionNumber
                }
            }
        }
        modMenuCache.putAll(mcToModMenu)
    }

    private fun resolveNeoForgeForMc(mcVersion: String): String? {
        if (neoForgeCache.isEmpty()) {
            populateNeoForgeCache()
        }
        return neoForgeCache[mcVersion]
    }

    private fun populateNeoForgeCache() {
        val json = fetch("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge") ?: return
        val obj = gson.fromJson(json, JsonObject::class.java)
        val versions = obj.getAsJsonArray("versions") ?: return

        val mcToNeo = mutableMapOf<String, String>()
        for (element in versions) {
            val version = element.asString
            val mc = neoForgeVersionToMc(version) ?: continue
            mcToNeo[mc] = version
        }
        neoForgeCache.putAll(mcToNeo)
    }

    private fun neoForgeVersionToMc(neoVersion: String): String? {
        val clean = neoVersion.removeSuffix("-beta")
        val parts = clean.split(".")
        if (parts.size < 3) return null

        val major = parts[0].toIntOrNull() ?: return null
        return when {
            major >= 26 -> {
                if (parts.size >= 4) parts.subList(0, 3).joinToString(".")
                else if (parts.size == 3) "${parts[0]}.${parts[1]}"
                else null
            }
            major >= 20 -> {
                val mcMinor = parts[1].toIntOrNull() ?: return null
                if (mcMinor == 0) return null
                "1.$major.$mcMinor"
            }
            else -> null
        }
    }

    private fun generateMcDep(mcVersion: String, loader: String): String {
        return when (loader) {
            "fabric" -> ">=$mcVersion"
            "forge" -> "[$mcVersion]"
            "neoforge" -> "[$mcVersion,)"
            else -> ">=$mcVersion"
        }
    }

    private fun isAlphaOrBeta(version: String): Boolean {
        val lower = version.lowercase()
        return "alpha" in lower || "beta" in lower
    }

    private fun resolveSodiumForMc(mcVersion: String): String? {
        if (sodiumCache.isEmpty()) populateSodiumCache()
        return sodiumCache[mcVersion]
    }

    // Returns true for MC versions where sodium 0.8+ is published on CaffeineMC's maven (>=1.21.11).
    private fun isNewSodiumMc(mcVersion: String): Boolean {
        val parts = mcVersion.split(".")
        val major = parts[0].toIntOrNull() ?: return false
        if (major > 1) return true // 26.x and beyond
        if (parts.size < 3) return false
        val minor = parts[1].toIntOrNull() ?: return false
        val patch = parts[2].toIntOrNull() ?: return false
        return minor > 21 || (minor == 21 && patch >= 11)
    }

    private fun populateSodiumCache() {
        // MC >= 1.21.11: sodium 0.8+ is on CaffeineMC's immutable maven.
        // Coordinate: net.caffeinemc:sodium-fabric:{sodiumVer}+mc{mcVer}
        val caffeineXml = fetch("https://maven.caffeinemc.net/releases/net/caffeinemc/sodium-fabric/maven-metadata.xml")
        if (caffeineXml != null) {
            // Group all versions by MC, preferring stable; among same stability, take last (newest)
            val caffeineByMc = mutableMapOf<String, String>()
            for (v in parseAllXmlTags(caffeineXml, "version")) {
                val plusMcIdx = v.indexOf("+mc")
                if (plusMcIdx == -1) continue
                val mc = v.substring(plusMcIdx + 3)
                if (!isNewSodiumMc(mc)) continue
                val existing = caffeineByMc[mc]
                val candidateIsBeta = v.contains("-beta") || v.contains("-alpha")
                val existingIsBeta = existing != null && (existing.contains("-beta") || existing.contains("-alpha"))
                if (existing == null || (!candidateIsBeta && existingIsBeta) || (candidateIsBeta == existingIsBeta)) {
                    caffeineByMc[mc] = v
                }
            }
            sodiumCache.putAll(caffeineByMc)
        }
        // MC < 1.21.11: sodium pre-0.8 is NOT on CaffeineMC maven. Use Modrinth.
        // Store the raw Modrinth version_number for use as maven.modrinth:sodium:{version}.
        val json = fetch("https://api.modrinth.com/v2/project/AANobbMI/version") ?: return
        val array = gson.fromJson(json, JsonArray::class.java)
        for (element in array) {
            val obj = element.asJsonObject
            val versionNumber = obj.get("version_number")?.asString ?: continue
            if (obj.get("version_type")?.asString != "release") continue
            val loaders = obj.getAsJsonArray("loaders") ?: continue
            if (loaders.none { it.asString == "fabric" }) continue
            val gameVersions = obj.getAsJsonArray("game_versions") ?: continue
            for (gv in gameVersions) {
                val mc = gv.asString
                if (sodiumCache.containsKey(mc)) continue
                if (isNewSodiumMc(mc)) continue // handled by CaffeineMC above
                sodiumCache[mc] = versionNumber
            }
        }
    }

    private fun resolveIrisForMc(mcVersion: String): String? {
        if (irisCache.isEmpty()) populateIrisCache()
        return irisCache[mcVersion] ?: findClosestVersion(irisCache, mcVersion)
    }

    private fun populateIrisCache() {
        val json = fetch("https://api.modrinth.com/v2/project/YL57xq9U/version") ?: return
        val array = gson.fromJson(json, JsonArray::class.java)
        val mcToIris = mutableMapOf<String, String>()
        for (element in array) {
            val obj = element.asJsonObject
            val versionNumber = obj.get("version_number")?.asString ?: continue
            val versionType = obj.get("version_type")?.asString ?: "release"
            val loaders = obj.getAsJsonArray("loaders") ?: continue
            val isFabric = loaders.any { it.asString == "fabric" }
            if (!isFabric) continue
            val gameVersions = obj.getAsJsonArray("game_versions") ?: continue
            for (gv in gameVersions) {
                val mc = gv.asString
                val existing = mcToIris[mc]
                if (existing == null || (versionType == "release" && isAlphaOrBeta(existing))) {
                    mcToIris[mc] = versionNumber
                }
            }
        }
        irisCache.putAll(mcToIris)
    }

    private fun resolveHypixelForMc(mcVersion: String): String? {
        // VersionInfo.kt covers the known MC versions. This is a best-effort runtime fallback.
        // Two published Modrinth releases map to major MC families:
        //   1.0.2+build.1+mc26.1 -> 26.x
        //   1.0.1+build.1+mc1.21 -> 1.21.x
        val major = mcVersion.split(".")[0].toIntOrNull() ?: return null
        return if (major > 1) "1.0.2+build.1+mc26.1" else "1.0.1+build.1+mc1.21"
    }

    private fun resolveLatestMixinExtras(): String? {
        if (mixinExtrasLatest != null) return mixinExtrasLatest
        val xml = fetch("https://repo.spongepowered.org/repository/maven-public/io/github/llamalad7/mixinextras-fabric/maven-metadata.xml") ?: return null
        return (parseXmlTag(xml, "release") ?: parseXmlTag(xml, "latest")).also { mixinExtrasLatest = it }
    }

    private fun findClosestVersion(cache: Map<String, String>, mcVersion: String): String? {
        val parts = mcVersion.split(".")
        for (i in parts.size downTo 2) {
            val prefix = parts.subList(0, i).joinToString(".")
            val match = cache.entries
                .filter { it.key.startsWith(prefix) }
                .maxByOrNull { it.key }
            if (match != null) return match.value
        }
        return null
    }

    private fun fetch(url: String): String? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (compatible; Blahaj/3.0; +https://github.com/axlecoffee/blahaj)")
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) response.body() else null
        } catch (e: Exception) {
            logger.debug("[Blahaj] HTTP fetch failed for $url: ${e.message}")
            null
        }
    }

    private fun parseXmlTag(xml: String, tag: String): String? {
        val start = xml.indexOf("<$tag>")
        if (start == -1) return null
        val end = xml.indexOf("</$tag>", start)
        if (end == -1) return null
        return xml.substring(start + tag.length + 2, end).trim()
    }

    private fun parseAllXmlTags(xml: String, tag: String): List<String> {
        val results = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val start = xml.indexOf("<$tag>", searchFrom)
            if (start == -1) break
            val end = xml.indexOf("</$tag>", start)
            if (end == -1) break
            results.add(xml.substring(start + tag.length + 2, end).trim())
            searchFrom = end + tag.length + 3
        }
        return results
    }
}
