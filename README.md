# Blahaj

Fully automated Gradle plugin for managing multiversion Minecraft mods, built on Stonecutter.

A massive credit goes to the original creator, [Toni (txnimc)](https://github.com/txnimc) for creating the original [Blahaj](https://github.com/txnimc/Blahaj) project!

The only difference(s) are:

- I have maven control so I know what's coming in :3
- Support for 1.21.10/1.21.11/26x (Hypixel SkyBlock mods!!!)
- misc stuff + kotlin (and less of a focus on Forge/Neoforge support, because I don't need it)

[Read the docs here](https://blahaj.txni.dev/) (original) or get started with the [template mod.](https://github.com/axlecoffee/blahaj-template)

```kotlin
plugins {
	id("coffee.axle.blahaj")
}

blahaj {
	config {
		yarn()
		versionedAccessWideners()
	}
	setup {
		txnilib("1.0.22")
		forgeConfig()

		// access Gradle's DependencyHandler
		deps.modImplementation("maven:modrinth:sodium:mc$mc-0.6.5-$loader")

		// configure Curseforge & Modrinth publish settings
		incompatibleWith("optifine")

		// add mods with Blahaj's fluent interface
		addMod("sodiumextras")
			.modrinth("sodium-extras") // override with Modrinth URL slug
			.addPlatform("1.21.1-neoforge", "neoforge-1.21.1-1.0.7")
			.addPlatform("1.21.1-fabric", "fabric-1.21.1-1.0.7") { required() }
	}
}
```

# License

My (axle.coffee) work in this repository is licensed under CC-BY-4.0 please see the REUSE.toml file!
