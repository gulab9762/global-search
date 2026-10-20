# Global Search - Minecraft 26.3 MultiLoader Project

A modern multi-loader mod project setup for Minecraft 26.3 supporting **Fabric** and **NeoForge** from a shared **Common** codebase.

## Project Structure

```
global-search/
├── .vscode/
│   ├── tasks.json             # Windows VS Code tasks to start clients and build
│   └── settings.json
├── build-logic/               # Convention plugins (multiloader-common, multiloader-loader)
├── common/                    # Shared code compiled across all platforms
│   └── src/main/java/...
├── fabric/                    # Fabric loader implementation & entrypoints
│   └── src/main/java/...
├── neoforge/                  # NeoForge loader implementation & entrypoints
│   └── src/main/java/...
├── gradle/wrapper/            # Gradle Wrapper (9.7.1)
├── build.gradle               # Root build script
├── settings.gradle            # Project & repository settings
└── gradle.properties          # Version configurations (MC 26.3, Fabric, NeoForge, Java 25)
```

## Running the Client in VS Code (Windows)

1. Open the Command Palette in VS Code (`Ctrl+Shift+P`).
2. Type and select **Tasks: Run Task**.
3. Choose either:
   - **Start Fabric Client (Windows)**
   - **Start NeoForge Client (Windows)**

Or use the default test task shortcut (`Terminal -> Run Test Task`).

## Running via Command Line (PowerShell / CMD)

- **Run Fabric Client**:
  ```powershell
  .\gradlew.bat :fabric:runClient
  ```

- **Run NeoForge Client**:
  ```powershell
  .\gradlew.bat :neoforge:runClient
  ```

- **Build all jars**:
  ```powershell
  .\gradlew.bat build
  ```

---

## Mod Developer Integration Guide

Global Search provides a public API (`GlobalSearchApi`) that allows other mod developers to:
- Add custom searchable categories (with in-game item icons and customized colors).
- Implement and register custom search index providers.
- Construct and index entries with a fluent builder (`SearchResult.builder(...)`).
- Open the Global Search GUI programmatically or query the central search index.

### 1. Adding Global Search as a Dependency

In your mod's `build.gradle`:

```groovy
repositories {
    maven {
        url = "https://maven.example.com" // Or local maven / flatDir
    }
}

dependencies {
    // Compile against Global Search API in common / client code
    compileOnly "com.igxxd:global-search-common:1.0.0"
}
```

---

### 2. Registering a Custom Search Category

You can register a custom search category to add a dedicated filter button in the left navigation panel:

```java
import com.igxxd.globalsearch.api.GlobalSearchApi;
import com.igxxd.globalsearch.search.SearchResultType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MyModCategories {

    public static final SearchResultType QUESTS = GlobalSearchApi.registerCategory(
        "quests",                           // Unique Category ID
        "Quests",                           // Display Label (on filter button)
        "QUEST",                            // Short badge code (3-6 characters)
        0xFFFFAA00,                         // Text / Accent color (ARGB)
        () -> new ItemStack(Items.WRITABLE_BOOK) // In-game icon displayed in search results
    );
}
```

*Note: You can also define custom background and value colors using the overloaded `GlobalSearchApi.registerCategory(...)` method.*

---

### 3. Implementing a Custom Search Index Provider

Create a class implementing `SearchIndexProvider`:

```java
import com.igxxd.globalsearch.search.SearchResult;
import com.igxxd.globalsearch.search.provider.SearchIndexProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MyQuestIndexProvider implements SearchIndexProvider {

    @Override
    public String getName() {
        return "My Mod Quests";
    }

    @Override
    public void index(Minecraft minecraft, List<SearchResult> results, Set<String> seenKeys) {
        // Query your mod's registry, config, or runtime data:
        for (MyQuest quest : MyQuestManager.getAllQuests()) {
            String id = "quest:" + quest.getId();
            String key = MyModCategories.QUESTS.name() + ":" + id.toLowerCase(Locale.ROOT);

            // Ensure deduplication using seenKeys
            if (seenKeys.add(key)) {
                results.add(SearchResult.builder(MyModCategories.QUESTS, id, quest.getTitle())
                    .modId("mymodid")
                    .modName("My Quest Mod")
                    .path("Quests > " + quest.getChapter())
                    .value(quest.getStatus()) // e.g. "Completed", "In Progress"
                    .icon(new ItemStack(quest.getDisplayItem()))
                    .description(quest.getDescription())
                    .build());
            }
        }
    }
}
```

