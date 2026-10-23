<div align="center">

# 🔍 Global Search
### *The Universal Spotlight Search & Command Palette for Minecraft*

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-52C41A?style=for-the-badge&logo=minecraft&logoColor=white)](https://modrinth.com/mod/global-search)
[![Fabric](https://img.shields.io/badge/Fabric-009688?style=for-the-badge&logo=fabric&logoColor=white)](https://modrinth.com/mod/global-search)
[![NeoForge](https://img.shields.io/badge/NeoForge-FF7043?style=for-the-badge&logo=neoforge&logoColor=white)](https://modrinth.com/mod/global-search)
[![Client-Side Safe](https://img.shields.io/badge/Side-100%25%20Client-blue?style=for-the-badge)](https://modrinth.com/mod/global-search)
[![Lightweight](https://img.shields.io/badge/Size-%3C%20400_KB-informational?style=for-the-badge)](https://modrinth.com/mod/global-search)

**Stop digging through endless pause screens, nested config folders, and controls pages.**  
Press **`[G]`** anywhere to bring up a blazing-fast, macOS Spotlight / VS Code / IntelliJ-style search overlay that indexes **everything** in your game.

---

</div>

## 🌟 Why Global Search?

Have you ever spent 5 minutes looking for that one keybind, wondering where a mod hid its render distance option, or trying to remember the exact syntax for `/locate structure`? 

**Global Search** unifies every setting, keybinding, mod config, item, biome, gamerule, advancement, and live stat into a single intuitive search bar with instant actions.

---

## 🎮 Player Features

### ⚡ Everything at Your Fingertips
* **⌨️ In-Line Keybind Rebinding**: Search for an action (or search *"by key value"* like `K` to see what's using it), hit **[Rebind]**, and press your new key. Detect conflicts instantly!
* **⚙️ Smart In-Place Setting Editor**: Change Render Distance, FOV, Brightness, Sound Volumes, and Game Rules with sliders, number inputs, and toggle pills right inside the overlay without navigating vanilla submenus.
* **🛠️ Full Mod Config Integration**: Automatically indexes and provides in-place editors for **Sodium / Iris**, **Cloth Config**, **MaliLib** (*Litematica, MiniHUD, Tweakeroo*), **Xaero's Minimap/Worldmap**, **NeoForge configs**, and generic JSON/TOML mod files.
* **📋 1-Click Command Copying**: Instant `/locate biome`, `/locate structure`, `/give @p <item>`, and `/give @p enchanted_book` commands with complete tags ready to paste into chat or command blocks.
* **📊 Live World Statistics**: Track mined blocks, crafted items, mob kills, play time, and distances in real-time without leaving your world.
* **🏆 Advancement Browser**: Filter completed vs uncompleted advancements, view descriptions, and check your completion progress.
* **🎨 Modern Translucent Themes**: Cycle themes on the fly (`[F6]` or `[Ctrl + T]`) with sleek glassmorphism, rounded corners, glowing badges, and custom scrollbars:
  * **Obsidian Glass** *(Default)* — Sleek modern translucent dark glass
  * **Deep Netherite & Ancient Debris** — Atmospheric RPG styling with ancient runes
  * **Ender Void** — Deep space obsidian with neon cyan accents
  * **Amethyst Dream** — Vibrant crystalline purple aesthetic

---

## 🕹️ Controls & Shortcuts

| Keybind | Action |
| :--- | :--- |
| **`[G]`** | Open / Close **Global Search** *(rebindable in Controls)* |
| **`[F6]`** or **`[Ctrl + T]`** | Cycle UI Theme (*Obsidian Glass*, *Netherite RPG*, etc.) |
| **`[↑]` / `[↓]`** | Navigate through search results |
| **`[Enter]`** | Select, trigger editor modal, or copy command |
| **`[Esc]`** | Back to search / Close overlay |
| **`[Tab]`** | Focus category sidebar or search filter |

---

## 🗂️ What Can You Search?

| Category | What's Indexed | Available Instant Actions |
| :--- | :--- | :--- |
| **Keybinds** | All vanilla & modded key mappings | Rebind in-place, conflict detector, search by key |
| **Vanilla Settings** | Video, Audio, Controls, Chat, Accessibility | Sliders, booleans, cycling choices, restore default |
| **Mod Configs** | Sodium, Iris, Cloth Config, Malilib, Xaero, NeoForge | Direct value editing & preset tweaks |
| **Items & Blocks** | Every registered item, tool, and block in the game | 1-click `/give @p` command copy |
| **Enchantments** | All vanilla & modded enchantments with max level | 1-click `/give enchanted_book` command copy |
| **Potions** | Standard, splash, lingering, and tipped arrows | 1-click `/give` command copy |
| **Biomes** | Overworld, Nether, End, and datapack biomes | 1-click `/locate biome` command copy |
| **Structures** | Trial Chambers, Ancient Cities, Villages, Mod structures | 1-click `/locate structure` command copy |
| **Game Rules** | All world gamerules (`keepInventory`, `doDaylightCycle`, etc.) | Direct toggle / integer edit without typing commands |
| **Player Stats** | Blocks mined, items used, distances, times | Live tick-by-tick counter lookup |
| **Advancements** | Story, Nether, End, Adventure, Husbandry, Custom trees | View description, status, and completion % |
| **Mods** | All loaded Fabric and NeoForge mods | View version, author, and description |

---

## 💻 For Mod Developers: Register Your Mod's Configs

Global Search is designed to be **completely developer-friendly**. You can integrate your mod's settings in two ways:

### 1. Zero-Code Automatic Discovery (Default)
If your mod stores configurations in standard `.minecraft/config/` formats (`.json`, `.toml`, `.properties`) or uses **Cloth Config**, **NeoForge ConfigSpec**, or **MaliLib**, **Global Search automatically indexes and makes your settings editable with zero lines of code required!**

---

### 2. Custom Config Registration (Programmatic API)
If you want to provide custom categories, custom modal sliders, or rich interactive handlers, add Global Search as an optional compile-only dependency and register your provider:

```groovy
repositories {
    maven { url = "https://maven.fabricmc.net/" }
}

dependencies {
    // Optional / compile-only dependency
    compileOnly "com.igxxd:globalsearch-fabric-26.3:1.0.0.0"
}
```

#### Example: Registering a Custom Setting Provider
```java
import com.igxxd.globalsearch.search.SearchIndexer;
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.SearchResultType;
import com.igxxd.globalsearch.search.provider.SearchIndexProvider;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Set;

public class MyModSearchProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "My Custom Mod";
    }

    @Override
    public int getPriority() {
        return 80; // Higher priorities index earlier
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        String id = "mymod:enable_auto_farm";
        String key = SearchResultType.CONFIG.name() + ":" + id;

        if (seenKeys.add(key)) {
            results.add(SearchResult.builder(SearchResultType.CONFIG, id, "Auto Farm Automation")
                    .path("My Mod > Automation")
                    .value(MyModConfig.autoFarmEnabled ? "true" : "false")
                    .defaultValue("false") // Preserves pristine default!
                    .modId("mymod")
                    .modName("My Cool Mod")
                    .description("Automatically harvests and replants crops when mature.")
                    .editable(true)
                    .editHandler((mc, res, scr, onValueChanged) -> {
                        // Toggle boolean or open custom screen
                        MyModConfig.autoFarmEnabled = !MyModConfig.autoFarmEnabled;
                        MyModConfig.save();
                        String updated = String.valueOf(MyModConfig.autoFarmEnabled);
                        res.setValue(updated);
                        onValueChanged.accept(updated);
                        return true;
                    })
                    .build());
        }
    }
}
```

#### Registering during mod client initialization:
```java
// Register safely inside your ClientModInitializer or client setup
SearchIndexer.registerProvider(new MyModSearchProvider());
```

---

## 🔒 Performance & Safety

- ⚡ **Zero-Lag Asynchronous Indexing**: All indexing providers run in background daemon threads. Opening the search screen never stutters, blocks the render loop, or drops FPS.
- 📦 **Featherlight Footprint**: The entire mod JAR is **under 380 KB** with optimized 256×256 power-of-two assets.
- 🌐 **100% Client-Side Safe**: Safe to install on the client and connect to any vanilla or modded server. Does not require server-side installation.

---

## 📥 Installation

1. Install **Fabric Loader** (with Fabric API) or **NeoForge** for **Minecraft 26.3**.
2. Download the matching **Global Search** JAR from the Versions tab.
3. Drop the JAR into your `.minecraft/mods` folder.
4. Launch the game and press **`[G]`** to start searching!

---

<div align="center">

*Found an issue or have a suggestion? Visit our [GitHub Issues](https://github.com/gulab9762/global-search/issues) page!*

</div>
