package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public class LevelChunk_fillUpdatesMixin
{
    // todo onStateReplaced needs a bit more love since it removes be which is needed
    @WrapOperation(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
    ))
    private void onAdded(BlockState blockState, Level world, BlockPos pos, BlockState oldState, boolean moved, Operation<Void> original)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
        {
            original.call(blockState, world, pos, oldState, moved);
        }
    }

    @WrapOperation(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;preRemoveSideEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
    ))
    private void onPreRemoveSideEffects(BlockEntity blockEntity, BlockPos pos, BlockState state, Operation<Void> original)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
        {
            original.call(blockEntity, pos, state);
        }
    }

    @WrapOperation(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;affectNeighborsAfterRemoval(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Z)V"
    ))
    private void onAffectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved, Operation<Void> original)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
        {
            original.call(state, level, pos, moved);
        }
    }
}
