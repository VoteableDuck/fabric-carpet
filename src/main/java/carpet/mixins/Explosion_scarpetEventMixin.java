package carpet.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static carpet.script.CarpetEventServer.Event.EXPLOSION_OUTCOME;

@Mixin(value = ServerExplosion.class, priority = 990)
public abstract class Explosion_scarpetEventMixin
{
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Vec3 center;
    @Shadow @Final private float radius;
    @Shadow @Final private boolean fire;
    @Shadow @Final private Explosion.BlockInteraction blockInteraction;
    @Shadow @Final private @Nullable Entity source;

    @Shadow /*@Nullable*/ public abstract /*@Nullable*/ LivingEntity getIndirectSourceEntity();

    private final List<Entity> affectedEntities = new ArrayList<>();

    @Inject(method = "explode", at = @At("HEAD"))
    private void explodeCM(CallbackInfoReturnable<Integer> cir)
    {
        affectedEntities.clear();
    }

    /**
     * NeoForge 1.21.11 moved the real entity-damage path into
     * hurtEntities(List<BlockPos>). Wrap the callback instead of redirecting it
     * so NeoForge and other mods can compose with Carpet's affected-entity log.
     */
    @WrapOperation(
            method = "hurtEntities(Ljava/util/List;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;onExplosionHit(Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void onEntityHit(Entity instance, Entity entity, Operation<Void> original)
    {
        affectedEntities.add(instance);
        original.call(instance, entity);
    }

    /**
     * NeoForge's explode() invokes hurtEntities(List<BlockPos>) with the same
     * exploded-position list that its detonation event receives. Wrapping that
     * invocation gives Scarpet the actual block list without brittle local
     * capture and fires the outcome only after entity damage has completed.
     */
    @WrapOperation(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ServerExplosion;hurtEntities(Ljava/util/List;)V"
            )
    )
    private void onExplosionDone(ServerExplosion instance, List<BlockPos> blocks, Operation<Void> original)
    {
        original.call(instance, blocks);
        if (EXPLOSION_OUTCOME.isNeeded() && !level.isClientSide())
        {
            EXPLOSION_OUTCOME.onExplosion(level, source, this::getIndirectSourceEntity, center, radius, fire, blocks, affectedEntities, blockInteraction);
        }
    }
}
