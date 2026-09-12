package carpet.neoforge;

import carpet.CarpetServer;
import carpet.utils.CarpetRulePrinter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge bootstrap for Carpet. The rest of Carpet keeps using its existing
 * mixin-driven lifecycle so the upstream behavior, including /player fake
 * players, remains unchanged.
 */
@Mod("carpet")
public final class CarpetNeoForge {
    public CarpetNeoForge() {
        CarpetServer.onGameStarted();

        // Fabric used a dedicated-server entrypoint for the optional rule dump
        // CLI. Preserve that behavior without changing CarpetRulePrinter itself.
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            new CarpetRulePrinter().onInitializeServer();
        }
    }
}
