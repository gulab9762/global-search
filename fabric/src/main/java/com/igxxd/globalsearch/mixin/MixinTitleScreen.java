package com.igxxd.globalsearch.mixin;

import com.igxxd.globalsearch.Constants;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        Constants.LOG.info("Global Search Fabric TitleScreen mixin executed!");
        Constants.LOG.info("Minecraft Version: {}", SharedConstants.getCurrentVersion().name());
    }
}
