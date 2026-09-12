package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class Player_parrotMixin extends LivingEntity
{
    protected Player_parrotMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Player;removeEntitiesOnShoulder()V"))
    private void cancelDropShoulderEntities2(Player playerEntity, Operation<Void> original)
    {
        if (!CarpetSettings.persistentParrots) {
            original.call(playerEntity);
        }
    }
}
