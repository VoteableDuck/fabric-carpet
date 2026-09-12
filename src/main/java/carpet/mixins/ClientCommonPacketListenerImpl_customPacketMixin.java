package carpet.mixins;

import carpet.network.CarpetClient;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge owns custom-payload dispatch. Carpet still needs the vanilla client
 * disconnect lifecycle hook to clear its connection state.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImpl_customPacketMixin
{
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onCMDisconnected(DisconnectionDetails reason, CallbackInfo ci)
    {
        CarpetClient.disconnect();
    }
}
