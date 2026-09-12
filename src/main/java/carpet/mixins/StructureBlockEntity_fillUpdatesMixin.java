package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockEntity_fillUpdatesMixin
{
    @WrapOperation(method = "placeStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/util/RandomSource;I)Z"
    ))
    private boolean onStructurePlace(StructureTemplate structure, ServerLevelAccessor serverWorldAccess, BlockPos pos,
                                     BlockPos blockPos, StructurePlaceSettings placementData, RandomSource random, int flags,
                                     Operation<Boolean> original)
    {
        boolean previous = CarpetSettings.impendingFillSkipUpdates.get();
        if (!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
        try
        {
            return original.call(structure, serverWorldAccess, pos, blockPos, placementData, random, flags);
        }
        finally
        {
            CarpetSettings.impendingFillSkipUpdates.set(previous);
        }
    }
}
