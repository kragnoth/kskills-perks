package com.kragnoth.kskillsperks.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.kragnoth.kskillsperks.KSkillsPerks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-world overrides on top of config/kskills_perks-common.toml.
 *
 * File: saves/<world>/serverconfig/kskills_perks-overrides.toml (or world/serverconfig/... on a dedicated server).
 * Only the keys present in that file are overridden; everything else comes from the common config.
 * A fully commented-out template listing every key is written the first time a world starts.
 */
public final class ConfigOverrides {
    private ConfigOverrides() { }

    public static final String FILE_NAME = "kskills_perks-overrides.toml";

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        load(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    static void clear() {
        for (Val<?> v : Val.ALL) {
            v.override = null;
        }
    }

    static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
    }

    public static void load(MinecraftServer server) {
        clear();
        Path path = file(server);
        try {
            if (!Files.exists(path)) {
                writeTemplate(path);
                KSkillsPerks.LOGGER.info("Wrote per-world override template {} (all lines commented out)", path);
                return;
            }
            Config cfg;
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                cfg = TomlFormat.instance().createParser().parse(r);
            }
            int applied = 0;
            for (Val<?> v : Val.ALL) {
                if (apply(cfg, v)) applied++;
            }
            if (applied > 0) {
                KSkillsPerks.LOGGER.info("Applied {} per-world config override(s) from {}", applied, path);
            }
        } catch (Exception e) {
            KSkillsPerks.LOGGER.error("Could not read {} — using config/kskills_perks-common.toml for everything", path, e);
            clear();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean apply(Config cfg, Val<?> v) {
        List<String> keyPath = v.base.getPath();
        Object raw = cfg.get(keyPath);
        if (raw == null) return false;

        Object converted = convert(raw, v.base.getDefault());
        ModConfigSpec.ValueSpec spec = PerksConfig.SPEC.getSpec().get(keyPath);
        if (converted == null || (spec != null && !spec.test(converted))) {
            KSkillsPerks.LOGGER.warn("Ignoring per-world override {} = {} (wrong type or out of range; common value {} kept)",
                    v.key(), raw, v.base.get());
            return false;
        }
        ((Val) v).override = converted;
        KSkillsPerks.LOGGER.info("  override {} = {} (common: {})", v.key(), converted, v.base.get());
        return true;
    }

    /** TOML gives Integer/Long/Double; coerce to the type the setting was defined with. */
    private static Object convert(Object raw, Object def) {
        if (def instanceof Integer) {
            return raw instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) ? n.intValue() : null;
        }
        if (def instanceof Double) {
            return raw instanceof Number n ? n.doubleValue() : null;
        }
        if (def instanceof String) {
            return raw instanceof String ? raw : null;
        }
        if (def instanceof List<?> defList && raw instanceof List<?> rawList) {
            Object sample = defList.isEmpty() ? "" : defList.get(0);
            List<Object> out = new ArrayList<>();
            for (Object o : rawList) {
                Object c = convert(o, sample);
                if (c == null) return null;
                out.add(c);
            }
            return out;
        }
        return null;
    }

    private static void writeTemplate(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Map<String, List<Val<?>>> bySection = new LinkedHashMap<>();
        for (Val<?> v : Val.ALL) {
            List<String> p = v.base.getPath();
            bySection.computeIfAbsent(String.join(".", p.subList(0, p.size() - 1)), k -> new ArrayList<>()).add(v);
        }
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("# Kragnoth's Skill Perks — per-world overrides\n");
            w.write("#\n");
            w.write("# Global values live in config/kskills_perks-common.toml.\n");
            w.write("# Anything you uncomment here overrides them for THIS world only. Leave a line commented to use the global value.\n");
            w.write("# Values shown are the current global values. Changes apply on the next world load / server start.\n");
            for (var e : bySection.entrySet()) {
                w.write("\n[" + e.getKey() + "]\n");
                for (Val<?> v : e.getValue()) {
                    List<String> p = v.base.getPath();
                    w.write("#" + p.get(p.size() - 1) + " = " + tomlValue(v.base.get()) + "\n");
                }
            }
        }
    }

    private static String tomlValue(Object o) {
        if (o instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (o instanceof List<?> l) {
            List<String> parts = new ArrayList<>();
            for (Object x : l) parts.add(tomlValue(x));
            return "[" + String.join(", ", parts) + "]";
        }
        return String.valueOf(o);
    }
}
