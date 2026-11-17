package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Per-player data saved with the player (survives relogs and death). */
public final class ModAttachments {
    private ModAttachments() { }

    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, KSkillsPerks.MOD_ID);

    /** Item magnet switched off with the toggle key. */
    public static final Supplier<AttachmentType<Boolean>> MAGNET_OFF = REGISTER.register("magnet_off",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** The player's Hollow state (1.2.0). */
    public static final Supplier<AttachmentType<com.kragnoth.kskillsperks.hollow.HollowData>> HOLLOW = REGISTER.register("hollow",
            () -> AttachmentType.builder(() -> com.kragnoth.kskillsperks.hollow.HollowData.EMPTY)
                    .serialize(com.kragnoth.kskillsperks.hollow.HollowData.CODEC).copyOnDeath().build());

    /** Game time when Last Stand can trigger again. */
    public static final Supplier<AttachmentType<Long>> LAST_STAND_READY = REGISTER.register("last_stand_ready",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build());
}
