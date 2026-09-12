package carpet.mixins;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Carpet's serverbound custom payload is dispatched through NeoForge's payload
 * registration API. This mixin remains as a stable upstream placeholder so the
 * mixin configuration does not need loader-specific surgery.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerimpl_connectionMixin
{
}
