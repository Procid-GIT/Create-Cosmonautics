package dev.devce.rocketnautics.content.blocks.pressurizer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

// naming java interfaces be like: Hi, I am FloodScanner!
public interface IFloodScanner {
    LongOpenHashSet floodFill(int maxBlocks);
    boolean canAirEnter(BlockPos fromPos, BlockPos toPos, Direction dir);
    boolean isExposedToVacuum(BlockPos pos);
}
