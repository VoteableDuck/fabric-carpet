package carpet.mixins;

import carpet.script.CarpetEventServer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.StatAwardEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Composes Scarpet's statistic event with NeoForge's cancellable/mutable
 * StatAwardEvent. Scarpet should only observe the statistic change that
 * NeoForge actually accepts, while still firing before StatsCounter stores
 * the new value.
 */
@Mixin(StatsCounter.class)
public abstract class StatsCounter_scarpetEventsMixin
{
    @WrapOperation(method = "setValue", at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/EventHooks;onStatAward(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/stats/Stat;I)Lnet/neoforged/neoforge/event/StatAwardEvent;",
            remap = false
    ))
    private StatAwardEvent carpet$afterNeoForgeStatAward(Player player, Stat<?> stat, int value, Operation<StatAwardEvent> original)
    {
        StatAwardEvent event = original.call(player, stat, value);
        if (!event.isCanceled() && player instanceof ServerPlayer serverPlayer && CarpetEventServer.Event.STATISTICS.isNeeded())
        {
            Stat<?> acceptedStat = event.getStat();
            int previousValue = ((StatsCounter) (Object) this).getValue(acceptedStat);
            int delta = event.getValue() - previousValue;
            if (delta != 0)
            {
                CarpetEventServer.Event.STATISTICS.onPlayerStatistic(serverPlayer, acceptedStat, delta);
            }
        }
        return event;
    }
}
