package cn.zgnhit.unityfeast;

import cn.zgnhit.unityfeast.block.TableBlock;
import cn.zgnhit.unityfeast.item.FeastFoodItem;
import cn.zgnhit.unityfeast.loot.WaterBeetleFishingModifier;
import cn.zgnhit.unityfeast.player.UnityHeartData;
import cn.zgnhit.unityfeast.player.PlayerLifecycleEvents;
import cn.zgnhit.unityfeast.recipe.SamePlanksTableRecipe;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.*;

@Mod(UnityFeastMod.ID)
public final class UnityFeastMod {
    public static final String ID = "unity_feast";
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ID);

    public static final DeferredBlock<TableBlock> TABLE = BLOCKS.registerBlock("table", TableBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem("table", TABLE);
    public static final DeferredItem<Item> DUMPLING = ITEMS.registerSimpleItem("dumpling", p -> p.food(food(4, false)));
    public static final DeferredItem<FeastFoodItem> TABLE_DUMPLING = ITEMS.registerItem("table_dumpling",
            p -> new FeastFoodItem(p.food(food(4, true)), FeastFoodItem.Kind.TABLE_DUMPLING));
    public static final DeferredItem<Item> WATER_BEETLE = ITEMS.registerSimpleItem("water_beetle",
            p -> p.food(food(1, false), effects(new MobEffectInstance(MobEffects.NAUSEA, 100))));
    public static final DeferredItem<Item> HOTPOT = ITEMS.registerSimpleItem("water_beetle_hotpot",
            p -> p.food(food(8, true), effects(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300), new MobEffectInstance(MobEffects.SLOW_FALLING, 300))));
    public static final DeferredItem<FeastFoodItem> UNITY_HEART = ITEMS.registerItem("unity_heart",
            p -> new FeastFoodItem(p.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build()), FeastFoodItem.Kind.UNITY_HEART));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SamePlanksTableRecipe>> TABLE_RECIPE = RECIPES.register("same_planks_table",
            () -> new RecipeSerializer<>(SamePlanksTableRecipe.CODEC, SamePlanksTableRecipe.STREAM_CODEC));
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<WaterBeetleFishingModifier>> FISHING = LOOT.register("water_beetle_fishing", () -> WaterBeetleFishingModifier.CODEC);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UnityHeartData>> HEART_DATA = ATTACHMENTS.register("unity_heart",
            () -> AttachmentType.builder(() -> UnityHeartData.EMPTY).serialize(UnityHeartData.SERIALIZER).copyOnDeath().build());

    public UnityFeastMod(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); RECIPES.register(bus); LOOT.register(bus); ATTACHMENTS.register(bus);
        NeoForge.EVENT_BUS.register(PlayerLifecycleEvents.class);
    }

    static {
        TABS.register("unity_feast", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + ID))
                .icon(() -> UNITY_HEART.get().getDefaultInstance()).displayItems((parameters, out) -> {
                    out.accept(TABLE_ITEM.get()); out.accept(DUMPLING.get()); out.accept(TABLE_DUMPLING.get());
                    out.accept(WATER_BEETLE.get()); out.accept(HOTPOT.get()); out.accept(UNITY_HEART.get());
                }).build());
    }

    private static FoodProperties food(int nutrition, boolean always) {
        var b = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F);
        if (always) b.alwaysEdible();
        return b.build();
    }
    private static Consumable effects(MobEffectInstance... effects) {
        return Consumable.builder().consumeSeconds(1.6F).onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects))).build();
    }
}
