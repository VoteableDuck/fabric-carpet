package carpet.mixins;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;

/**
 * NeoForge registers CarpetPayload through RegisterPayloadHandlersEvent.
 *
 * Upstream Fabric Carpet injects the payload codec directly into vanilla's
 * CustomPacketPayload codec list. Keeping that hack on NeoForge can compete
 * with NeoForge's own registry and process the same payload through two paths,
 * so this mixin intentionally remains an empty compatibility placeholder.
 */
@Mixin(CustomPacketPayload.class)
public interface CustomPacketPayload_networkStuffMixin
{
}
