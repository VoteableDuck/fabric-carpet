package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(PieceGeneratorSupplier.class)
public interface PieceGeneratorSupplier_plopMixin
{
    @ModifyVariable(method = "simple", at = @At("HEAD"), argsOnly = true)
    private static Predicate<Object> carpet$skipGenerationChecks(Predicate<Object> predicate)
    {
        return context -> CarpetSettings.skipGenerationChecks.get() || predicate.test(context);
    }
}
