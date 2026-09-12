package carpet.mixins;

import carpet.fakes.ServerPlayerInteractionManagerInterface;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_BREAK_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_INTERACTS_WITH_BLOCK;


@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameMode_scarpetEventsMixin implements ServerPlayerInteractionManagerInterface
{
    @Shadow public ServerPlayer player;

    @Shadow private boolean isDestroyingBlock;

    @Shadow private BlockPos destroyPos;

    @Shadow private int lastSentState;

    @Shadow public ServerLevel level;

    /**
     * NeoForge 1.21.11 replaces vanilla's direct Level#removeBlock call in
     * destroyBlock with a patched ServerPlayerGameMode#removeBlock helper and
     * invokes that helper from both the no-drops and normal harvesting paths.
     * Keep a per-invocation cancellation bit so we can preserve Carpet's
     * original destroyBlock=false contract without depending on NeoForge's
     * changed local-variable layout.
     */
    private boolean carpet$cancelBlockBreak;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void carpet$resetBlockBreakCancellation(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir)
    {
        carpet$cancelBlockBreak = false;
    }

    @WrapOperation(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean onBlockBroken(ServerPlayerGameMode instance, BlockPos blockPos, BlockState blockState, boolean canHarvest, ItemStack toolStack, Operation<Boolean> original)
    {
        if (PLAYER_BREAK_BLOCK.onBlockBroken(player, blockPos, blockState))
        {
            carpet$cancelBlockBreak = true;
            this.level.sendBlockUpdated(blockPos, blockState, blockState, 3);
            return false;
        }
        return original.call(instance, blockPos, blockState, canHarvest, toolStack);
    }

    @ModifyReturnValue(method = "destroyBlock", at = @At("RETURN"))
    private boolean carpet$preserveCancelledBreakResult(boolean original)
    {
        return carpet$cancelBlockBreak ? false : original;
    }

    @Inject(method = "useItemOn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/criterion/ItemUsedOnLocationTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
    ))
    private void onBlockActivated(ServerPlayer serverPlayerEntity, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir)
    {
        PLAYER_INTERACTS_WITH_BLOCK.onBlockHit(player, hand, hitResult);
    }

    @Override
    public BlockPos getCurrentBreakingBlock()
    {
        if (!isDestroyingBlock) return null;
        return destroyPos;
    }

    @Override
    public int getCurrentBlockBreakingProgress()
    {
        if (!isDestroyingBlock) return -1;
        return lastSentState;
    }

    @Override
    public void setBlockBreakingProgress(int progress)
    {
        lastSentState = Mth.clamp(progress, -1, 10);
        level.destroyBlockProgress(-1*this.player.getId(), destroyPos, lastSentState);
    }
}
