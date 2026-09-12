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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge bootstrap for Carpet. One-time mod loading, NeoForge networking and
 * server-start lifecycle live here; hot game-loop hooks remain in Carpet's
 * existing mixins.
 */
@Mod("carpet")
public final class CarpetNeoForge {
    public CarpetNeoForge(IEventBus modEventBus) {
        // Run after all mod constructors, matching Fabric's entrypoint ordering
        // closely enough for Carpet extensions to register themselves first.
        modEventBus.addListener(this::onLoadComplete);
        modEventBus.addListener(this::registerPayloads);

        // NeoForge owns server construction/world-start timing. Using its events
        // avoids firing Carpet startup too early from MinecraftServer.loadLevel,
        // which is especially important when restoring persistent fake players.
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        CarpetServer.onGameStarted();

        // Fabric used a dedicated-server entrypoint for the optional rule-dump
        // CLI. It must run after CarpetServer.onGameStarted initializes rules.
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            new CarpetRulePrinter().onInitializeServer();
        }
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        CarpetServer.onServerLoaded(event.getServer());
    }

    private void onServerStarting(ServerStartingEvent event) {
        CarpetServer.onServerLoadedWorlds(event.getServer());
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
