package dev.devce.rocketnautics.content.blocks.pressurizer;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

// Used AI for this
public enum ToggleMode implements INamedIconOptions {
    OFF("off", AllIcons.I_DISABLE),
    ON("on", AllIcons.I_ACTIVE);

    private final String translationKey;
    private final AllIcons icon;

    ToggleMode(String name, AllIcons icon) {
        this.translationKey = "rocketnautics.togglemode." + name;
        this.icon = icon;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}