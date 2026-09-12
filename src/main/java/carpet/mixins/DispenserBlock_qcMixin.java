package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DispenserBlock.class)
public class DispenserBlock_qcMixin {

    @WrapOperation(
        method = "neighborChanged",
        at = @At(
            value = "INVOKE",
            ordinal = 1,
            target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean carpet$hasQuasiSignal(
            Level queriedLevel,
            BlockPos vanillaAbove,
            Operation<Boolean> original,
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            Orientation neighborOrientation,
            boolean movedByPiston
    ) {
        int range = CarpetSettings.quasiConnectivity;
        if (range <= 0) {
            // Carpet explicitly allows disabling vanilla quasi-connectivity.
            return false;
        }

        // Preserve the normal first-above-block check through the complete
        // operation chain so NeoForge/other mixins can still participate.
        if (original.call(queriedLevel, vanillaAbove)) {
            return true;
        }

        // Vanilla already checked distance 1 through the wrapped operation.
        // Carpet only adds the configured extra vertical range here.
        for (int distance = 2; distance <= range; distance++) {
            BlockPos candidate = pos.above(distance);
            if (level.isOutsideBuildHeight(candidate)) {
                break;
            }
            if (level.hasNeighborSignal(candidate)) {
                return true;
            }
        }

        return false;
    }
}
