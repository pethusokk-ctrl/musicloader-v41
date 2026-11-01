package com.musicloader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicLoaderClient implements ClientModInitializer {
    public static final String MOD_ID = "musicloader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        MusicLibrary.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MusicPlayer.shutdown());
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "text"),
                (graphics, tickCounter) -> MusicHud.render(graphics));
        LOGGER.info("Music Loader запущен. Папка: {}", MusicLibrary.DIR);
    }
}
