package dev.devce.rocketnautics.content.blocks.pressurizer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayDeque;
import java.util.Queue;

public abstract class AbstractFloodScanner {
    protected final PressurizerBlockEntity pressurizer;
    protected Level level;

    protected AbstractFloodScanner(PressurizerBlockEntity pressurizer) {
        this.pressurizer = pressurizer;
    }


    /**
     * Sets surrounding air blocks in a room as breathable, by putting them in a list via a BFS floodfill algorithm.
     * @param maxBlocks the max blocks it can scan, if the scan is bigger than that, then the room isn't considered sealed/pressurized.
     * @return A list of breathable coordintes.
     */
    protected LongOpenHashSet floodFill(int maxBlocks) {


        Queue<BlockPos> q = new ArrayDeque<>();  // Make queue
        LongOpenHashSet visited = new LongOpenHashSet();
        q.add(pressurizer.getBlockPos());  // add seed (Current Block Position)
        visited.add(pressurizer.getBlockPos().asLong()); // add the seed along with visited

        // Standard flood fill BFS algorithm
        while (!q.isEmpty()) {
            BlockPos current = q.poll();  // get current block
            if (visited.size() > maxBlocks) {  // Detect leak because leak will cause it to quickly exceed max
                return new LongOpenHashSet(); // return nothing
            }

            for (Direction dir: Direction.values()) {  // Transverse all directions
                BlockPos neighbor = current.relative(dir);  // Get neighbor
                long neighborLong = neighbor.asLong();

                if (visited.contains(neighborLong)) {  // skip if visited
                    continue;
                }
                // Make sure its not a hole.
                if (canAirEnter(level, current, neighbor, dir)) {
                    if (isExposedToVacuum(neighbor)) {
                        return new LongOpenHashSet(); // return nothing
                    }

                    visited.add(neighborLong);
                    q.add(neighbor);
                }

            }
        }

        return visited;

    }
    /**
     * Detects if Air can enter from the passed arguments.
     * @param fromPos  The position to scan from, or to start from
     * @param toPos  The next position/neighboring block.
     * @param dir The direction to scan in
     * @return True or False, whether if air can enter or not.
     */
    protected boolean canAirEnter(Level level, BlockPos fromPos, BlockPos toPos, Direction dir) {
        BlockState fromState = level.getBlockState(fromPos);
        BlockState toState = level.getBlockState(toPos);

        if (!fromPos.equals(pressurizer.worldPos) && fromState.isFaceSturdy(level, fromPos, dir)) {
            return false;
        }

        if (toState.isFaceSturdy(level, toPos, dir.getOpposite())) return false;

        if (toState.is(Tags.Blocks.GLASS_BLOCKS)) {
            return false;
        }

        if (toState.getBlock() instanceof DoorBlock && !toState.getValue(DoorBlock.OPEN)) {
            return false;
        }

        return true;
    }
    protected abstract boolean isExposedToVacuum(BlockPos pos);
}
