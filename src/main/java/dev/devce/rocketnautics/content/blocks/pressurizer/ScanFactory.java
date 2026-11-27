package dev.devce.rocketnautics.content.blocks.pressurizer;

public abstract class ScanFactory {
    public abstract AbstractFloodScanner createScan(PressurizerBlockEntity pressurizer);
}
