package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SummonCommand.class)
public class SummonCommand_lightningMixin
{
    @WrapOperation(method = "createEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"
    ))
    private static BlockPos addRiders(Entity entity, Operation<BlockPos> original)
    {
        BlockPos at = original.call(entity);
        // [CM] SummonNaturalLightning - preserve vanilla/NeoForge block-position operation and add natural side effects.
        if (CarpetSettings.summonNaturalLightning && entity instanceof LightningBolt && !entity.level().isClientSide())
        {
            ServerLevel world = (ServerLevel) entity.level();
            DifficultyInstance localDifficulty = world.getCurrentDifficultyAt(at);
            boolean spawnTrap = world.getGameRules().get(GameRules.SPAWN_MOBS)
                    && world.random.nextDouble() < (double) localDifficulty.getEffectiveDifficulty() * 0.01D;
            if (spawnTrap) {
                SkeletonHorse skeletonHorse = EntityType.SKELETON_HORSE.create(world, EntitySpawnReason.EVENT);
                if (skeletonHorse != null) {
                    skeletonHorse.setTrap(true);
                    skeletonHorse.setAge(0);
                    skeletonHorse.setPos(entity.getX(), entity.getY(), entity.getZ());
                    world.addFreshEntity(skeletonHorse);
                }
            }
        }
        return at;
    }
}
