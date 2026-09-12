package carpet.patches;

import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Embedded channel used by Carpet fake players on NeoForge.
 *
 * NeoForge may write additional login/play networking messages through the
 * connection's Netty channel. Fake players do not have a real remote peer, so
 * retaining those messages in EmbeddedChannel's inbound/outbound queues would
 * accumulate objects that can never be consumed. Flush them immediately while
 * keeping the channel open so vanilla/Carpet connection checks still treat the
 * fake player as connected.
 */
public class FakePlayerChannel extends EmbeddedChannel
{
    @Override
    protected void handleOutboundMessage(Object msg)
    {
        this.flushOutbound();
    }

    @Override
    protected void handleInboundMessage(Object msg)
    {
        this.flushInbound();
    }
}
