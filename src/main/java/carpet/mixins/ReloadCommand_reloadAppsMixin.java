package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ReloadCommand.class)
public class ReloadCommand_reloadAppsMixin
{
    /*
     * Fabric Carpet targeted the intermediary-only synthetic command callback
     * method_13530 with remap=false. NeoForge production uses Mojang mappings,
     * so that name does not exist. Hook the stable Mojmap reloadPacks entry
     * point instead; it is the operation reached by /reload after the pack list
     * has been resolved and preserves Carpet's existing reload notification.
     */
    @Inject(
            method = "reloadPacks(Ljava/util/Collection;Lnet/minecraft/commands/CommandSourceStack;)V",
            at = @At("TAIL")
    )
    private static void onReload(Collection<String> packs, CommandSourceStack source, CallbackInfo ci)
    {
        CarpetServer.onReload(source.getServer());
    }
}
