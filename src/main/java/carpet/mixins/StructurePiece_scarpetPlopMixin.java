package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StructurePiece.class)
public class StructurePiece_scarpetPlopMixin
{
    @WrapOperation(method = "placeBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;markPosForPostprocessing(Lnet/minecraft/core/BlockPos;)V"
    ))
    private void markOrNot(ChunkAccess chunk, BlockPos pos, Operation<Void> original)
    {
        if (!CarpetSettings.skipGenerationChecks.get()) {
            original.call(chunk, pos);
        }
    }
}
