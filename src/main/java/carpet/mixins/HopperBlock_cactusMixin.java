package carpet.mixins;

import carpet.helpers.BlockRotator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HopperBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HopperBlock.class)
public class HopperBlock_cactusMixin
{
    @WrapOperation(method = "getStateForPlacement", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/context/BlockPlaceContext;getClickedFace()Lnet/minecraft/core/Direction;"
    ))
    private Direction getOppositeOpposite(BlockPlaceContext context, Operation<Direction> original)
    {
        Direction clickedFace = original.call(context);
        if (BlockRotator.flippinEligibility(context.getPlayer()))
        {
            return clickedFace.getOpposite();
        }
        return clickedFace;
    }
}
