package carpet.mixins;

import carpet.CarpetSettings;
import carpet.utils.SpawnOverrides;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Monster.class)
public class Monster_templesMixin
{
    @WrapOperation(method = "checkSurfaceMonstersSpawnRules", at = @At(value = "INVOKE", target="Lnet/minecraft/world/level/ServerLevelAccessor;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean isSkylightOrTempleVisible(ServerLevelAccessor serverWorldAccess, BlockPos pos, Operation<Boolean> original,
                                                     EntityType<? extends Mob> entityType, ServerLevelAccessor serverLevelAccessor, EntitySpawnReason entitySpawnReason, BlockPos blockPos, RandomSource randomSource)
    {
        return original.call(serverWorldAccess, pos) ||
                (CarpetSettings.huskSpawningInTemples && entityType == EntityType.HUSK && SpawnOverrides.isStructureAtPosition((ServerLevel)serverWorldAccess, BuiltinStructures.DESERT_PYRAMID, pos));
    }
}
