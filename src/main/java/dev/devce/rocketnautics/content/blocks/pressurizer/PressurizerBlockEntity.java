package dev.devce.rocketnautics.content.blocks.pressurizer;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.content.energy.CustomEnergyStorage;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PressurizerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private boolean initialized = false;
    public ScrollOptionBehaviour<ToggleMode> modeSelector;
    public ScanFactory factory;
    private LongOpenHashSet localPressurized = new LongOpenHashSet(); // local copy of pressurized blocks
    public static final int ENERGY_CAPACITY = 10000;
    public static final int CONSUMPTION_RATE = 5;
    public final CustomEnergyStorage energyStorage = new CustomEnergyStorage(ENERGY_CAPACITY, CONSUMPTION_RATE);
    public static Map<ResourceKey<Level>, LongOpenHashSet> pressurized = new HashMap<>(); // global pressurized blocks across each dimension (This method was suggested by AI for consideration of dimensions)
    int ticksSinceUpdate = 0;
    public BlockPos worldPos = this.worldPosition;  // Current world position
    public int maxBlocks = RocketConfig.SERVER.maxPressurizedVolume.get();

    public IEnergyStorage getEnergyStorage(@org.jetbrains.annotations.Nullable Direction side) {
        return energyStorage;
    }

    public boolean isActive() {
        return energyStorage.getEnergyStored() >= CONSUMPTION_RATE && isTurnedOn();
    }

    public PressurizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static LongOpenHashSet getPressurized(Level level) {
        if (level == null) return new LongOpenHashSet();
        return pressurized.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet());
    }

    // Used AI for this and the Enum
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        Component label = Component.translatable("gui.rocketnautics.pressurizer.power");
        modeSelector = new ScrollOptionBehaviour<>(
                ToggleMode.class,
                label,
                this,
                new CenteredSideValueBoxTransform(
                        (state, direction) -> {
                            if (!state.hasProperty(PressurizerBlock.FACING)) return direction.getAxis().isHorizontal();
                            Direction facing = state.getValue(PressurizerBlock.FACING);
                            return direction != facing && direction != facing.getOpposite();
                        }
                )
        );

        behaviours.add(modeSelector);


    }

    public Level getLevel() {
        return this.level;
    }

    public boolean isTurnedOn() {
        return modeSelector.get() == ToggleMode.ON;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("block.rocketnautics.pressurizer").withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal("    ")
                .append(Component.translatable("gui.rocketnautics.goggles.energy_stored").withStyle(ChatFormatting.GRAY))
                .append(": ")
                .append(Component.literal(energyStorage.getEnergyStored() + " / " + ENERGY_CAPACITY + "FE").withStyle(ChatFormatting.AQUA)));
        return true;
    }



    @Override
    public void tick() {
        ticksSinceUpdate++;
        super.tick();
        boolean active = isActive();
        if (level == null || level.isClientSide()) return;
        if (active) {  // If active
            energyStorage.extractEnergyInternal(CONSUMPTION_RATE);
            ticksSinceUpdate++;  // Update Ticks since last update
            if (!initialized || ticksSinceUpdate >= 60) {  // update every 3 seconds or on initialization
                initialized = true;
                ticksSinceUpdate = 0;
                AbstractFloodScanner scan = factory.createScan(this);
                LongOpenHashSet newBlocks = scan.floodFill(maxBlocks); // new Flood fill scan, stores list of breathable coordinates scanned
                if (!newBlocks.equals(localPressurized)) {  // update if different
                    getPressurized(this.level).removeAll(localPressurized);
                    localPressurized = newBlocks;
                    getPressurized(this.level).addAll(localPressurized);  // add to global pressurized list

                    setChanged();
                    notifyUpdate();
                }
            }
        } else if (!localPressurized.isEmpty()) {  // Works because first condition fails, and if there is stuff inside the hashmap while its inactive, depressurize.
            getPressurized(this.level).removeAll(localPressurized);
            localPressurized.clear();
            initialized = false;
            setChanged();
            notifyUpdate();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        // run same code on destroying block as when turned off
        if (!localPressurized.isEmpty()) {
            getPressurized(this.level).removeAll(localPressurized);
            localPressurized.clear();
            initialized = false;
        }
    }

    @Override
    public void remove() {
        super.remove();
        // run same code on destroying block as when turned off
        if (!localPressurized.isEmpty()) {
            if (this.level != null) {
                getPressurized(this.level).removeAll(localPressurized);
            }
            localPressurized.clear();
            initialized = false;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLongArray("LocalPressurized", localPressurized.toLongArray());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        if (this.level != null) {
            getPressurized(this.level).removeAll(localPressurized);
        }
        localPressurized.clear();

        for (long pos: tag.getLongArray("LocalPressurized")) {
            localPressurized.add(pos);
        }

        if (this.level != null) {
            getPressurized(this.level).addAll(localPressurized);
        }
    }

    @Override
    public void initialize() {
        boolean isSublevel = SableCompanion.INSTANCE.getContaining(this) instanceof SubLevel;
        if (isSublevel){
            this.factory = new SubLevelScanFactory();
        } else {
            this.factory = new AirScanFactory();
        }
    }

}


