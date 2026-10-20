package com.igxxd.globalsearch.client;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.gui.GlobalSearchScreen;
import com.igxxd.globalsearch.search.SearchIndexer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

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

        // Scan registry at end of mod loaders / client start
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Constants.LOG.info("Global Search: Client started, initiating background indexing...");
            CompletableFuture.runAsync(() -> {
                try {
                    SearchIndexer.getOrIndex(client);
                } catch (Throwable t) {
                    Constants.LOG.error("Failed to perform initial search indexing", t);
                }
            });
        });

        // Register /globalsearch and /search commands with 'rescan' subcommand
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("globalsearch")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.gui.setScreen(new GlobalSearchScreen(client.gui.screen())));
                        return 1;
                    })
                    .then(ClientCommands.literal("rescan")
                            .executes(context -> {
                                Minecraft client = Minecraft.getInstance();
                                int count = SearchIndexer.rescan(client);
                                if (client.player != null) {
                                    client.player.sendSystemMessage(Component.literal("Global Search: Rescanned & indexed " + count + " entries.")
                                            .withStyle(ChatFormatting.GREEN));
                                }
                                return 1;
                            })));

            dispatcher.register(ClientCommands.literal("search")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.gui.setScreen(new GlobalSearchScreen(client.gui.screen())));
                        return 1;
                    })
                    .then(ClientCommands.literal("rescan")
                            .executes(context -> {
                                Minecraft client = Minecraft.getInstance();
                                int count = SearchIndexer.rescan(client);
                                if (client.player != null) {
                                    client.player.sendSystemMessage(Component.literal("Global Search: Rescanned & indexed " + count + " entries.")
                                            .withStyle(ChatFormatting.GREEN));
                                }
                                return 1;
                            })));
        });
    }
}
