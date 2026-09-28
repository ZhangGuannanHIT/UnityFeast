package cn.zgnhit.unityfeast;

import cn.zgnhit.unityfeast.block.TableBlock;
import cn.zgnhit.unityfeast.block.TableBlockEntity;
import cn.zgnhit.unityfeast.block.GreenbeltBlock;
import cn.zgnhit.unityfeast.block.SauceVatBlock;
import cn.zgnhit.unityfeast.block.CuttingBoardBlock;
import cn.zgnhit.unityfeast.block.CuttingBoardBlockEntity;
import cn.zgnhit.unityfeast.item.GreenbeltItem;
import cn.zgnhit.unityfeast.item.KitchenKnifeItem;
import cn.zgnhit.unityfeast.recipe.SamePlanksBoardRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.Weapon;
import cn.zgnhit.unityfeast.item.StinkyFishItem;
import cn.zgnhit.unityfeast.item.FoodDamage;
import cn.zgnhit.unityfeast.item.CaptainItem;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.NeoForgeMod;
import cn.zgnhit.unityfeast.entity.Rat;
import cn.zgnhit.unityfeast.entity.RatSpawnsBiomeModifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.neoforged.neoforge.common.world.BiomeModifier;
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
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ID);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>,MapCodec<RatSpawnsBiomeModifier>> RAT_SPAWNS = BIOME_MODIFIERS.register("rat_spawns",()->RatSpawnsBiomeModifier.CODEC);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
    public static final TagKey<Item> RAW_FISH = TagKey.create(Registries.ITEM,id("raw_fish"));
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,ID);
    public static final DeferredHolder<EntityType<?>,EntityType<Rat>> RAT=ENTITIES.register("rat",()->EntityType.Builder.of(Rat::new,MobCategory.CREATURE)
            .sized(.4F,.3F).eyeHeight(.23F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE,id("rat"))));

    public static final DeferredBlock<TableBlock> TABLE = BLOCKS.registerBlock("table", TableBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem("table", TABLE);
    public static final DeferredBlock<GreenbeltBlock> GREENBELT=BLOCKS.registerBlock("greenbelt",GreenbeltBlock::new,
            p->p.mapColor(MapColor.PLANT).replaceable().noCollision().instabreak().sound(SoundType.GRASS).randomTicks().ignitedByLava().pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<GreenbeltItem> GREENBELT_ITEM=ITEMS.registerItem("greenbelt",p->new GreenbeltItem(GREENBELT.get(),p.food(food(2,false),
            Consumable.builder().consumeSeconds(1.6F).onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NAUSEA,60),.3F)).build()).useBlockDescriptionPrefix()));
    public static final DeferredBlock<SauceVatBlock> SAUCE_VAT=BLOCKS.registerBlock("sauce_vat",SauceVatBlock::new,p->p.mapColor(MapColor.COLOR_RED).strength(2).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> SAUCE_VAT_ITEM=ITEMS.registerSimpleBlockItem("sauce_vat",SAUCE_VAT);
    public static final DeferredItem<Item> SOY_PASTE=ITEMS.registerSimpleItem("soy_paste",p->p.stacksTo(1).food(food(6,false),effects(new MobEffectInstance(MobEffects.NAUSEA,100))).usingConvertsTo(Items.BOWL).craftRemainder(Items.BOWL));
    public static final DeferredItem<Item> DIPPED_LETTUCE=ITEMS.registerSimpleItem("dipped_lettuce",p->p.food(food(4,false),effects(new MobEffectInstance(MobEffects.STRENGTH,200,1))));
    public static final DeferredItem<KitchenKnifeItem> KITCHEN_KNIFE=ITEMS.registerItem("kitchen_knife",p->new KitchenKnifeItem(p.durability(251).repairable(Items.IRON_INGOT)
            .component(DataComponents.WEAPON,new Weapon(1)).attributes(ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,3,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,-3,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build())));
    public static final DeferredBlock<CuttingBoardBlock> CUTTING_BOARD=BLOCKS.registerBlock("cutting_board",CuttingBoardBlock::new,p->p.mapColor(MapColor.WOOD).strength(1).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> CUTTING_BOARD_ITEM=ITEMS.registerSimpleBlockItem("cutting_board",CUTTING_BOARD);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD_ENTITY=BLOCK_ENTITIES.register("cutting_board",()->new BlockEntityType<>(CuttingBoardBlockEntity::new,CUTTING_BOARD.get()));
    public static final DeferredItem<Item> RAW_PORK_SLICES=ITEMS.registerSimpleItem("raw_pork_slices",p->p.food(food(4,false),Consumable.builder().consumeSeconds(1.6F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NAUSEA,100)))
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.POISON,200),.3F)).build()));
    public static final DeferredItem<Item> SALMON_SASHIMI=ITEMS.registerSimpleItem("salmon_sashimi",p->p.food(food(2,false),Consumable.builder().consumeSeconds(1.6F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.POISON,100),.2F)).build()));
    public static final DeferredItem<Item> WHITE_CUT_CHICKEN=ITEMS.registerSimpleItem("white_cut_chicken",p->p.food(food(6,false),effects(new MobEffectInstance(MobEffects.LEVITATION,200))));
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<SamePlanksBoardRecipe>> BOARD_RECIPE=RECIPES.register("same_planks_board",()->new RecipeSerializer<>(SamePlanksBoardRecipe.CODEC,SamePlanksBoardRecipe.STREAM_CODEC));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TableBlockEntity>> TABLE_ENTITY = BLOCK_ENTITIES.register("table",()->new BlockEntityType<>(TableBlockEntity::new,TABLE.get()));
    public static final DeferredItem<StinkyFishItem> STINKY_FISH = ITEMS.registerItem("stinky_fish",p->new StinkyFishItem(p.food(food(2,false))));
    public static final DeferredItem<Item> SAN_ZHI=ITEMS.registerSimpleItem("san_zhi",p->p.food(food(2,false),effects(new MobEffectInstance(MobEffects.POISON,60),new MobEffectInstance(MobEffects.NAUSEA,60))));
    public static final DeferredItem<FeastFoodItem> WEIJIXIAN=ITEMS.registerItem("weijixian",p->new FeastFoodItem(p.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build()),FeastFoodItem.Kind.WEIJIXIAN));
    public static final DeferredItem<CaptainItem> CAPTAIN=ITEMS.registerItem("captain",p->new CaptainItem(p.stacksTo(1).attributes(ItemAttributeModifiers.builder()
            .add(NeoForgeMod.CREATIVE_FLIGHT,new AttributeModifier(id("captain_flight"),1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.OFFHAND).build())));
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
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        BIOME_MODIFIERS.register(bus);
        NeoForge.EVENT_BUS.register(PlayerLifecycleEvents.class);
        NeoForge.EVENT_BUS.register(FoodDamage.class);
    }

    static {
        TABS.register("unity_feast", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + ID))
                .icon(() -> UNITY_HEART.get().getDefaultInstance()).displayItems((parameters, out) -> {
                    out.accept(TABLE_ITEM.get()); out.accept(DUMPLING.get()); out.accept(TABLE_DUMPLING.get());
                    out.accept(WATER_BEETLE.get()); out.accept(HOTPOT.get()); out.accept(UNITY_HEART.get());
                    out.accept(STINKY_FISH.get());
                    out.accept(SAN_ZHI.get());
                    out.accept(WEIJIXIAN.get());out.accept(CAPTAIN.get());
                    out.accept(GREENBELT_ITEM.get());out.accept(SAUCE_VAT_ITEM.get());out.accept(SOY_PASTE.get());out.accept(DIPPED_LETTUCE.get());
                    out.accept(KITCHEN_KNIFE.get());out.accept(CUTTING_BOARD_ITEM.get());out.accept(RAW_PORK_SLICES.get());out.accept(SALMON_SASHIMI.get());out.accept(WHITE_CUT_CHICKEN.get());
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
