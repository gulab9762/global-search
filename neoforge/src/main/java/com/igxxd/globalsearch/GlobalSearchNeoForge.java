package com.igxxd.globalsearch;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Constants.MOD_ID)
public class GlobalSearchNeoForge {

    public GlobalSearchNeoForge(IEventBus modEventBus) {
        Constants.LOG.info("Initializing {} for NeoForge...", Constants.MOD_NAME);
        CommonClass.init();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Constants.LOG.info("Global Search NeoForge Common Setup executed.");
    }
}
