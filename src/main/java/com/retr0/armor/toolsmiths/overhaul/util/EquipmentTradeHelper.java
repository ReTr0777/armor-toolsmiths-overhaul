package com.retr0.armor.toolsmiths.overhaul.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

public class EquipmentTradeHelper {

    public static boolean isEquipmentForProfession(VillagerProfession profession, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return false;
        String path = itemId.getPath();

        // Disallow netherite items as requested
        if (path.contains("netherite")) {
            return false;
        }

        Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (professionId == null) return false;

        String profName = professionId.getPath();

        if ("armorer".equals(profName)) {
            return isArmor(path) || path.equals("shield");
        } else if ("toolsmith".equals(profName)) {
            return isTool(path);
        } else if ("weaponsmith".equals(profName)) {
            return isWeapon(path);
        }

        return false;
    }

    public static boolean isDefaultEquipmentSellOffer(MerchantOffer offer) {
        if (offer == null) return false;
        ItemStack result = offer.getResult();
        if (result.isEmpty()) return false;

        Item item = result.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return false;
        String path = itemId.getPath();

        boolean isEquipment = isArmor(path) || isTool(path) || isWeapon(path) || path.equals("shield");
        if (!isEquipment) return false;

        // Default vanilla equipment trades always cost Emeralds for primary itemCostA
        return offer.getItemCostA().item().value() == Items.EMERALD;
    }

    public static int getTotalEnchantmentLevels(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return 0;

        int totalLevels = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            totalLevels += entry.getIntValue();
        }
        return totalLevels;
    }

    public static MerchantOffer createOrderOffer(ItemStack equipmentStack) {
        Item item = equipmentStack.getItem();
        Item materialItem = getMaterialItem(item);
        
        int baseCount = getBaseMaterialCount(item);
        int totalEnchantmentLevels = getTotalEnchantmentLevels(equipmentStack);
        int extraCost = totalEnchantmentLevels * 2;
        int count = Math.min(64, baseCount + extraCost);

        ItemStack sellStack = equipmentStack.copy();
        sellStack.setCount(1);

        // 12 uses, 10 villager xp, 0.05 price multiplier
        return new MerchantOffer(
                new ItemCost(materialItem, count),
                Optional.empty(),
                sellStack,
                12,
                10,
                0.05f
        );
    }

    public static boolean isArmor(String path) {
        return path.endsWith("_chestplate") ||
               path.endsWith("_leggings") ||
               path.endsWith("_helmet") ||
               path.endsWith("_boots");
    }

    public static boolean isTool(String path) {
        return path.endsWith("_pickaxe") ||
               path.endsWith("_axe") ||
               path.endsWith("_shovel") ||
               path.endsWith("_hoe");
    }

    public static boolean isWeapon(String path) {
        return path.endsWith("_sword") ||
               path.endsWith("_axe") ||
               path.equals("bow") ||
               path.equals("crossbow") ||
               path.equals("trident") ||
               path.equals("mace");
    }

    private static Item getMaterialItem(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return Items.IRON_INGOT;
        String path = id.getPath();

        if (path.contains("diamond")) return Items.DIAMOND;
        if (path.contains("iron")) return Items.IRON_INGOT;
        if (path.contains("golden") || path.contains("gold")) return Items.GOLD_INGOT;
        if (path.contains("leather")) return Items.LEATHER;
        if (path.contains("chainmail")) return Items.IRON_INGOT;
        if (path.contains("turtle")) return Items.TURTLE_SCUTE;
        if (path.contains("stone")) return Items.COBBLESTONE;
        if (path.contains("wooden") || path.contains("wood")) return Items.OAK_PLANKS;

        if (path.equals("shield")) return Items.OAK_PLANKS;

        return Items.IRON_INGOT;
    }

    private static int getBaseMaterialCount(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return 3;
        String path = id.getPath();

        if (path.endsWith("_chestplate")) return 8;
        if (path.endsWith("_leggings")) return 7;
        if (path.endsWith("_helmet")) return 5;
        if (path.endsWith("_boots")) return 4;

        if (path.endsWith("_pickaxe") || path.endsWith("_axe")) return 3;
        if (path.endsWith("_sword") || path.endsWith("_hoe")) return 2;
        if (path.endsWith("_shovel")) return 1;

        if (path.equals("shield")) return 6;

        return 3;
    }
}
