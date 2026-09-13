package carpet.mixins;

import carpet.fakes.ServerWorldInterface;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.EXPLOSION;
import static carpet.script.CarpetEventServer.Event.LIGHTNING;
import static carpet.script.CarpetEventServer.Event.CHUNK_UNLOADED;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_scarpetMixin extends Level implements ServerWorldInterface
{

    protected ServerLevel_scarpetMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i)
    {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    /**
     * Scarpet documents the lightning event as firing after the strike, with the
     * lightning entity (and possible horse trap) already spawned. NeoForge can
     * cancel the lightning entity in EntityJoinLevelEvent, so only publish the
     * Scarpet event after the actual addFreshEntity call succeeds.
     */
    @WrapOperation(method = "tickThunder", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            ordinal = 1
    ))
    private boolean onNaturalLightning(ServerLevel level, Entity entity, Operation<Boolean> original,
                                       @Local BlockPos blockPos, @Local(ordinal = 1) boolean spawnedTrap)
    {
        boolean added = original.call(level, entity);
        if (added && LIGHTNING.isNeeded()) {
            LIGHTNING.onWorldEventFlag(level, blockPos, spawnedTrap ? 1 : 0);
        }
        return added;
    }

    /**
     * NeoForge fires its cancellable ExplosionEvent.Start after constructing the
     * ServerExplosion. Run Scarpet's explosion event only after that hook has
     * allowed the explosion, but still before ServerExplosion#explode applies it.
     * Use the BlockInteraction already calculated by ServerLevel so NeoForge's
     * EntityMobGriefingEvent result is preserved without firing it a second time.
     */
    @Inject(method = "explode", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerExplosion;explode()I",
            shift = At.Shift.BEFORE
    ), cancellable = true)
    private void handleExplosion(Entity entity, DamageSource damageSource, ExplosionDamageCalculator explosionDamageCalculator, double x, double y, double z, float g, boolean bl, ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions2, WeightedList<ExplosionParticleInfo> weightedList, Holder<SoundEvent> holder, CallbackInfo ci, @Local Explosion.BlockInteraction blockInteraction)
    {
        if (EXPLOSION.isNeeded()) {
            boolean cancelled = EXPLOSION.onExplosion((ServerLevel) (Object) this, entity, null, new Vec3(x, y, z), g, bl, null, null, blockInteraction);
            if (cancelled) ci.cancel();
        }
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void handleChunkUnload(LevelChunk levelChunk, CallbackInfo ci)
    {
        if (CHUNK_UNLOADED.isNeeded())
        {
            ServerLevel level = (ServerLevel)((Object)this);
            CHUNK_UNLOADED.onChunkEvent(level, levelChunk.getPos(), false);
        }
    }

    @Final
    @Shadow
    private ServerLevelData serverLevelData;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;

    @Unique
    @Override
    public ServerLevelData getWorldPropertiesCM(){
        return serverLevelData;
    }

    @Unique
    @Override
    public LevelEntityGetter<Entity> getEntityLookupCMPublic() {
        return entityManager.getEntityGetter();
    }
}
