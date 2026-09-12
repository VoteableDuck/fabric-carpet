package carpet.mixins;

import carpet.helpers.BlockRotator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameMode_cactusMixin
{

    @WrapOperation(method = "useItemOn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
    ))
    private InteractionResult activateWithOptionalCactus(final BlockState blockState, final ItemStack itemStack, final Level world, final Player player, final InteractionHand hand, final BlockHitResult hitResult, Operation<InteractionResult> original)
    {
        boolean flipped = BlockRotator.flipBlockWithCactus(blockState, world, player, hand, hitResult);
        if (flipped)
            return InteractionResult.SUCCESS;

        return original.call(blockState, itemStack, world, player, hand, hitResult);
    }
}
