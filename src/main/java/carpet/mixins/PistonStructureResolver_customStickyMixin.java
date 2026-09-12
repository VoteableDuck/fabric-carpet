package carpet.mixins;

import carpet.fakes.BlockPistonBehaviourInterface;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge 1.21.11 replaces PistonStructureResolver's vanilla isSticky and
 * canStickToEachOther helpers with BlockState/IBlockExtension hooks. Wrap those
 * hooks instead of redirecting removed vanilla methods, while preserving the
 * directional Carpet custom-sticky semantics.
 */
@Mixin(PistonStructureResolver.class)
public class PistonStructureResolver_customStickyMixin
{
    @Shadow @Final private Level level;
    @Shadow @Final private Direction pushDirection;

    @WrapOperation(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            )
    )
    private boolean carpet$canStickBlockLine(
            BlockState state,
            BlockState behindState,
            Operation<Boolean> original,
            @Local(ordinal = 1) BlockPos behindPos)
    {
        if (state.getBlock() instanceof BlockPistonBehaviourInterface behaviour)
        {
            return behaviour.isStickyToNeighbor(
                    level,
                    behindPos.relative(pushDirection),
                    state,
                    behindPos,
                    behindState,
                    pushDirection.getOpposite(),
                    pushDirection
            );
        }

        return original.call(state, behindState);
    }

    // NeoForge checks sticking from both states. Carpet's custom hook already
    // decides the directional relationship, so do not let the mirror check
    // override that decision.
    @WrapOperation(
            method = "addBlockLine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            )
    )
    private boolean carpet$allowMirrorBlockLineCheck(
            BlockState state, BlockState behindState, Operation<Boolean> original)
    {
        return true;
    }

    @WrapOperation(
            method = "addBranchingBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            )
    )
    private boolean carpet$canStickBranch(
            BlockState neighborState,
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos pos,
            @Local(ordinal = 1) BlockPos neighborPos,
            @Local Direction direction)
    {
        if (state.getBlock() instanceof BlockPistonBehaviourInterface behaviour)
        {
            return behaviour.isStickyToNeighbor(
                    level, pos, state, neighborPos, neighborState, direction, pushDirection
            );
        }

        return original.call(neighborState, state);
    }

    @WrapOperation(
            method = "addBranchingBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canStickTo(Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            )
    )
    private boolean carpet$allowMirrorBranchCheck(
            BlockState neighborState, BlockState state, Operation<Boolean> original)
    {
        return true;
    }
}
