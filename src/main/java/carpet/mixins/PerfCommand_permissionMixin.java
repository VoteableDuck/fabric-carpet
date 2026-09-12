package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.Commands;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PerfCommand.class)
public class PerfCommand_permissionMixin
{
    @WrapOperation(method = "register", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/Commands;hasPermission(Lnet/minecraft/server/permissions/PermissionCheck;)Lnet/minecraft/server/permissions/PermissionProviderCheck;"
    ))
    private static PermissionProviderCheck canRun(PermissionCheck permissionCheck, Operation<PermissionProviderCheck> original)
    {
        return original.call(CarpetSettings.perfPermissionCheck);
    }
}
