# 🔍 Global Search — The Universal Spotlight Search for Minecraft

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Modloader-Fabric%20%26%20NeoForge-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey.svg)](LICENSE)

> **Never waste time digging through dozens of option menus, submenus, or JEI/REI pages again.**  
> **Global Search** brings a blazing-fast, macOS Spotlight / IntelliJ-style search experience directly inside Minecraft. Press your hotkey anywhere, instantly find whatever you need, and edit settings or copy commands in one click.

---

## ✨ Features at a Glance

- 🔍 **Instant Universal Search**: Find keybinds, vanilla settings, mod configs, items, enchantments, potions, biomes, dimensions, structures, gamerules, player statistics, and advancements in one unified search bar.
- ⌨️ **Live In-Line Keybind Rebinding**: Search for conflicts or unbound keys, click **[Edit]**, and press any key or mouse button to rebind immediately without digging into controls menus. Also supports **"By Key Value"** search to find which action is assigned to a specific key!
- 🎛️ **Universal Interactive Setting Editor**: Seamlessly toggle booleans, adjust numeric sliders with smart bounds clamping, or pick choices right from the search overlay.
- 📋 **1-Click Command Copying**: Instant `/locate biome`, `/locate structure`, `/give @p <item>`, and `/give @p enchanted_book` commands with full NBT tags ready to paste into chat or command blocks.
- 📊 **Real-Time Live World Statistics**: View mined blocks, crafted items, play time, distance traveled, and damage stats synchronized live with the world without opening the vanilla stats screen.
- 🏆 **Advancement Tracker**: Filter completed vs uncompleted advancements, view descriptions, reward tiers, and completion progress percentages.
- 🏛️ **Full Structure & Biome Indexing**: Discovers both vanilla structures (Trial Chambers, Ancient Cities, Outposts) and modded datapack structures with live coordinate locating.
- ⚡ **Asynchronous Zero-Lag Engine**: All indexing providers run concurrently in background daemon threads. Opening the search screen never stutters or drops a frame.
- 🎨 **Sleek Custom Themes**: Switch effortlessly between **Obsidian Glass**, **Netherite RPG**, **Ender Void**, and **Amethyst Dream** (`[F6]` or `[Ctrl + T]`) with glassmorphism styling, glowing cards, and custom scrollbars.
- 📜 **Scrollable Category Navigation**: Dedicated category sidebar with live result counts, category icons, and a custom scrollbar that adapts gracefully to any GUI scale.

---

## ⚔️ Supported Categories

| Category | What You Can Search | Interactive Actions |
| :--- | :--- | :--- |
| **⌨️ Keybinds** | All vanilla & modded key mappings | Rebind in-place, search by key or action, conflict detector |
| **⚙️ Vanilla Settings** | FOV, Audio, Video, Graphics, Chat, Accessibility | Sliders, toggles, cycling choices |
| **🛠️ Mod Configs** | Cloth Config, Malilib (Litematica, MiniHUD), NeoForge, Xaero's | Direct config jumps and value editing |
| **⛏️ Items & Blocks** | All registered blocks and items across mods | 1-click `/give @p` command copy |
| **✨ Enchantments** | Vanilla & modded enchantments with max levels | Copy enchanted book give command |
| **🧪 Potions** | All standard, splash, lingering, and tipped arrows | Copy `/give` command with exact effects |
| **🌲 Biomes** | Overworld, Nether, End, and modded biomes | 1-click `/locate biome` command copy |
| **🏛️ Structures** | Ancient Cities, Trial Chambers, Villages, Mod structures | 1-click `/locate structure` copy & locate in chat |
| **⚖️ Game Rules** | All world gamerules (keepInventory, doDaylightCycle, etc.) | Instant boolean / number toggle without typing |
| **📊 Statistics** | Custom stats, mined blocks, crafted items | Real-time tick-by-tick counter tracking |
| **🏆 Advancements** | Story, Nether, End, Adventure, Husbandry, Mod trees | View progress, description, completion status |
| **📦 Mods** | Installed mods, versions, and authors | Mod metadata and fast filtering |

---

## 🎮 Default Controls & Shortcuts

| Hotkey | Action |
| :--- | :--- |
| `[G]` *(Customizable)* | Open **Global Search** |
| `[F6]` or `[Ctrl + T]` | Cycle Visual Theme (Obsidian Glass, Netherite RPG, etc.) |
| `[Enter]` | Select, Edit, or Copy Highlighted Entry |
| `[↑ / ↓]` | Navigate Results List |
| `[Esc]` | Close Search / Cancel Rebind |

---

## 📥 Installation

1. Install the appropriate mod loader:
   - **Fabric Loader** (requires Fabric API)
   - **NeoForge**
2. Download the latest `globalsearch-*.jar` for Minecraft **26.3**.
3. Place the JAR file into your `.minecraft/mods` directory.
4. Launch Minecraft and press `[G]` to search everything!

---

## 💡 Compatibility

- **Minecraft Version**: 26.3
- **Loaders**: Fabric & NeoForge
- **Integrations**: Cloth Config, Malilib (Litematica, Tweakeroo, etc.), Xaero's Maps, Vanilla Options, Datapack worldgen.
- **Client-Side**: 100% client-side functional; works flawlessly when connecting to vanilla or modded servers!
