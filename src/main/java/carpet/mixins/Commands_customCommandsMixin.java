package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class Commands_customCommandsMixin
{
    @Inject(method = "performCommand", at = @At("HEAD"))
    private void onExecuteBegin(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfo ci)
    {
        if (!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "performCommand", at = @At("RETURN"))
    private void onExecuteEnd(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfo ci)
    {
        CarpetSettings.impendingFillSkipUpdates.set(false);
    }

    @WrapOperation(method = "performCommand", at = @At(
                value = "INVOKE",
                target = "Lorg/slf4j/Logger;isDebugEnabled()Z",
                remap = false
            ),
        require = 0
    )
    private boolean doesOutputCommandStackTrace(Logger logger, Operation<Boolean> original)
    {
        if (CarpetSettings.superSecretSetting)
            return true;
        return original.call(logger);
    }
}
