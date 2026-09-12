package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StructureTemplate.class)
public class StructureTemplate_fillUpdatesMixin
{
    @WrapOperation(method = "placeInWorld", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private void skipUpdateNeighbours(ServerLevelAccessor serverWorldAccess, BlockPos pos, Block block, Operation<Void> original)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
            original.call(serverWorldAccess, pos, block);
    }

    @WrapOperation(method = "placeInWorld", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;getKnownShape()Z"
    ))
    private boolean skipPostprocess(StructurePlaceSettings structurePlacementData, Operation<Boolean> original)
    {
        return original.call(structurePlacementData) || CarpetSettings.impendingFillSkipUpdates.get();
    }
}
