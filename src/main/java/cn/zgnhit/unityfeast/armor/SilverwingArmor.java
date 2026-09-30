package cn.zgnhit.unityfeast.armor;

import cn.zgnhit.unityfeast.UnityFeastMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** The vanilla netherite material, with only its rendered equipment asset replaced. */
public final class SilverwingArmor {
    private SilverwingArmor() {}
    private static final ArmorMaterial BASE = ArmorMaterials.NETHERITE;
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(BASE.durability(), BASE.defense(),
            BASE.enchantmentValue(), BASE.equipSound(), BASE.toughness(), BASE.knockbackResistance(),
            BASE.repairIngredient(), ResourceKey.create(EquipmentAssets.ROOT_ID, UnityFeastMod.id("silverwing")));

    public static Item.Properties properties(Item.Properties properties, ArmorType type) {
        return properties.humanoidArmor(MATERIAL, type).fireResistant();
    }
}
