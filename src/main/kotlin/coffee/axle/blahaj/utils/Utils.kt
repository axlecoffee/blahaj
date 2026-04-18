// SPDX-License-Identifier: CC-BY-4.0
// SPDX-FileCopyrightText: Axle Coffee <contact@axle.coffee>
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.exclusiveMaven(url: String, vararg groups: String) {
    exclusiveContent {
        forRepository { maven(url) }
        filter {
            groups.forEach {
                includeGroup(it)
            }
        }
    }
}