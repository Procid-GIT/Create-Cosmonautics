package dev.devce.rocketnautics.content.blocks.pressurizer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayDeque;
import java.util.Queue;

/// Solid implementation class for flood fill algorithm
public class AirScan implements IFloodScanner {
    private final PressurizerBlockEntity pressurizer;
    private final Level level;


    public AirScan(PressurizerBlockEntity pressurizer) {
        this.pressurizer = pressurizer;
        this.level = pressurizer.getLevel();
    }

    /**
     * Detects if Air can enter from the passed arguments.
     * @param fromPos  The position to scan from, or to start from
     * @param toPos  The next position/neighboring block.
     * @param dir The direction to scan in
     * @return True or False, whether if air can enter or not.
     */
    public boolean canAirEnter(BlockPos fromPos, BlockPos toPos, Direction dir) {
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

    /**
     * Detects if the block passed as a parameter can see the sky or is exposed to air blocks, ideally for moon bases.
     * @param pos The positon of the block to check
     * @return True or false, whether if exposed to vacuum or not.
     */
    public boolean isExposedToVacuum(BlockPos pos) {
        // get highest block at certain Z, counting glass blocks
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return pos.getY() >= topY;
    }

    // I use AI for coding advice, im not that good at minecraft modding yet, plus i mean im good at java just still learning modding.
    // AI also gave me design oversights, and came up with some of the logic, mainly cuz i don't understand Minecraft modding logic.

    /**
     * Sets surrounding air blocks in a room as breathable, by putting them in a list via a BFS floodfill algorithm.
     * @param maxBlocks the max blocks it can scan, if the scan is bigger than that, then the room isn't considered sealed/pressurized.
     * @return A list of breathable coordintes.
     */

    public LongOpenHashSet floodFill(int maxBlocks) {


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
                if (canAirEnter(current, neighbor, dir)) {
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
}