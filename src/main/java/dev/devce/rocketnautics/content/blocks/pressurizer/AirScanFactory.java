package dev.devce.rocketnautics.content.blocks.pressurizer;

public class AirScanFactory extends ScanFactory {
    @Override
    public IFloodScanner createScan(PressurizerBlockEntity pressurizer) {
        return new AirScan(pressurizer);
    }
}
