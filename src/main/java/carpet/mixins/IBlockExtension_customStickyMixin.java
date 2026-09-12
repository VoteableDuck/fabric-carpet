package carpet.mixins;

import carpet.fakes.BlockPistonBehaviourInterface;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge replaces PistonStructureResolver's vanilla sticky helpers with
 * IBlockExtension hooks. Bridge Carpet's custom piston behaviour interface to
 * that hook so Carpet sticky blocks remain visible to NeoForge's resolver.
 */
@Mixin(IBlockExtension.class)
public interface IBlockExtension_customStickyMixin
{
    @Inject(method = "isStickyBlock", at = @At("HEAD"), cancellable = true)
    private static void carpet$isStickyBlock(BlockState state, CallbackInfoReturnable<Boolean> cir)
    {
        if (state.getBlock() instanceof BlockPistonBehaviourInterface behaviour)
        {
            cir.setReturnValue(behaviour.isSticky(state));
        }
    }
}
