package carpet.mixins;

import carpet.helpers.BlockRotator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(UseOnContext.class)
public class UseOnContext_cactusMixin
{
    @WrapOperation(method = "getHorizontalDirection", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDirection()Lnet/minecraft/core/Direction;"
    ))
    private Direction getPlayerFacing(Player playerEntity, Operation<Direction> original)
    {
        Direction dir = original.call(playerEntity);
        if (BlockRotator.flippinEligibility(playerEntity))
        {
            dir = dir.getOpposite();
        }
        return dir;
    }
}
