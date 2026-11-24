package dev.devce.rocketnautics.content.blocks.pressurizer;

public abstract class ScanFactory {
    public abstract IFloodScanner createScan(PressurizerBlockEntity pressurizer);
}
