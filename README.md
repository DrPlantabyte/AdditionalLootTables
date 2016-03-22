# AdditionalLootTables
Mod for Minecraft-Forge 1.9 that makes it easier to add custom mod items to loot tables

# How it works
When this mod is installed, it will create a folder in the **config** folder called **additional-loot-tables**. You (or your mod) can place folders in **additional-loot-tables** that contain loot table definitions.

For example, **ModX** wants to make dungeon chests contain a jetpack as treasure. To do this, **ModX** creates the file **config/additional-loot-tables/modx/chests/simple_dungeon.json**. This file contains the loot table definition for spawning a jetpack. When a player with **ModX** and **AdditionalLootTables** installed creates or loads a world, **AdditionalLootTables** automatically detects the files in **config/additional-loot-tables** and merges them with the default (or manually added) Minecraft loot tables.
