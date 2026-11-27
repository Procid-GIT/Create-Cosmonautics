package dev.devce.rocketnautics.content.blocks.pressurizer;

public class SubLevelScanFactory extends ScanFactory {
    @Override
    public AbstractFloodScanner createScan(PressurizerBlockEntity pressurizer) {
        return new SubLevelScan(pressurizer);
    }
}
