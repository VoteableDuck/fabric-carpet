package carpet.patches;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;

/**
 * Embedded channel used by Carpet fake players on NeoForge.
 *
 * NeoForge may write additional login/play networking messages through the
 * connection's Netty channel. Fake players do not have a real remote peer, so
 * those messages must be discarded without retaining reference-counted Netty
 * buffers. Keep the channel itself open so vanilla/Carpet connection checks
 * still treat the fake player as connected.
 */
public class FakePlayerChannel extends EmbeddedChannel
{
    @Override
    protected void handleOutboundMessage(Object msg)
    {
        ReferenceCountUtil.release(msg);
    }

    @Override
    protected void handleInboundMessage(Object msg)
    {
        ReferenceCountUtil.release(msg);
    }
}
