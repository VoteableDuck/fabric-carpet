package carpet.mixins;

import carpet.fakes.LevelInterface;
import carpet.fakes.WorldChunkInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public abstract class Level_movableBEMixin implements LevelInterface, LevelAccessor
{
    /**
     * @author 2No2Name
     */
    @Override
    public boolean setBlockStateWithBlockEntity(BlockPos blockPos, BlockState newState, BlockEntity newBlockEntity, int flags)
    {
        Level level = (Level) (Object) this;
        if (!level.isInValidBounds(blockPos) || !level.isClientSide() && level.isDebug())
        {
            return false;
        }

        blockPos = blockPos.immutable();
        LevelChunk chunk = level.getChunkAt(blockPos);
        BlockSnapshot blockSnapshot = null;
        if (level.captureBlockSnapshots && !level.isClientSide())
        {
            blockSnapshot = BlockSnapshot.create(level.dimension(), level, blockPos, flags);
            level.capturedBlockSnapshots.add(blockSnapshot);
        }

        BlockState oldState;
        if (newBlockEntity != null && newState.getBlock() instanceof EntityBlock)
        {
            oldState = ((WorldChunkInterface) chunk).setBlockStateWithBlockEntity(blockPos, newState, newBlockEntity, flags);
            if (newBlockEntity instanceof LidBlockEntity)
            {
                level.scheduleTick(blockPos, newState.getBlock(), 5);
            }
        }
        else
        {
            oldState = chunk.setBlockState(blockPos, newState, flags);
        }

        if (oldState == null)
        {
            if (blockSnapshot != null)
            {
                level.capturedBlockSnapshots.remove(blockSnapshot);
            }
            return false;
        }

        if (blockSnapshot == null)
        {
            level.markAndNotifyBlock(blockPos, chunk, oldState, newState, flags, 512);
        }
        return true;
    }
}
