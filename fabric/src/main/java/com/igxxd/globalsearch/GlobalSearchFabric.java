package com.igxxd.globalsearch;

import net.fabricmc.api.ModInitializer;

public class GlobalSearchFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Initializing {} Fabric module...", Constants.MOD_NAME);
        CommonClass.init();
    }
}
