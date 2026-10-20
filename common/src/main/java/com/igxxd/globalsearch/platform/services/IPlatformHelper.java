package com.igxxd.globalsearch.platform.services;

import java.util.Map;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Resolves human-readable mod name for a given mod id.
     *
     * @param modId The mod ID.
     * @return Display name or mod ID fallback.
     */
    String getModName(String modId);

    /**
     * Returns a map of all loaded mod IDs and their display names.
     *
     * @return Map of modId -> displayName.
     */
    Map<String, String> getLoadedMods();

    /**
     * Resolves human-readable mod description for a given mod id.
     *
     * @param modId The mod ID.
     * @return Description or empty string.
     */
    String getModDescription(String modId);

    /**
     * Opens an InputStream to the mod's icon or logo if available.
     *
     * @param modId The mod ID.
     * @return Optional containing InputStream to the image, or empty.
     */
    java.util.Optional<java.io.InputStream> getModIconStream(String modId);
}

