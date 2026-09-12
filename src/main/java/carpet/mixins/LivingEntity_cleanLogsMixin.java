package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_cleanLogsMixin extends Entity
{

    public LivingEntity_cleanLogsMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    @WrapOperation(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasCustomName()Z"))
    private boolean shouldLogDeaths(LivingEntity livingEntity, Operation<Boolean> original)
    {
        return original.call(livingEntity)
                && livingEntity.level() instanceof ServerLevel serverLevel
                && CarpetSettings.cleanLogs
                && serverLevel.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES);
    }
}
