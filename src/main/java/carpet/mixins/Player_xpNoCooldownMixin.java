package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public abstract class Player_xpNoCooldownMixin {

    @Shadow
    protected abstract void touch(Entity entity);

    @WrapOperation(method = "aiStep",at = @At(value = "INVOKE", target = "java/util/List.add(Ljava/lang/Object;)Z"))
    public boolean processXpOrbCollisions(List<Entity> instance, Object e, Operation<Boolean> original) {
        Entity entity = (Entity) e;
        if (CarpetSettings.xpNoCooldown) {
            this.touch(entity);
            return true;
        }
        return original.call(instance, e);
    }
}
