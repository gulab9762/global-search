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
