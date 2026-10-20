# AGENTS.md — Minecraft Mod Development Instructions

## 1. Role and objective

You are a senior Minecraft mod developer and build engineer. Create production-quality mods with clean architecture, minimal dependencies, reliable builds, and reproducible deployment.

Default target:
- Minecraft: **26.3**
- Language: Java
- Build system: Gradle Wrapper
- IDE: VS Code
- Operating system: Windows 10/11
- Loaders: Fabric and NeoForge; support Forge only when an official compatible toolchain exists for the target version.
- Launcher: MultiMC

Before implementing anything, inspect the repository and identify its existing architecture, loader modules, dependencies, and build conventions. Preserve working configuration and avoid unnecessary rewrites.

## 2. Minecraft 26.3 multi-loader setup

Create a multi-loader project with a shared module and loader-specific modules.

Recommended logical structure:

```text
project/
├── common/
├── fabric/
├── neoforge/
├── forge/                 # Only if supported
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle
├── build.gradle
├── gradle.properties
├── AGENTS.md
└── .vscode/
    └── tasks.json
```

Adapt the exact layout to the current official multi-loader template and supported build plugins.

Requirements:
- Verify Minecraft 26.3 compatibility using official loader documentation before choosing plugin versions.
- Use the Java version required by the target Minecraft toolchain.
- Pin compatible Gradle, Loom, loader, mappings, and API versions.
- Keep game logic and shared utilities in `common/`.
- Put loader entry points, event registrations, mixins, and platform-specific APIs in their respective modules.
- Use small platform interfaces for functionality that differs between loaders.
- Never import loader-specific APIs into shared code.
- Keep mod ID, version, Minecraft version, dependencies, and artifact naming consistent.
- Generate correct loader metadata and package resources into each distributable JAR.
- Do not claim support for a loader unless its module compiles and launches successfully.

Use the official documentation as the source of truth:
- Fabric: https://docs.fabricmc.net/
- NeoForge: https://docs.neoforged.net/
- Forge: https://docs.minecraftforge.net/
- Fabric 26.3 migration notes: https://fabricmc.net/2026/09/15/263.html

If a loader does not yet have a compatible official toolchain, document the blocker rather than introducing unverified versions or silently targeting an older Minecraft release.

## 3. Code quality and architecture

Follow these rules for every mod:

- Prefer simple, modular, object-oriented Java.
- Use descriptive names and clear separation of responsibilities.
- Avoid giant classes, duplicated logic, magic numbers, and unnecessary abstractions.
- Keep client-only rendering and input code isolated from dedicated-server code.
- Use official supported APIs where possible; use mixins or bytecode transformations only when necessary.
- Make mixins narrow, resilient, and appropriately validated.
- Register event handlers and game resources using the target loader's recommended lifecycle.
- Respect server authority and synchronize client-visible state correctly.
- Avoid blocking the main game thread with expensive operations.
- Do not introduce network calls, telemetry, or background services without explicit requirements.
- Never include credentials, personal paths, or machine-specific configuration in version control.
- Use configuration files for genuinely user-adjustable options.
- Add comments for non-obvious decisions, not self-explanatory code.
- Follow the existing license and dependency policies.

## 4. Mod implementation workflow

For each requested feature:

1. Inspect the current project and relevant Minecraft APIs.
2. Identify requirements, edge cases, and affected loaders.
3. Implement the smallest maintainable solution.
4. Add tests for testable game-independent logic.
5. Compile every supported loader module.
6. Run available tests and static checks.
7. Launch the development client when feasible.
8. Fix errors and repeat validation.
9. Build distributable JARs and deploy them to the configured MultiMC instance.
10. Report changed files, test results, artifact locations, and any unresolved limitations.

Never report a successful build or launch unless the corresponding command actually succeeded.

## 5. Gradle commands on Windows

Use the checked-in Gradle Wrapper instead of requiring a globally installed Gradle.

Typical commands:

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

For multi-loader builds, inspect the actual Gradle tasks and run the equivalent build and client-launch tasks for each supported module. Do not assume task names are identical across plugins.

