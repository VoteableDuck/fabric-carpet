package carpet.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Carpet-owned accessors use a namespace prefix so they cannot collide with
 * accessors injected by other NeoForge mods into ServerExplosion.
 */
@Mixin(ServerExplosion.class)
public interface ExplosionAccessor
{
    @Accessor("level")
    ServerLevel carpet$getLevel();

    @Accessor("center")
    Vec3 carpet$getCenter();

    @Accessor("radius")
    float carpet$getRadius();

    @Accessor("source")
    Entity carpet$getSource();

    @Accessor("damageSource")
    DamageSource carpet$getDamageSource();
}
