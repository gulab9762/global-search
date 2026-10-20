package com.igxxd.globalsearch;

import com.igxxd.globalsearch.gui.GlobalSearchScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class GlobalSearchNeoForge {

    public static KeyMapping OPEN_SEARCH_KEY;

    public GlobalSearchNeoForge(IEventBus modEventBus) {
        Constants.LOG.info("Initializing {} for NeoForge...", Constants.MOD_NAME);
        CommonClass.init();

        if (FMLLoader.getCurrent().getDist().isClient()) {
            modEventBus.addListener(this::onRegisterKeyMappings);
            NeoForge.EVENT_BUS.addListener(this::onClientTick);
            NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
        }
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("globalsearch", "general"));
        OPEN_SEARCH_KEY = new KeyMapping(
                "key.globalsearch.open",
                InputConstants.KEY_K,
                category
        );
        event.register(OPEN_SEARCH_KEY);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (OPEN_SEARCH_KEY != null && OPEN_SEARCH_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.setScreen(new GlobalSearchScreen(mc.gui.screen()));
        }
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("globalsearch")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> client.gui.setScreen(new GlobalSearchScreen(client.gui.screen())));
                    return 1;
                }));

        event.getDispatcher().register(Commands.literal("search")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> client.gui.setScreen(new GlobalSearchScreen(client.gui.screen())));
                    return 1;
                }));
    }
}
