package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.class)
public class Explosion_xpFromBlocksMixin {

    @WrapOperation(method = "onExplosionHit", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"
    ))
    private void spawnXPAfterBreak(BlockState instance, ServerLevel serverLevel, BlockPos blockPos, ItemStack itemStack, boolean b,
                                   Operation<Void> original)
    {
        original.call(instance, serverLevel, blockPos, itemStack, b || CarpetSettings.xpFromExplosions);
    }
}
