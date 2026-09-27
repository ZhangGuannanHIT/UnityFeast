package cn.zgnhit.unityfeast.loot;

import cn.zgnhit.unityfeast.UnityFeastMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.loot.*;

public final class WaterBeetleFishingModifier implements IGlobalLootModifier {
    // Implement the stable interface, avoiding LootModifier's changed constructor in 26.1.2.
    public static final MapCodec<WaterBeetleFishingModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(m -> m.conditions),
            Codec.INT.optionalFieldOf("priority", 1000).forGetter(m -> m.priority)
    ).apply(i, WaterBeetleFishingModifier::new));
    private static final Identifier FISHING = Identifier.withDefaultNamespace("gameplay/fishing");
    private final LootItemCondition[] conditions;
    private final int priority;
    private final Predicate<LootContext> predicate;
    public WaterBeetleFishingModifier(LootItemCondition[] conditions, int priority) {
        this.conditions = conditions; this.priority = priority;
        this.predicate = AllOfCondition.allOf(List.of(conditions));
    }
    @Override public int priority() { return priority; }
    public static boolean replace(double sample) { return sample < 0.5; }
    @Override public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!predicate.test(context)) return loot;
        // Only the root table; the vanilla retrieve() success branch supplies this exact hook and owner.
        if (!context.getQueriedLootTableId().equals(FISHING) || loot.isEmpty()) return loot;
        if (!(context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)
                || hook.getPlayerOwner() == null || hook.getHookedIn() != null
                || context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY) != hook.getPlayerOwner()) return loot;
        var tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (!(tool instanceof ItemStack stack) || !stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) return loot;
        // Do not inspect the bobber's block: a legitimate surface bobber can be above the water.
        if (replace(context.getRandom().nextDouble())) {
            loot.clear();
            loot.add(new ItemStack(UnityFeastMod.WATER_BEETLE.get()));
        }
        return loot;
    }
    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
