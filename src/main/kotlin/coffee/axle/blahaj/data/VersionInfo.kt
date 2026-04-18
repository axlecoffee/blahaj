// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj.data

class VersionInfo private constructor() {
    companion object {
        private val versionDefaults: MutableMap<String, MutableMap<String, String>> = mutableMapOf(
            // Forge Version
            "deps.fml" to mutableMapOf(
                "1.20.1-forge" to "47.2.16",
                "1.21.1-neoforge" to "21.1.226",
                "1.21.4-neoforge" to "21.4.157",
                "26.1.2-neoforge" to "26.1.2.15-beta"
            ),
            // Fabric Version
            "deps.fabric_loader" to mutableMapOf(
                "1.20.1-fabric" to "0.16.10",
                "1.21.1-fabric" to "0.16.10",
                "1.21.4-fabric" to "0.16.10",
                "1.21.10-fabric" to "0.18.4", // 0.19.2 latest
                "1.21.11-fabric" to "0.18.4", // 0.19.2 latest
                "26.1-fabric" to "0.19.2",
                "26.1.1-fabric" to "0.19.2",
                "26.1.2-fabric" to "0.19.2", // assume no one is running old fabric loader
                "26.2-fabric" to "0.19.2" // this "should" be the correct name scheme, currently only 26.2-snapshot-3 is out
            ),
            // Fabric API
            "deps.fapi" to mutableMapOf(
                // TODO: auto populate based on https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
                "1.20.1-fabric" to "0.92.8+1.20.1",

                "1.21.1-fabric" to "0.116.11+1.21.1",
                "1.21.4-fabric" to "0.119.4+1.21.4",


                "1.21.10-fabric" to "0.138.4+1.21.10",
                "1.21.11-fabric" to "0.141.3+1.21.11", // "final" with 8.7m downloads

                "26.1-fabric" to "0.145.1+26.1",
                "26.1.1-fabric" to "0.145.4+26.1.1", // 0.146.1 and 0.146.0 are for "26.1.x" - assume 26.1.3 will supported
                "26.1.2-fabric" to "0.146.1+26.1.2",
                "26.2-fabric" to "0.146.1+26.2"
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
                "26.1-fabric" to "18.0.0-alpha.8",
                "26.1.1-fabric" to "18.0.0-alpha.8",
                "26.1.2-fabric" to "18.0.0-alpha.8"
                
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
                "26.1.2-neoforge" to "[26.1.2,)"
            ),
            // Fabric Language Kotlin (MC-version independent, uses "*" wildcard)
            "deps.flk" to mutableMapOf(
                "*" to "1.13.10+kotlin.2.3.20"
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