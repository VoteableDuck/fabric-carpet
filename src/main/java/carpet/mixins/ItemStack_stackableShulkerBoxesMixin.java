package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class ItemStack_stackableShulkerBoxesMixin
{
    @ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
    private int getCMMMaxStackSize(int original)
    {
        ItemStack stack = (ItemStack) (Object) this;
        if (CarpetSettings.shulkerBoxStackSize > 1
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock
                && stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().findAny().isEmpty())
        {
            return CarpetSettings.shulkerBoxStackSize;
        }
        return original;
    }
}
