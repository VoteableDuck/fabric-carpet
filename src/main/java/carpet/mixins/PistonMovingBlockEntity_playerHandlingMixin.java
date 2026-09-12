package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntity_playerHandlingMixin
{
    @Inject(method = "moveEntityByPiston", at = @At("HEAD"), cancellable = true)
    private static void dontPushSpectators(Direction direction, Entity entity, double d, Direction direction2, CallbackInfo ci)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof Player player && player.isCreative() && player.getAbilities().flying) ci.cancel();
    }

    @WrapOperation(method = "moveCollidedEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private static void ignoreAccel(Entity entity, double x, double y, double z, Operation<Void> original)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof Player player && player.isCreative() && player.getAbilities().flying) return;
        original.call(entity, x, y, z);
    }

    @WrapOperation(method = "moveCollidedEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;"
    ))
    private static PushReaction moveFakePlayers(Entity entity, Operation<PushReaction> original,
        Level world, BlockPos blockPos, float progress, PistonMovingBlockEntity pistonBlockEntity)
    {
        if (entity instanceof EntityPlayerMPFake && pistonBlockEntity.getMovedState().is(Blocks.SLIME_BLOCK))
        {
            Vec3 velocity = entity.getDeltaMovement();
            double x = velocity.x;
            double y = velocity.y;
            double z = velocity.z;
            Direction direction = pistonBlockEntity.getMovementDirection();
            switch (direction.getAxis()) {
                case X -> x = direction.getStepX();
                case Y -> y = direction.getStepY();
                case Z -> z = direction.getStepZ();
            }

            entity.setDeltaMovement(x, y, z);
        }
        return original.call(entity);
    }
}
