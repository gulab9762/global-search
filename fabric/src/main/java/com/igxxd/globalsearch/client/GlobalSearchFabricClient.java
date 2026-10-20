package com.igxxd.globalsearch.client;

import com.igxxd.globalsearch.Constants;
import net.fabricmc.api.ClientModInitializer;

public class GlobalSearchFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Initializing {} Fabric client...", Constants.MOD_NAME);
    }
}
