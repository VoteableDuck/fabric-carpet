package carpet.mixins;

import carpet.network.CarpetClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListener_customPacketsMixin extends ClientCommonPacketListenerImpl
{
    protected ClientPacketListener_customPacketsMixin(final Minecraft minecraft, final Connection connection, final CommonListenerCookie commonListenerCookie)
    {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void onGameJoined(ClientboundLoginPacket packet, CallbackInfo info)
    {
        CarpetClient.gameJoined(minecraft.player);
    }

    // Carpet custom payload dispatch is registered through NeoForge's payload
    // API in CarpetNeoForgeClient. Keeping the old unknown-payload interception
    // here would cause duplicate handling or bypass channel negotiation.
}
