// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
package coffee.axle.blahaj.api

import coffee.axle.blahaj.BlahajBuild

data class ModData (
    val id : String,
    val name : String,
    val version : String,
    val group : String,
    val author : String,
    val namespace : String,
    val displayName : String,
    val description : String,
    val discord : String,
    val mcDep : String,
    val license : String,
    val github : String,
    val clientuser  : String,
    val clientuuid : String,
    val mcVersion : String,
    val isActive : Boolean,
    val loader : String,
    val projectName : String,
    val isFabric : Boolean,
    val isForge : Boolean,
    val isNeo : Boolean,
    val depends : MutableMap<String, String>) {

    companion object {
        fun from(build: BlahajBuild): ModData {
            return ModData(
                build.project.properties["mod.id"].toString(),
                build.project.properties["mod.name"].toString(),
                build.project.properties["mod.version"].toString(),
                build.project.properties["mod.group"].toString(),
                build.project.properties["mod.author"].toString(),
                build.project.properties["mod.namespace"].toString(),
                build.project.properties["mod.display_name"].toString(),
                build.project.properties["mod.description"].toString(),
                build.project.properties["mod.discord"].toString(),
                build.getVersion("mod.mc_dep").toString(),
                build.project.properties["mod.license"].toString(),
                build.project.properties["mod.github"].toString(),
                build.project.properties["client.user"].toString(),
                build.project.properties["client.uuid"].toString(),
                build.sc.current.project.substringBeforeLast('-'),
                build.sc.active?.project == build.sc.current.project,
                build.loader,
                build.sc.current.project,
                build.loader == "fabric",
                build.loader == "forge",
                build.loader == "neoforge",
                mutableMapOf()
            )
        }
    }

    public fun getDependsBlock() : String {
        val sb = StringBuilder()
        for (depend in depends) {
            var value = depend.value;
            if (isFabric) {
                if (value != "*")
                    value = ">=$value"
                sb.append("\"${depend.key}\": \"${value}\",")
            } else {
                if (value != "*")
                    value = "[$value,)"
                sb.append("[[dependencies.\"${id}\"]]\n" +
                        "modId=\"${depend.key}\"\n" +
                        "mandatory=true\n" +
                        "versionRange=\"${value}\"\n" +
                        "ordering=\"NONE\"\n" +
                        "side=\"BOTH\"")
            }
        }

        return sb.toString()
    }
}