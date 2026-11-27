package dev.devce.rocketnautics.content.blocks.pressurizer;

import dev.ryanhcode.sable.companion.SableCompanion;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;

public class SubLevelScan extends AbstractFloodScanner{
    private final SubLevel subLevel;


    public SubLevelScan(PressurizerBlockEntity pressurizer) {
        super(pressurizer);
        this.subLevel = (SubLevel) SableCompanion.INSTANCE.getContaining(pressurizer);
        if (subLevel.getLevel() == null) return;
        this.level = subLevel.getLevel();
    }


    public boolean isExposedToVacuum(BlockPos pos) {
        LevelPlot plot = subLevel.getPlot();
        BoundingBox3ic box = plot.getBoundingBox();
        return !box.contains(pos.getX(), pos.getY(), pos.getZ());
    }
}
