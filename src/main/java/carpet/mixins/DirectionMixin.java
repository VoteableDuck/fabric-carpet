package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.EntityInterface;
import carpet.helpers.BlockRotator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Direction.class)
public abstract class DirectionMixin
{
    @WrapOperation(method = "orderedByNearest", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private static float getYaw(Entity entity, float partialTick, Operation<Float> original)
    {
        float yaw;
        if (!CarpetSettings.placementRotationFix)
        {
            yaw = original.call(entity, partialTick);
        }
        else
        {
            yaw = ((EntityInterface) entity).getMainYaw(partialTick);
        }
        if (BlockRotator.flippinEligibility(entity))
        {
            yaw += 180f;
        }
        return yaw;
    }

    @WrapOperation(method = "orderedByNearest", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private static float getPitch(Entity entity, float partialTick, Operation<Float> original)
    {
        float pitch = original.call(entity, partialTick);
        if (BlockRotator.flippinEligibility(entity))
        {
            pitch = -pitch;
        }
        return pitch;
    }
}
