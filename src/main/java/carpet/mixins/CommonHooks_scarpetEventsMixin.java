package carpet.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

import static carpet.script.CarpetEventServer.Event.PLAYER_PLACES_BLOCK;

/**
 * NeoForge captures item-driven block changes and only commits them after its
 * cancellable EntityPlaceEvent / EntityMultiPlaceEvent has accepted the
 * transaction. Carpet's Fabric hook inside BlockItem#place runs before that
 * decision, so it would report placements that NeoForge later rolls back.
 */
@Mixin(value = CommonHooks.class, remap = false)
public abstract class CommonHooks_scarpetEventsMixin
{
    @WrapOperation(
            method = "onPlaceItemIntoWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/EventHooks;onBlockPlace(Lnet/minecraft/world/entity/Entity;Lnet/neoforged/neoforge/common/util/BlockSnapshot;Lnet/minecraft/core/Direction;)Z",
                    remap = false
            )
    )
    private static boolean carpet$afterBlockPlaceEvent(Entity entity, BlockSnapshot snapshot, Direction side, Operation<Boolean> original,
                                                        @Local(argsOnly = true) UseOnContext context)
    {
        boolean cancelled = original.call(entity, snapshot, side);
        if (!cancelled)
        {
            carpet$dispatchCommittedBlockPlacement(entity, snapshot, context);
        }
        return cancelled;
    }

    @WrapOperation(
            method = "onPlaceItemIntoWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/EventHooks;onMultiBlockPlace(Lnet/minecraft/world/entity/Entity;Ljava/util/List;Lnet/minecraft/core/Direction;)Z",
                    remap = false
            )
    )
    private static boolean carpet$afterMultiBlockPlaceEvent(Entity entity, List<BlockSnapshot> snapshots, Direction side, Operation<Boolean> original,
                                                             @Local(argsOnly = true) UseOnContext context)
    {
        boolean cancelled = original.call(entity, snapshots, side);
        if (!cancelled && !snapshots.isEmpty())
        {
            // NeoForge itself uses the first snapshot as the primary placement.
            carpet$dispatchCommittedBlockPlacement(entity, snapshots.getFirst(), context);
        }
        return cancelled;
    }

    private static void carpet$dispatchCommittedBlockPlacement(Entity entity, BlockSnapshot snapshot, UseOnContext context)
    {
        // Preserve Carpet's original BlockItem-only semantics: CommonHooks also
        // captures block changes caused by other kinds of items.
        if (entity instanceof ServerPlayer player
                && context.getItemInHand().getItem() instanceof BlockItem
                && PLAYER_PLACES_BLOCK.isNeeded())
        {
            PLAYER_PLACES_BLOCK.onBlockPlaced(player, snapshot.getPos(), context.getHand(), context.getItemInHand());
        }
    }
}
