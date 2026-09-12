package carpet.neoforge;

import carpet.network.CarpetClient;
import carpet.network.ClientNetworkHandler;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Physical-client-only NeoForge hooks. Keeping this class side-gated avoids
 * loading Minecraft client classes on a dedicated server.
 */
@EventBusSubscriber(modid = "carpet", value = Dist.CLIENT)
public final class CarpetNeoForgeClient {
    private CarpetNeoForgeClient() {
    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
                CarpetClient.CarpetPayload.TYPE,
                (payload, context) -> ClientNetworkHandler.onServerData(
                        payload.data(), (LocalPlayer) context.player())
        );
    }
}
