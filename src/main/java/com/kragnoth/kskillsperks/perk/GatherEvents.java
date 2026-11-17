package com.kragnoth.kskillsperks.perk;

import com.kragnoth.kskillsperks.KSkillsPerks;
import com.kragnoth.kskillsperks.config.PerksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/** Auto-Smelt, Treasure Hunter, Auto-Replant. */
@EventBusSubscriber(modid = KSkillsPerks.MOD_ID)
public final class GatherEvents {
    private GatherEvents() { }

    public static final ResourceKey<LootTable> TREASURE_TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, KSkillsPerks.id("treasure/stone"));

    private record Replant(ServerLevel level, BlockPos pos, BlockState state) { }

    private static final Deque<Replant> REPLANTS = new ArrayDeque<>();

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        var rnd = level.getRandom();

        // Auto-Smelt: ores drop their smelted result (real furnace recipes, so modded ores work too).
        if (state.is(Tags.Blocks.ORES) && PerkManager.has(player, Perk.AUTO_SMELT)
                && rnd.nextDouble() < PerksConfig.AUTO_SMELT_CHANCE.get()) {
            Item oreItem = state.getBlock().asItem();
            boolean silkTouched = event.getDrops().stream().anyMatch(e -> e.getItem().is(oreItem));
            if (!silkTouched) {
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack stack = drop.getItem();
                    var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
                    if (recipe.isPresent()) {
                        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
                        if (!result.isEmpty()) {
                            drop.setItem(result.copyWithCount(Math.min(result.getMaxStackSize(), result.getCount() * stack.getCount())));
                        }
                    }
                }
            }
        }

        // Treasure Hunter: small chance of bonus loot from stone (loot table is datapack-overridable).
        if (PerkManager.has(player, Perk.TREASURE_HUNTER) && isStone(state)
                && rnd.nextDouble() < PerksConfig.TREASURE_CHANCE.get()) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(TREASURE_TABLE);
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            }
        }

        // Auto-Replant: mature crops replant themselves from their own drops.
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state) && PerkManager.has(player, Perk.AUTO_REPLANT)) {
            Item seed = crop.getCloneItemStack(level, pos, state).getItem();
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                if (stack.is(seed) && !stack.isEmpty()) {
                    stack.shrink(1);
                    drop.setItem(stack);
                    REPLANTS.add(new Replant(level, pos.immutable(), crop.getStateForAge(0)));
                    break;
                }
            }
            event.getDrops().removeIf(e -> e.getItem().isEmpty());
        }
    }

    private static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Tags.Blocks.STONES);
    }

    /** Replant one tick later, once the broken crop has actually been removed. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        while (!REPLANTS.isEmpty()) {
            Replant r = REPLANTS.poll();
            if (r.level().isLoaded(r.pos()) && r.level().getBlockState(r.pos()).isAir() && r.state().canSurvive(r.level(), r.pos())) {
                r.level().setBlock(r.pos(), r.state(), 3);
            }
        }
    }
}
