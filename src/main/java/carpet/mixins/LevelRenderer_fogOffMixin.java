package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GameRenderer.class, priority = 69420)
public class LevelRenderer_fogOffMixin
{
    @WrapOperation(method = "renderLevel", require = 0, expect = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"
    ))
    private <Value> Value isReallyThick(
            EnvironmentAttributeProbe instance,
            EnvironmentAttribute<Value> environmentAttribute,
            float f,
            Operation<Value> original)
    {
        if (CarpetSettings.fogOff) {
            @SuppressWarnings("unchecked")
            Value falseValue = (Value) Boolean.FALSE;
            return falseValue;
        }
        return original.call(instance, environmentAttribute, f);
    }
}
