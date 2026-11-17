package com.kragnoth.kskillsperks.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A small Hollow meter left of the hotbar (1.2.0): the chance that nearby animals come up taken. Hidden at 0%. */
public final class HollowHud {
    private HollowHud() { }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        float w = ClientAccess.hollowWeight;
        if (w <= 0f) return;
        int pct = Math.round(w * 100);
        Component text = Component.translatable("hud.kskills_perks.hollow", pct);
        int x = g.guiWidth() / 2 - 91 - 6 - mc.font.width(text);
        int y = g.guiHeight() - 15;
        int barW = mc.font.width(text);
        g.fill(x, y + 10, x + barW, y + 12, 0x80000000);
        g.fill(x, y + 10, x + Math.round(barW * Math.min(1f, w / 0.5f)), y + 12, ClientAccess.hollowHeld ? 0xFF6A3D8A : 0xFF3D8A78);
        g.drawString(mc.font, text, x, y, ClientAccess.hollowHeld ? 0xB48ED6 : 0x8ED6C0, true);
    }
}
