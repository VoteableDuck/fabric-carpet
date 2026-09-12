package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.PathfindingVisualizer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

@Mixin(PathNavigation.class)
public abstract class PathNavigation_pathfindingMixin
{

    @Shadow @Final protected Mob mob;

    @WrapOperation(method =  "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;"
    ))
    private Path pathToBlock(PathNavigation navigation, Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, Operation<Path> original)
    {
        if (!LoggerRegistry.__pathfinding)
            return original.call(navigation, targets, regionOffset, offsetUpward, accuracy);
        long start = System.nanoTime();
        Path path = original.call(navigation, targets, regionOffset, offsetUpward, accuracy);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        targets.forEach(b -> PathfindingVisualizer.slowPath(mob, Vec3.atBottomCenterOf(b), duration, path != null));
        return path;
    }

    @WrapOperation(method =  "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;"
    ))
    private Path pathToEntity(PathNavigation navigation, Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, Operation<Path> original)
    {
        if (!LoggerRegistry.__pathfinding)
            return original.call(navigation, targets, regionOffset, offsetUpward, accuracy);
        long start = System.nanoTime();
        Path path = original.call(navigation, targets, regionOffset, offsetUpward, accuracy);
        long finish = System.nanoTime();
        float duration = (1.0F*((finish - start)/1000))/1000;
        targets.forEach(b -> PathfindingVisualizer.slowPath(mob, Vec3.atBottomCenterOf(b), duration, path != null));
        return path;
    }
}
