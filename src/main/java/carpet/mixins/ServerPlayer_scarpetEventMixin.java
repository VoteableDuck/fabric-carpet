package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import carpet.script.EntityEventsGroup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_CHANGES_DIMENSION;
import static carpet.script.CarpetEventServer.Event.PLAYER_DIES;
import static carpet.script.CarpetEventServer.Event.PLAYER_FINISHED_USING_ITEM;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_scarpetEventMixin extends Player implements ServerPlayerInterface
{
    @Unique
    private boolean isInvalidReference = false;

    public ServerPlayer_scarpetEventMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Shadow public boolean wonGame;

    @WrapOperation(method = "completeUsingItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;completeUsingItem()V"
    ))
    private void finishedUsingItem(ServerPlayer playerEntity, Operation<Void> original)
    {
        if (PLAYER_FINISHED_USING_ITEM.isNeeded())
        {
            InteractionHand hand = getUsedItemHand();
            if (!PLAYER_FINISHED_USING_ITEM.onItemAction((ServerPlayer) (Object)this, hand, getUseItem())) {
                original.call(playerEntity);
            }
        }
        else
        {
            original.call(playerEntity);
        }
    }

    /**
     * NeoForge can cancel player death before vanilla ServerPlayer death
     * handling runs. Observe the result of the existing hook instead of
     * posting another LivingDeathEvent. Scarpet must only report a death that
     * NeoForge allowed, and Carpet fake players need the same result so their
     * extra disconnect cleanup does not override another mod's cancellation.
     */
    @WrapOperation(method = "die", at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z",
            remap = false
    ))
    private boolean handleNeoForgeDeathCancellation(LivingEntity entity, DamageSource source, Operation<Boolean> original)
    {
        boolean cancelled = original.call(entity, source);

        if (entity instanceof EntityPlayerMPFake fakePlayer)
        {
            fakePlayer.setNeoForgeDeathCancelled(cancelled);
        }

        if (!cancelled)
        {
            ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.Event.ON_DEATH, source.getMsgId());
            if (PLAYER_DIES.isNeeded())
            {
                PLAYER_DIES.onPlayerEvent((ServerPlayer) (Object)this);
            }
        }

        return cancelled;
    }

    private Vec3 previousLocation;
    private ResourceKey<Level> previousDimension;

    @Inject(method = "teleport", at = @At("HEAD"))
    private void logPreviousCoordinates(TeleportTransition serverWorld, CallbackInfoReturnable<Entity> cir)
    {
        previousLocation = position();
        previousDimension = level().dimension();
    }

    @Inject(method = "teleport", at = @At("RETURN"))
    private void atChangeDimension(TeleportTransition destinationP, CallbackInfoReturnable<Entity> cir)
    {
        // NeoForge can cancel dimension travel before vanilla runs by returning
        // null from ServerPlayer#teleport. Do not report a Scarpet dimension
        // change when no teleport actually took place.
        if (cir.getReturnValue() == null)
        {
            return;
        }

        if (PLAYER_CHANGES_DIMENSION.isNeeded())
        {
            ServerPlayer player = (ServerPlayer) (Object)this;
            ServerLevel destination = destinationP.newLevel();
            Vec3 to = null;
            if (!wonGame || previousDimension != Level.END || destination.dimension() != Level.OVERWORLD)
            {
                to = position();
            }
            PLAYER_CHANGES_DIMENSION.onDimensionChange(player, previousLocation, to, previousDimension, destination.dimension());
        }
    }

    @Override
    public void invalidateEntityObjectReference()
    {
        isInvalidReference = true;
    }

    @Override
    public boolean isInvalidEntityObject()
    {
        return isInvalidReference;
    }
}
