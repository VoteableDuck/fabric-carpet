package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherBoss.class)
public class WitherBoss_moreBlueMixin
{
    @WrapOperation(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private float nextFloatAmplfied(RandomSource random, Operation<Float> original)
    {
        float value = original.call(random);
        if (CarpetSettings.moreBlueSkulls) return value / 100.0F;
        return value;
    }
}
