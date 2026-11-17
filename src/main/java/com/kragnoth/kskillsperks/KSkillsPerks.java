package com.kragnoth.kskillsperks;

import com.kragnoth.kskillsperks.config.ConfigOverrides;
import com.kragnoth.kskillsperks.config.PerksConfig;
import com.kragnoth.kskillsperks.net.NetworkHandler;
import com.kragnoth.kskillsperks.perk.IndustryTicker;
import com.kragnoth.kskillsperks.perk.ModAttachments;
import com.kragnoth.kskillsperks.perk.PerkReward;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KSkillsPerks.MOD_ID)
public class KSkillsPerks {
    public static final String MOD_ID = "kskills_perks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public KSkillsPerks(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, PerksConfig.SPEC);
        // Per-world overrides on top of the common config.
        NeoForge.EVENT_BUS.addListener(ConfigOverrides::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(ConfigOverrides::onServerStopped);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> IndustryTicker.clearStoked());
        ModAttachments.REGISTER.register(modBus);
        modBus.addListener(NetworkHandler::register);
        // Pufferfish's Skills loads skill trees when the server starts, so registering here is early enough.
        PerkReward.register();
        com.kragnoth.kskillsperks.perk.ConditionalAttributeReward.register();
        LOGGER.info("Registered Pufferfish's Skills reward types {} and {}", PerkReward.ID,
                com.kragnoth.kskillsperks.perk.ConditionalAttributeReward.ID);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
