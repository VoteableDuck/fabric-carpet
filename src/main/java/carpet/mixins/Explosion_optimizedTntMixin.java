package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.OptimizedExplosion;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.ExplosionLogHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(value = ServerExplosion.class)
public abstract class Explosion_optimizedTntMixin
{
    @Shadow @Final private ServerLevel level;

    @Shadow @Nullable public abstract LivingEntity getIndirectSourceEntity();

    private ExplosionLogHelper eLogger;

    @Inject(method = "calculateExplodedPositions", at = @At("HEAD"),
            cancellable = true)
    private void calculateExplodedPositionsCM(final CallbackInfoReturnable<List<BlockPos>> cir)
    {
        if (CarpetSettings.optimizedTNT && !level.isClientSide() && !(getIndirectSourceEntity() instanceof Breeze))
        {
            cir.setReturnValue(OptimizedExplosion.doExplosionA((Explosion) (Object) this, eLogger));
        }
    }

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void interactWithBlocksCM(final List<BlockPos> list, final CallbackInfo ci)
    {
        if (eLogger != null)
        {
            eLogger.setAffectBlocks(!list.isEmpty());
            eLogger.onExplosionDone(this.level.getGameTime());
        }
        if (CarpetSettings.explosionNoBlockDamage)
        {
            list.clear();
        }
    }

    // Optional due to Overwrite in Lithium. If block damage is disabled, use
    // bedrock resistance; otherwise preserve the complete wrapped operation.
    @WrapOperation(method = "calculateExplodedPositions", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getBlockExplosionResistance(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Ljava/util/Optional;"))
    private Optional<Float> noBlockCalcsWithNoBlockDamage(
            ExplosionDamageCalculator instance,
            Explosion explosion,
            BlockGetter blockGetter,
            BlockPos blockPos,
            BlockState blockState,
            FluidState fluidState,
            Operation<Optional<Float>> original)
    {
        if (CarpetSettings.explosionNoBlockDamage)
            return Optional.of(Blocks.BEDROCK.getExplosionResistance());
        return original.call(instance, explosion, blockGetter, blockPos, blockState, fluidState);
    }

    @Inject(method = "<init>",
            at = @At(value = "RETURN"))
    private void onExplostion(ServerLevel world, Entity entity, DamageSource damageSource, final ExplosionDamageCalculator explosionBehavior, final Vec3 vec3, final float power, final boolean createFire, final Explosion.BlockInteraction destructionType, final CallbackInfo ci)
    {
        if (LoggerRegistry.__explosions && !world.isClientSide())
        {
            eLogger = new ExplosionLogHelper(vec3.x, vec3.y, vec3.z, power, createFire, destructionType, level.registryAccess());
        }
    }

    /**
     * NeoForge 1.21.11 routes entity damage through hurtEntities(List<BlockPos>)
     * and adjusts the knockback vector through its explosion event immediately
     * before Entity.push. Wrap that exact call so Carpet observes the final
     * vector without replacing NeoForge or another mod's operation.
     */
    @WrapOperation(
            method = "hurtEntities(Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private void setVelocityAndUpdateLogging(Entity entity, Vec3 velocity, Operation<Void> original)
    {
        if (eLogger != null)
        {
            eLogger.onEntityImpacted(entity, velocity.subtract(entity.getDeltaMovement()));
        }
        original.call(entity, velocity);
    }
}