---

### 4. Registering Your Provider

Register your provider during client mod initialization (e.g. `ClientModInitializer` on Fabric or `FMLClientSetupEvent` on NeoForge):

```java
import com.igxxd.globalsearch.api.GlobalSearchApi;

public class MyModClient {
    public static void onClientInit() {
        // Register your custom search provider
        GlobalSearchApi.registerProvider(new MyQuestIndexProvider());
    }
}
```

---

### 5. Opening Global Search Programmatically

Any mod can open the Global Search screen (e.g., from a custom keybind, pause menu button, or quest book button):

```java
import com.igxxd.globalsearch.api.GlobalSearchApi;
import net.minecraft.client.gui.screens.Screen;

public class MyGuiHelper {
    public static void openSearch(Screen currentScreen) {
        GlobalSearchApi.openSearchScreen(currentScreen);
    }
}
```

---

### 6. Supporting Inline Editing in Your Mod

Global Search allows players to edit settings and keybinds directly from search results with an inline `[Edit]` button and clickable value. Mod developers can make their custom entries editable through two easy ways:

#### Option A: Direct Callback on `SearchResult`

```java
results.add(SearchResult.builder(MY_CATEGORY, "sound_effect", "Sound Effects")
    .value("ON")
    .onEdit((minecraft, screen, onValueChanged) -> {
        boolean newState = MyConfig.toggleSound();
        String display = newState ? "ON" : "OFF";
        onValueChanged.accept(display); // Updates the UI row instantly
        return true; // handled
    })
    .build());
```

#### Option B: Implementing `handleEdit` on `SearchIndexProvider`

```java
public class MyIndexProvider implements SearchIndexProvider {
    @Override
    public String getName() { return "My Mod Config"; }

    @Override
    public void index(Minecraft mc, List<SearchResult> results, Set<String> seenKeys) {
        results.add(SearchResult.builder(MY_CATEGORY, "volume", "Master Music")
            .value("50%")
            .provider(this) // bind provider
            .editable(true)  // mark as editable
            .build());
    }

    @Override
    public boolean handleEdit(Minecraft mc, SearchResult result, Screen screen, Consumer<String> onValueChanged) {
        if ("volume".equals(result.getId())) {
            int next = (MyConfig.getVolume() + 10) % 110;
            MyConfig.setVolume(next);
            onValueChanged.accept(next + "%");
            return true;
        }
        return false;
    }
}
```

You can also trigger editing programmatically on any `SearchResult`:
```java
GlobalSearchApi.triggerEdit(result, screen, (newValue) -> {
    System.out.println("Result updated to: " + newValue);
});
```

---

### 7. Best Practices for Mod Integrations

1. **Client-Only Registration**: Always register search providers and categories in client initialization code; the UI and indexing are client-side.
2. **Deduplication**: Always check `if (seenKeys.add(key))` before adding results to avoid duplicates across scans.
3. **Responsive Descriptions**: Provide clear, helpful descriptions. If you use translation keys, Global Search automatically resolves them using `Language.getInstance()`.
4. **Icons**: Supply recognizable in-game `ItemStack` icons for entries to ensure an authentic visual experience.
5. **Fast Non-Blocking Edits**: When implementing inline edits, keep execution fast (e.g. toggling a boolean or cycling options) and call `onValueChanged.accept(newValue)` to provide instantaneous visual feedback to the player.

