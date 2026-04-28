package com.github.warriorjacq9.terraindiffusionbp.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

/**
 * Persisted per-world settings for terrain diffusion.
 *
 * <p>This is stored in the world save via Minecraft's persistent state manager.
 */
public final class WorldScaleSettingsState extends PersistentState {
    public static final String ID = "world_scale_settings";
    public WorldScaleSettingsState() {
        super(ID);
    }

    private int scale;
    private boolean explicitScale;

    /**
     * Returns the currently persisted world scale.
     */
    public int getScale() {
        return scale;
    }

    /**
     * Returns whether this world has an explicitly chosen scale.
     */
    public boolean hasExplicitScale() {
        return explicitScale;
    }

    /**
     * Applies a new persisted world scale and marks the state dirty.
     */
    public void setScale(int configuredScale) {
        this.scale = WorldScaleManager.clampScale(configuredScale);
        this.explicitScale = true;
        markDirty();
    }

    @Override
    public void fromTag(NbtCompound tag) {
        if(tag.contains("scale")) {
            scale = WorldScaleManager.clampScale(tag.getInt("scale"));
        } else {
            scale = WorldScaleManager.DEFAULT_SCALE;
        }
        if(tag.contains("explicit_scale")) {
            explicitScale = tag.getBoolean("explicit_scale");
        } else {
            explicitScale = false;
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("scale", scale);
        nbt.putBoolean("explicit_scale", explicitScale);
        return nbt;
    }
}