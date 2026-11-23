package dev.devce.rocketnautics.content.blocks.pressurizer;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.devce.rocketnautics.content.energy.CustomEnergyStorage;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PressurizerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private boolean initialized = false;
    private LongOpenHashSet localPressurized = new LongOpenHashSet(); // local copy of pressurized blocks
    public ScrollOptionBehaviour<ToggleMode> modeSelector;
    public static final int ENERGY_CAPACITY = 10000;
    public static final int CONSUMPTION_RATE = 5;
    public final CustomEnergyStorage energyStorage = new CustomEnergyStorage(ENERGY_CAPACITY, CONSUMPTION_RATE);
    public static LongOpenHashSet pressurized = new LongOpenHashSet();
    int ticksSinceUpdate = 0;

    public IEnergyStorage getEnergyStorage(@org.jetbrains.annotations.Nullable Direction side) {
        return energyStorage;
    }

    public boolean isActive() {
        return energyStorage.getEnergyStored() >= CONSUMPTION_RATE && isTurnedOn();
    }

    public PressurizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    private void onToggled(ToggleMode newMode) {
        setChanged();
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
    // Detects if Air can enter from a certain position's neighbor, not counting doors.
    private boolean canAirEnter(Level level, BlockPos fromPos, BlockPos toPos, Direction dir) {
        BlockState fromState = level.getBlockState(fromPos);
        BlockState toState = level.getBlockState(toPos);

        if (!fromPos.equals(this.worldPosition) && fromState.isFaceSturdy(level, fromPos, dir)) {
            return false;
        }

        if (toState.isFaceSturdy(level, toPos, dir.getOpposite())) return false;

        if (toState.getBlock() instanceof StainedGlassBlock || toState.getBlock() instanceof ConnectedGlassBlock || toState.getBlock() instanceof TintedGlassBlock) {
            return false;
        }

        if (toState.getBlock() instanceof DoorBlock && !toState.getValue(DoorBlock.OPEN)) {
            return false;
        }

        return true;
    }

    private boolean isExposedToVacuum(Level level, BlockPos pos) {
        // get highest block at certain Z, counting glass blocks
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return pos.getY() >= topY;
    }

    // I use AI for coding advice, im not that good at minecraft modding yet, plus i mean im good at java just still learning modding.
    // AI also gave me design oversights, and came up with some of the logic, mainly cuz i don't understand Minecraft modding logic.

    /**
     * Sets surrounding air blocks in a room as breathable, by putting them in a list via a BFS floodfill algorithm.
     * @param maxBlocks
     * @return A list of breathable coordintes.
     */

    public LongOpenHashSet floodFill(int maxBlocks) {


        Queue<BlockPos> q = new LinkedList<>();  // Make queue
        LongOpenHashSet visited = new LongOpenHashSet();
        q.add(this.getBlockPos());  // add seed (Current Block Position)
        visited.add(this.getBlockPos().asLong()); // add the seed along with visited

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
                if (canAirEnter(level, current, neighbor, dir)) {
                    if (isExposedToVacuum(level, neighbor)) {
                        return new LongOpenHashSet(); // return nothing
                    }

                    visited.add(neighborLong);
                    q.add(neighbor);
                }

            }
        }

        return visited;

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
                LongOpenHashSet newBlocks = floodFill(1000); // new Flood fill scan, stores list of breathable coordinates scanned
                if (!newBlocks.equals(localPressurized)) {  // update if different
                    pressurized.removeAll(localPressurized);
                    localPressurized = newBlocks;
                    pressurized.addAll(localPressurized);  // add to global pressurized list

                    setChanged();
                    notifyUpdate();
                }
            }
        } else if (!localPressurized.isEmpty()) {  // Works because first condition fails, and if there is stuff inside the hashmap while its inactive, depressurize.
            pressurized.removeAll(localPressurized);
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
            pressurized.removeAll(localPressurized);
            localPressurized.clear();
            initialized = false;
        }
    }

    @Override
    public void remove() {
        super.remove();
        // run same code on destroying block as when turned off
        if (!localPressurized.isEmpty()) {
            pressurized.removeAll(localPressurized);
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

        pressurized.removeAll(localPressurized);
        localPressurized.clear();

        for (long pos: tag.getLongArray("LocalPressurized")) {
            localPressurized.add(pos);
        }

        pressurized.addAll(localPressurized);
    }



}


