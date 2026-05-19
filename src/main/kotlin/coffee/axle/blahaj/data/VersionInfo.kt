// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj.data

class VersionInfo private constructor() {
    companion object {
        private val versionDefaults: MutableMap<String, MutableMap<String, String>> = mutableMapOf(
            // Forge Version
            "deps.fml" to mutableMapOf(
                "1.20.1-forge" to "47.2.16",
                "1.21.1-neoforge" to "21.1.230",
                "1.21.4-neoforge" to "21.4.157",
                "26.1.2-neoforge" to "26.1.2.59-beta"
            ),
            // Fabric Version
            "deps.fabric_loader" to mutableMapOf(
                "1.20.1-fabric" to "0.16.10",
                "1.21.1-fabric" to "0.16.10",
                "1.21.4-fabric" to "0.16.10",
                // Use blahaj/3.0.4 for 1.21.10 support with fabric 0.18x
                "1.21.10-fabric" to "0.19.2",
                "1.21.11-fabric" to "0.19.2", 
                "26.1-fabric" to "0.19.2",
                "26.1.1-fabric" to "0.19.2",
                "26.1.2-fabric" to "0.19.2", // assume no one is running old fabric loader
                "26.2-fabric" to "0.19.2" // this "should" be the correct name scheme, currently only 26.2-snapshot-5 is out
            ),
            // Fabric API
            "deps.fapi" to mutableMapOf(
                // TODO: auto populate based on https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
                "1.20.1-fabric" to "0.92.9+1.20.1",

                "1.21.1-fabric" to "0.116.12+1.21.1",
                "1.21.4-fabric" to "0.119.4+1.21.4",


                "1.21.10-fabric" to "0.138.4+1.21.10",
                "1.21.11-fabric" to "0.141.4+1.21.11", // "final" with 8.7m downloads

                "26.1-fabric" to "0.145.1+26.1",
                "26.1.1-fabric" to "0.145.4+26.1.1", // 0.146.1 and 0.146.0 are for "26.1.x" - assume 26.1.3 will supported
                "26.1.2-fabric" to "0.149.1+26.1.2",
                "26.2-fabric" to "0.149.1+26.2"
            ),
            // Forge Config API Port
            "deps.forgeconfigapi" to mutableMapOf(
                "1.20.1-fabric" to "8.0.0",
                "1.21.1-fabric" to "21.1.0",
                "1.21.4-fabric" to "21.4.1"
            ),
            // Mod Menu
            "deps.modmenu" to mutableMapOf(
                "1.20.1-fabric" to "7.2.2",
                "1.21.1-fabric" to "11.0.4",
                "1.21.4-fabric" to "13.0.4",
                "1.21.10-fabric" to "16.0.1",
                "1.21.11-fabric" to "17.0.0",
                "26.1-fabric" to "18.0.0-beta.1",
                "26.1.1-fabric" to "18.0.0-beta.1",
                "26.1.2-fabric" to "18.0.0-beta.1"
                //"26.2-fabric" to "18.0.0-alpha.8"
            ),
            // Minecraft Dependency Block
            "mod.mc_dep" to mutableMapOf(
                "1.20.1-fabric" to ">=1.20 <=1.20.1",
                "1.20.1-forge" to "[1.20.1]",
                "1.21.1-fabric" to ">=1.21",
                "1.21.1-neoforge" to "[1.21.1,)",
                "1.21.4-fabric" to ">=1.21.4",
                "1.21.4-neoforge" to "[1.21.4,)",
                "1.21.10-fabric" to ">=1.21.10",
                "1.21.11-fabric" to ">=1.21.11",
                "26.1-fabric" to ">=26.1",
                "26.1.1-fabric" to ">=26.1",
                "26.1.2-fabric" to ">=26.1",
                "26.1.2-neoforge" to "[26.1.2,)",
                "26.2-fabric" to ">=26.2"
            ),
            // Fabric Language Kotlin (MC-version independent, uses "*" wildcard)
            "deps.flk" to mutableMapOf(
                "*" to "1.13.11+kotlin.2.3.21"
            ),

            // Mocha - this is my personal abstraction library that I will soon use in all my mods
            // it's a multiversion helper as well!
            "deps.mocha" to mutableMapOf(
                "1.21.10-fabric" to "",
                "1.21.11-fabric" to "",
                "26.1-fabric" to "",
                "26.1.1-fabric" to "",
                "26.1.2-fabric" to "",
                "26.2-fabric" to ""
            ),
            /**
            * Sodium is extremely interesting
            * Based on my conversation with douira [318403987911082006] a "Project Maintainer"
            * all sodium versions after 0.8.0 have been published to their actual https://maven.caffeinemc.net/#/ repository, but for some reason they are not indexed by search engines or something, so they don't show up on mvnrepository or similar sites. This means that the only way to get the version numbers is to scrape them from the repository metadata, which is what I have done for the versions I care about. For older versions, I have left the version string blank, which will cause Blahaj to attempt to resolve it using the VersionResolver, which may or may not work depending on how well it can scrape the repository.
            * and should be immutable https://discord.com/channels/602796788608401408/1502128021723942952/1502129200566370434
            * This is unfortunate though, since for as long as i need to support 0.7x (1.21.10 and earlier) the sodium maven wont allow me to pull copies
            * therefore, versions including and prior to 1.21.10 are pulled from modrinth - which serves copies
            * I highly doubt the old 1.21.10 (and prior) versions will change, because as douira said, they are unsupported.
            * Furthermore, Sodium does support NEOFORGE in a "beta" state - however i dont actually use neoforge, feel free to PR
            */
            "deps.sodium" to mutableMapOf(
                "1.21.11-fabric" to "0.8.12+mc1.21.11",
                "26.1-fabric" to "0.8.8+mc26.1",
                "26.1.1-fabric" to "0.8.9+mc26.1.1",
                "26.1.2-fabric" to "0.8.12+mc26.1.2",
                "1.21.9-fabric" to "mc1.21.10-0.7.3-fabric",
                "1.21.10-fabric" to "mc1.21.10-0.7.3-fabric",
                "1.21.6-fabric" to "mc1.21.8-0.7.3-fabric",
                "1.21.7-fabric" to "mc1.21.8-0.7.3-fabric",
                "1.21.8-fabric" to "mc1.21.8-0.7.3-fabric",
                "1.21.4-fabric" to "mc1.21.4-0.6.13-fabric",
                "1.21.2-fabric" to "mc1.21.3-0.6.13-fabric",
                "1.21.3-fabric" to "mc1.21.3-0.6.13-fabric",
                "1.21-fabric" to "mc1.21.1-0.6.13-fabric",
                "1.21.1-fabric" to "mc1.21.1-0.6.13-fabric",
                "1.20.1-fabric" to "mc1.20.1-0.5.13-fabric",
                "1.20.6-fabric" to "mc1.20.6-0.5.11",
                "1.20.5-fabric" to "mc1.20.6-0.5.8",
                "1.20.3-fabric" to "mc1.20.4-0.5.8",
                "1.20.4-fabric" to "mc1.20.4-0.5.8",
                "1.20.2-fabric" to "mc1.20.2-0.5.5",
                "1.20-fabric" to "mc1.20-0.4.10",
                "1.19.4-fabric" to "mc1.19.4-0.4.10",
                "1.19.3-fabric" to "mc1.19.3-0.4.9",
                "1.19-fabric" to "mc1.19.2-0.4.4",
                "1.19.1-fabric" to "mc1.19.2-0.4.4",
                "1.19.2-fabric" to "mc1.19.2-0.4.4",
                "1.18.2-fabric" to "mc1.18.2-0.4.1",
                "1.17-fabric" to "mc1.17.1-0.3.4",
                "1.17.1-fabric" to "mc1.17.1-0.3.4",
                "1.16.3-fabric" to "mc1.16.5-0.2.0",
                "1.16.4-fabric" to "mc1.16.5-0.2.0",
                "1.16.5-fabric" to "mc1.16.5-0.2.0"
            ),

            "deps.iris" to mutableMapOf(
                "26.1-fabric" to "1.10.9+26.1-fabric",
                "26.1.1-fabric" to "1.10.9+26.1-fabric",
                "26.1.2-fabric" to "1.10.9+26.1-fabric",
                "1.21.11-fabric" to "1.10.7+1.21.11-fabric",
                "1.21.9-fabric" to "1.9.7+1.21.10-fabric",
                "1.21.10-fabric" to "1.9.7+1.21.10-fabric",
                "1.21.6-fabric" to "1.9.6+1.21.8-fabric",
                "1.21.7-fabric" to "1.9.6+1.21.8-fabric",
                "1.21.8-fabric" to "1.9.6+1.21.8-fabric",
                "1.21.5-fabric" to "1.8.11+1.21.5-fabric",
                "1.20.1-fabric" to "1.7.6+1.20.1",
                "1.21.4-fabric" to "1.8.8+1.21.4-fabric",
                "1.21-fabric" to "1.8.8+1.21.1-fabric",
                "1.21.1-fabric" to "1.8.8+1.21.1-fabric",
                "1.21.3-fabric" to "1.8.1+1.21.3-fabric",
                "1.21.2-fabric" to "1.8.0+1.21.3-fabric",
                "1.20.5-fabric" to "1.7.2+1.20.6",
                "1.20.6-fabric" to "1.7.2+1.20.6",
                "1.20.3-fabric" to "1.7.2+1.20.4",
                "1.20.4-fabric" to "1.7.2+1.20.4",
                "1.20.2-fabric" to "1.6.14+1.20.2",
                "1.20-fabric" to "1.6.11+1.20.1",
                "1.19.4-fabric" to "1.6.11+1.19.4",
                "1.19-fabric" to "1.6.11+1.19.2",
                "1.19.1-fabric" to "1.6.11+1.19.2",
                "1.19.2-fabric" to "1.6.11+1.19.2",
                "1.18.2-fabric" to "1.6.11+1.18.2",
                "1.19.3-fabric" to "1.5.2+1.19.3",
                "1.16.5-fabric" to "1.4.5+1.16.5",
                "1.17.1-fabric" to "1.17.x-v1.2.7",
                "1.18.1-fabric" to "1.18.x-v1.2.0",
                "1.18-fabric" to "mc1.18.1-1.1.3",
                "1.17-fabric" to "mc1.17-v1.1.1"
            ),

            "deps.mixinextras" to mutableMapOf(
                "*" to "0.5.4"
            ),

            "deps.devauth" to mutableMapOf(
                "*" to "1.2.2"
            ),

            // harmful asf ig
            "deps.hypixel" to mutableMapOf(
                "1.21.10-fabric" to "1.0.1+build.1+mc1.21",
                "1.21.11-fabric" to "1.0.1+build.1+mc1.21",
                "26.1-fabric" to "1.0.2+build.1+mc26.1",
                "26.1.1-fabric" to "1.0.2+build.1+mc26.1",
                "26.1.2-fabric" to "1.0.2+build.1+mc26.1",
                "26.2-fabric" to "1.0.2+build.1+mc26.1"
            ),
            // Curseforge/Modrinth Version Targets
            "mod.mc_targets" to mutableMapOf(
                "1.20.1-fabric" to "1.20 1.20.1",
                "1.20.1-forge" to "1.20 1.20.1",
                "1.21.1-fabric" to "1.21.1",
                "1.21.1-neoforge" to "1.21.1",
                "1.21.4-fabric" to "1.21.4",
                "1.21.4-neoforge" to "1.21.4",
                "1.21.10-fabric" to "1.21.10",
                "1.21.11-fabric" to "1.21.11",
                "26.1-fabric" to "26.1",
                "26.1.1-fabric" to "26.1.1",
                "26.1.2-fabric" to "26.1.2",
                "26.1.2-neoforge" to "26.1.2"
            )
        )

        fun getVersion(gradleProperties: Map<String, *>, propertyKey: String, versionString: String) : String? {
            var gradleVersion = gradleProperties[propertyKey] as? String
            if (gradleVersion == "[VERSIONED]" || gradleVersion == "VERSIONED")
                gradleVersion = null

            if (gradleVersion != null) return gradleVersion

            val hardcoded = versionDefaults[propertyKey]?.get(versionString)
            if (hardcoded != null) return hardcoded

            val wildcard = versionDefaults[propertyKey]?.get("*")
            if (wildcard != null) return wildcard

            val parts = versionString.split("-", limit = 2)
            if (parts.size == 2) {
                val resolved = VersionResolver.resolve(propertyKey, parts[0], parts[1])
                if (resolved != null) {
                    addOrUpdateDefault(propertyKey, versionString, resolved)
                    return resolved
                }
            }

            return null
        }

        fun addOrUpdateDefault(propertyKey: String, versionString: String, version: String) {
            versionDefaults.computeIfAbsent(propertyKey) { mutableMapOf() }[versionString] = version
        }

        fun getVersionDefaults(): Map<String, Map<String, String>> {
            return versionDefaults
        }
    }
}