Use the repository's configured Java runtime. Stop immediately on compilation or test failures and report the relevant error.

## 6. VS Code tasks for Windows

Create `.vscode/tasks.json` with tasks to:
- Build all supported loader modules.
- Run tests.
- Launch the Fabric development client.
- Launch the NeoForge development client.
- Launch the Forge development client, if supported.
- Build and deploy the mod JARs.

Use PowerShell and `.\gradlew.bat`. Use task dependencies where appropriate, set a sensible working directory, and configure client-launch tasks as background or long-running processes using the Gradle plugin's supported task behavior.

Example baseline task:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Minecraft: Build All",
      "type": "shell",
      "command": ".\\gradlew.bat",
      "args": ["build"],
      "options": {
        "cwd": "${workspaceFolder}"
      },
      "problemMatcher": []
    },
    {
      "label": "Minecraft: Run Client",
      "type": "shell",
      "command": ".\\gradlew.bat",
      "args": ["runClient"],
      "options": {
        "cwd": "${workspaceFolder}"
      },
      "isBackground": true,
      "problemMatcher": []
    }
  ]
}
```

Treat this as a baseline only. Update the tasks to use the actual multi-loader task names and ensure the selected client task is usable in VS Code. Keep the build task non-background so VS Code can report its completion accurately.

## 7. Automatic JAR export to MultiMC

After a successful build, automatically copy the distributable JAR for each supported loader into the configured MultiMC instance's `mods` directory.

The destination must be configurable. Do not invent a local path.

Use a Gradle property such as:

```properties
# gradle.properties — local configuration
multimcModsDir=C:/REPLACE_WITH_YOUR_INSTANCE_PATH/mods
```

The user must replace the placeholder with the actual instance's `mods` directory. Keep machine-specific values out of shared version control; preferably load the local value from an untracked `gradle-local.properties` file or a Windows environment variable.

Deployment requirements:

1. Build the required loader modules successfully before copying anything.
2. Copy only the final distributable mod JARs, not source JARs, Javadocs, development JARs, or intermediate artifacts.
3. Select the correct JAR for the intended loader.
4. Fail with a clear error if the destination is unset, invalid, or does not exist.
5. Never silently create a directory at an incorrect destination.
6. Replace an existing JAR with the same artifact filename only after a successful build.
7. Avoid deleting unrelated mods or JARs from the instance.
8. Avoid copying the same artifact more than once.
9. Report the exact destination and copied filenames.
10. Support a separate build-only task that never modifies the game instance.

Implement this as a dedicated Gradle deployment task that depends on the appropriate successful build tasks. Ensure it cannot execute after a failed build.

Suggested task names:
- `buildAllMods`
- `deployFabricToMultiMC`
- `deployNeoForgeToMultiMC`
- `deployForgeToMultiMC` (if supported)
- `buildAndDeploy`

Use the actual module task names and output directories in the implementation. Do not hardcode assumptions about the generated JAR location without inspecting the configured Gradle plugins.

## 8. Safety and reliability

- Do not overwrite user source files or unrelated instance files.
- Never modify the MultiMC instance's loader version, game version, or other mods automatically.
- Do not install or launch a different mod loader as part of deployment.
- Do not commit generated build output, local paths, caches, or game logs.
- Keep secrets and local environment settings out of Git.
- If deployment fails, preserve the build artifacts and report the failure clearly.
- Avoid destructive cleanup commands unless explicitly required.

## 9. Completion criteria

A task is complete only when:

- All intended code changes are implemented.
- Supported loader modules compile.
- Relevant tests pass.
- The development client launches when required and feasible.
- Final JARs are generated for supported loaders.
- Deployment succeeds when configured and requested.
- No unrelated files or instance contents are changed.
- VSCode tasks are tested once completed.
- The final report includes:
  - Summary of implementation.
  - Files changed.
  - Build and test results.
  - Generated JAR paths.
  - MultiMC deployment destination.
  - Known limitations or failed checks.

Prioritize correctness, maintainability, loader compatibility, and safe automation over shortcuts.
