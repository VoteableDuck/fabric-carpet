package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_ATTACKS_ENTITY;
import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_INTERACTS_WITH_ENTITY;
import static carpet.script.CarpetEventServer.Event.PLAYER_TAKES_DAMAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_COLLIDES_WITH_ENTITY;

@Mixin(Player.class)
public abstract class Player_scarpetEventsMixin extends LivingEntity
{
    protected Player_scarpetEventsMixin(EntityType<? extends LivingEntity> type, Level world)
    {
        super(type, world);
    }

    @Inject(method = "actuallyHurt", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
    ))
    private void playerTakingDamage(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfo ci)
    {
        // version of LivingEntity_scarpetEventsMixin::entityTakingDamage
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DAMAGE, amount, source);
        if (PLAYER_TAKES_DAMAGE.isNeeded())
        {
            if (PLAYER_TAKES_DAMAGE.onDamage(this, amount, source))
            {
                ci.cancel();
            }
        }
        if (source.getEntity() instanceof ServerPlayer && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            if (PLAYER_DEALS_DAMAGE.onDamage(this, amount, source))
            {
                ci.cancel();
            }
        }
    }

    @Inject(method = "touch", at = @At("HEAD"))
    private void onEntityCollision(Entity entity, CallbackInfo ci)
    {
        if (PLAYER_COLLIDES_WITH_ENTITY.isNeeded() && !level().isClientSide())
        {
            PLAYER_COLLIDES_WITH_ENTITY.onEntityHandAction((ServerPlayer)(Object)this, entity, null);
        }
    }

    @Inject(method = "interactOn", cancellable = true, at = @At("HEAD"))
    private void doInteract(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (!level().isClientSide() && PLAYER_INTERACTS_WITH_ENTITY.isNeeded())
        {
            if (PLAYER_INTERACTS_WITH_ENTITY.onEntityHandAction((ServerPlayer) (Object)this, entity, hand))
            {
                cir.setReturnValue(InteractionResult.PASS);
                cir.cancel();
            }
        }
    }

    /**
     * NeoForge fires its cancellable AttackEntityEvent at the start of
     * Player#attack. Let that hook decide first so Scarpet does not report an
     * attack another mod has already rejected. Scarpet can still cancel an
     * otherwise accepted attack by making the wrapped hook return false.
     */
    @WrapOperation(method = "attack", at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;onPlayerAttackTarget(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Z",
            remap = false
    ))
    private boolean carpet$afterNeoForgeAttackEvent(Player player, Entity target, Operation<Boolean> original)
    {
        boolean allowed = original.call(player, target);
        if (!allowed)
        {
            return false;
        }
        if (!level().isClientSide() && PLAYER_ATTACKS_ENTITY.isNeeded() && target.isAttackable())
        {
            return !PLAYER_ATTACKS_ENTITY.onEntityHandAction((ServerPlayer)(Object)this, target, null);
        }
        return true;
    }

    @ModifyReturnValue(method = "wantsToStopRiding", at = @At("TAIL"))
    private boolean dontUnmountFromIfPermanentVehicle(boolean original)
    {
        if (this.getVehicle() == null)
        {
            // may also be called when leaving entity camera in spectator
            return original;
        }
        return original && !((EntityInterface) this.getVehicle()).isPermanentVehicle();
    }
}
