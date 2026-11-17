package com.kragnoth.kskillsperks.hollow;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /kskills hollow ... (op 2). Every command returns a number, so scripts can read it (KubeJS: server.runCommandSilent).
 *   status <player>                 -> weight in percent
 *   defeat <player>                 -> 1 the first time (tier 5, held, locked into the vision), else 0
 *   hold <player> / release <player> -> 1 if it changed anything
 *   tier <player> [<n>]             -> current tier (raises it to n; never lowers)
 *   escapements <player> [add <n>]  -> Escapements made after release
 *   reset <player>                  -> clears the Hollow state (testing)
 */
public final class HollowCommands {
    private HollowCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("kskills").requires(s -> s.hasPermission(2)).then(Commands.literal("hollow")
                .then(Commands.literal("status").then(Commands.argument("player", EntityArgument.player()).executes(HollowCommands::status)))
                .then(Commands.literal("defeat").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> Hollow.defeat(pl(c)) ? 1 : 0)))
                .then(Commands.literal("hold").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> { boolean was = Hollow.get(pl(c)).released(); Hollow.hold(pl(c)); return was ? 1 : 0; })))
                .then(Commands.literal("release").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> Hollow.release(pl(c)) ? 1 : 0)))
                .then(Commands.literal("tier").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> Hollow.get(pl(c)).tier())
                        .then(Commands.argument("tier", IntegerArgumentType.integer(0, 5))
                                .executes(c -> { Hollow.raiseTier(pl(c), IntegerArgumentType.getInteger(c, "tier")); return Hollow.get(pl(c)).tier(); }))))
                .then(Commands.literal("escapements").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> Hollow.get(pl(c)).escapements())
                        .then(Commands.literal("add").then(Commands.argument("n", IntegerArgumentType.integer(-1000, 1000))
                                .executes(c -> Hollow.addEscapements(pl(c), IntegerArgumentType.getInteger(c, "n")))))))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> { Hollow.put(pl(c), HollowData.EMPTY.withMigrated(true)); Hollow.sync(pl(c)); return 1; })))));
    }

    private static ServerPlayer pl(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return EntityArgument.getPlayer(c, "player");
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = pl(c);
        HollowData d = Hollow.get(p);
        int pct = (int) Math.round(Hollow.weight(p) * 100);
        String state = d.released() ? "released" : d.defeated() ? "held (Hollowed)" : "not yet Hollowed";
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": " + state + ", tier " + d.tier()
                + (d.heldDay() > 0 ? ", held " + (Hollow.day(p.server) + 1 - d.heldDay()) + " day(s)" : "")
                + ", escapements " + d.escapements() + ", taken chance " + pct + "%"), false);
        return pct;
    }
}
