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
public abstract class Player_creativeNoClipMixin extends LivingEntity
{
    protected Player_creativeNoClipMixin(EntityType<? extends LivingEntity> type, Level world)
    {
        super(type, world);
    }

    private boolean carpet$canNoClip(Player playerEntity, Operation<Boolean> original)
    {
        return original.call(playerEntity)
                || (CarpetSettings.creativeNoClip && playerEntity.isCreative() && playerEntity.getAbilities().flying);
    }

    @WrapOperation(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
    )
    private boolean canClipThroughWorld(Player playerEntity, Operation<Boolean> original)
    {
        return carpet$canNoClip(playerEntity, original);
    }

    @WrapOperation(method = "aiStep", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
    )
    private boolean collidesWithEntities(Player playerEntity, Operation<Boolean> original)
    {
        return carpet$canNoClip(playerEntity, original);
    }

    @WrapOperation(method = "updatePlayerPose", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
    )
    private boolean spectatorsDontPose(Player playerEntity, Operation<Boolean> original)
    {
        return carpet$canNoClip(playerEntity, original);
    }
}
