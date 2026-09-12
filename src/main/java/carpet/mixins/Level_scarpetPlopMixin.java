package carpet.mixins;

import carpet.CarpetSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public class Level_scarpetPlopMixin
{

    @WrapOperation(method = "getHeight", at = @At(
            value = "INVOKE",
            target = "net/minecraft/world/level/chunk/LevelChunk.getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
    ))
    private int fixSampleHeightmap(LevelChunk chunk, Heightmap.Types type, int x, int z, Operation<Integer> original)
    {
        Heightmap.Types sampledType = type;
        if (CarpetSettings.skipGenerationChecks.get())
        {
            if (type == Heightmap.Types.OCEAN_FLOOR_WG) sampledType = Heightmap.Types.OCEAN_FLOOR;
            else if (type == Heightmap.Types.WORLD_SURFACE_WG) sampledType = Heightmap.Types.WORLD_SURFACE;
        }
        return original.call(chunk, sampledType, x, z);
    }
}
