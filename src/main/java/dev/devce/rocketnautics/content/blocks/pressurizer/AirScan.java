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
public class AirScan extends AbstractFloodScanner {

    public AirScan(PressurizerBlockEntity pressurizer) {
        super(pressurizer);
        this.level = pressurizer.getLevel();
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


}