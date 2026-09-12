package carpet.mixins;

import carpet.fakes.PlayerListHudInterface;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class Gui_tablistMixin
{
    @Shadow @Final private PlayerTabOverlay tabList;

    @WrapOperation(method = "renderTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean onDraw(Minecraft minecraftClient, Operation<Boolean> original)
    {
        return original.call(minecraftClient) && !((PlayerListHudInterface) tabList).hasFooterOrHeader();
    }
}
