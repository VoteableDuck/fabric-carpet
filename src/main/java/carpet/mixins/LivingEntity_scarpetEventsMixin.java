package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.fakes.LivingEntityInterface;
import carpet.script.EntityEventsGroup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_DEALS_DAMAGE;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_scarpetEventsMixin extends Entity implements LivingEntityInterface
{

    @Shadow protected abstract void jumpFromGround();

    @Shadow protected boolean jumping;

    public LivingEntity_scarpetEventsMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    /**
     * NeoForge can cancel LivingDeathEvent before vanilla death handling starts.
     * Observe the result of the existing hook instead of posting another event,
     * and only report Scarpet's ON_DEATH when NeoForge actually permits death.
     */
    @WrapOperation(method = "die", at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z",
            remap = false
    ))
    private boolean handleNeoForgeDeathCancellation(LivingEntity entity, DamageSource source, Operation<Boolean> original)
    {
        boolean cancelled = original.call(entity, source);
        if (!cancelled)
        {
            ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DEATH, source.getMsgId());
        }
        return cancelled;
    }

    @Inject(method = "actuallyHurt", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
            shift = At.Shift.BEFORE
    ))
    private void entityTakingDamage(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DAMAGE, amount, source);
        // this is not applicable since its not a playr for sure
        //if (entity instanceof ServerPlayerEntity && PLAYER_TAKES_DAMAGE.isNeeded())
        //{
        //    PLAYER_TAKES_DAMAGE.onDamage(entity, float_2, damageSource_1);
        //}
        if (source.getEntity() instanceof ServerPlayer && PLAYER_DEALS_DAMAGE.isNeeded())
        {
            if(PLAYER_DEALS_DAMAGE.onDamage(this, amount, source)) {
                ci.cancel();
            }
        }
    }

    @Override
    public void doJumpCM()
    {
        jumpFromGround();
    }

    @Override
    public boolean isJumpingCM()
    {
        return jumping;
    }
}
