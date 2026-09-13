package carpet.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_PLACING_BLOCK;

@Mixin(BlockItem.class)
public class BlockItem_scarpetEventMixin
{
    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void beforePlacement(BlockPlaceContext context, BlockState placementState, CallbackInfoReturnable<Boolean> cir)
    {
        if (context.getPlayer() instanceof ServerPlayer player && PLAYER_PLACING_BLOCK.isNeeded())
        {
            if (PLAYER_PLACING_BLOCK.onBlockPlaced(player, context.getClickedPos(), context.getHand(), context.getItemInHand()))
            {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
}
