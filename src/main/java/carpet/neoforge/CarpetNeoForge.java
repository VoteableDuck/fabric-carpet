package carpet.neoforge;

import carpet.CarpetServer;
import carpet.network.CarpetClient;
import carpet.network.ServerNetworkHandler;
import carpet.utils.CarpetRulePrinter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge bootstrap for Carpet. Most of Carpet's game lifecycle stays in its
 * existing mixins; loader lifecycle and custom-payload registration live here.
 */
@Mod("carpet")
public final class CarpetNeoForge {
    public CarpetNeoForge(IEventBus modEventBus) {
        // Run after all mod constructors, matching Fabric's entrypoint ordering
        // closely enough for Carpet extensions to register themselves first.
        modEventBus.addListener(this::onLoadComplete);
        modEventBus.addListener(this::registerPayloads);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        CarpetServer.onGameStarted();

        // Fabric used a dedicated-server entrypoint for the optional rule-dump
        // CLI. It must run after CarpetServer.onGameStarted initializes rules.
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            new CarpetRulePrinter().onInitializeServer();
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Carpet is intentionally optional on the remote side: server-only
        // Carpet must continue to accept vanilla and non-Carpet clients.
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playBidirectional(
                CarpetClient.CarpetPayload.TYPE,
                CarpetClient.CarpetPayload.STREAM_CODEC,
                (payload, context) -> ServerNetworkHandler.onClientData(
                        (ServerPlayer) context.player(), payload.data())
        );
    }
}
