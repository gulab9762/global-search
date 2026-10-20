package com.igxxd.globalsearch.client;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSearchScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class GlobalSearchFabricClient implements ClientModInitializer {

    public static KeyMapping OPEN_SEARCH_KEY;

    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Registering keybind and commands for Global Search Fabric...");

        // Register Keybind
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("globalsearch", "general"));
        OPEN_SEARCH_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.globalsearch.open",
                InputConstants.KEY_K,
                category
        ));

        // Client tick to check keybind press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_SEARCH_KEY != null && OPEN_SEARCH_KEY.consumeClick()) {
                client.gui.setScreen(new GlobalSearchScreen(client.gui.screen()));
            }
        });

        // Register /globalsearch and /search commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("globalsearch")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.gui.setScreen(new GlobalSearchScreen(client.gui.screen())));
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("search")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.gui.setScreen(new GlobalSearchScreen(client.gui.screen())));
                        return 1;
                    }));
        });
    }
}
