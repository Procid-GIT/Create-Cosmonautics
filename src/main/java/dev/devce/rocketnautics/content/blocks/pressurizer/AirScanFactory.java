package dev.devce.rocketnautics.content.blocks.pressurizer;

public class AirScanFactory extends ScanFactory {
    @Override
    public AbstractFloodScanner createScan(PressurizerBlockEntity pressurizer) {
        return new AirScan(pressurizer);
    }
}
