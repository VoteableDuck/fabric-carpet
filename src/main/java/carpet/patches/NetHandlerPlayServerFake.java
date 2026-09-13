package carpet.patches;

import carpet.patches.EntityPlayerMPFake;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class NetHandlerPlayServerFake extends ServerGamePacketListenerImpl
{
    public NetHandlerPlayServerFake(final MinecraftServer minecraftServer, final Connection connection, final ServerPlayer serverPlayer, final CommonListenerCookie i)
    {
        super(minecraftServer, connection, serverPlayer, i);
    }

    /**
     * Carpet fake players have no remote peer and never negotiate NeoForge
     * payload channels. Match NeoForge's own fake-player network handler and
     * report every custom channel as unavailable so mods do not try to send
     * negotiated payloads to an in-process bot.
     */
    @Override
    public boolean hasChannel(Identifier payloadId)
    {
        return false;
    }

    /**
     * There is no client behind a Carpet fake player. Drop outbound packets at
     * the listener boundary, matching NeoForge's FakePlayerNetHandler. This is
     * important on NeoForge because ServerCommonPacketListenerImpl validates
     * custom payload channel negotiation before forwarding to Connection; a
     * mod that sends an optional payload during PlayerLoggedIn/DatapackSync can
     * otherwise throw before Carpet finishes creating the fake player.
     */
    @Override
    public void send(Packet<?> packet)
    {
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener)
    {
    }

    @Override
    public void disconnect(Component message)
    {
        if (message.getContents() instanceof TranslatableContents text && (text.getKey().equals("multiplayer.disconnect.idling") || text.getKey().equals("multiplayer.disconnect.duplicate_login")))
        {
            ((EntityPlayerMPFake) player).kill(message);
        }
    }

    @Override
    public void teleport(PositionMoveRotation positionMoveRotation, Set<Relative> set)
    {
        super.teleport(positionMoveRotation, set);
        if (player.level().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.level().getChunkSource().move(player);
        }
    }
}
