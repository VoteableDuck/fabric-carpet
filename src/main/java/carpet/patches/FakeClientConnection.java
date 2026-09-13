package carpet.patches;

import org.jspecify.annotations.Nullable;

import carpet.fakes.ClientConnectionInterface;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;

public class FakeClientConnection extends Connection
{
    public FakeClientConnection(PacketFlow p)
    {
        super(p);
        // Keep a real open Netty channel so vanilla/NeoForge connection checks
        // consider fake players connected, while discarding traffic that has no
        // remote peer to consume it.
        ((ClientConnectionInterface)this).setChannel(new FakePlayerChannel());

        // Carpet fake players bypass NeoForge's configuration phase and enter
        // PlayerList directly. Mirror initializeOtherConnection's metadata so
        // NeoForge APIs which inspect the Netty channel still see a complete,
        // vanilla-style connection instead of null connection attributes.
        ChannelAttributes.setPayloadSetup(this, NetworkPayloadSetup.empty());
        ChannelAttributes.setConnectionType(this, ConnectionType.OTHER);
    }

    @Override
    public void setReadOnly()
    {
    }
    
    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean bl)
    {
    }

    @Override
    public void handleDisconnection()
    {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener)
    {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener)
    {
    }
}
