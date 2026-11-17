package com.kragnoth.kskillsperks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A config setting that reads from the global common config unless the current world
 * has an override (see {@link ConfigOverrides}).
 */
public final class Val<T> implements Supplier<T> {
    static final List<Val<?>> ALL = new ArrayList<>();

    final ModConfigSpec.ConfigValue<T> base;
    volatile T override;

    Val(ModConfigSpec.ConfigValue<T> base) {
        this.base = base;
        ALL.add(this);
    }

    @Override
    public T get() {
        T o = override;
        return o != null ? o : base.get();
    }

    /** Dotted path as written in the TOML file, e.g. "bulwark.last_stand_cooldown". */
    public String key() {
        return String.join(".", base.getPath());
    }

    public boolean isOverridden() {
        return override != null;
    }
}
