package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Level.class)
public abstract class Level_fillUpdatesMixin
{
    private static final String NEOFORGE_MARK_AND_NOTIFY = "markAndNotifyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;II)V";

    /*
     * NeoForge splits the client/physics notification tail out of vanilla's
     * Level#setBlock(BlockPos, BlockState, int, int) into markAndNotifyBlock.
     * The UPDATE_KNOWN_SHAPE (16) check therefore lives in this method in the
     * production-patched game, rather than in setBlock as it does in vanilla.
     */
    @ModifyConstant(
            method = NEOFORGE_MARK_AND_NOTIFY,
            constant = @Constant(intValue = 16)
    )
    private int addFillUpdatesInt(int original)
    {
        return CarpetSettings.impendingFillSkipUpdates.get() ? -1 : original;
    }

    /*
     * Same NeoForge split as above: neighbour notification is performed from
     * markAndNotifyBlock. Wrap the patched call so other NeoForge/mod hooks can
     * still compose with it whenever Carpet is not suppressing fill updates.
     */
    @WrapOperation(
            method = NEOFORGE_MARK_AND_NOTIFY,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
            )
    )
    private void updateNeighborsMaybe(Level world, BlockPos blockPos, Block block, Operation<Void> original)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
        {
            original.call(world, blockPos, block);
        }
    }
}
