package com.retr0.armor.toolsmiths.overhaul.util;

import com.retr0.armor.toolsmiths.overhaul.ArmorToolsmithsOverhaul;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Optional;

public class EquipmentTradeHelper {

    public static boolean isSmithProfession(VillagerProfession profession) {
        if (profession == null) return false;
        Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (professionId == null) return false;
        String profName = professionId.getPath();
        return "armorer".equals(profName) || "toolsmith".equals(profName) || "weaponsmith".equals(profName);
    }

    public static void addStarterSmithTrades(VillagerProfession profession, MerchantOffers offers) {
        if (profession == null || offers == null) return;

        Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (professionId == null) return;
        String profName = professionId.getPath();

        // 15 Coal -> 1 Emerald
        MerchantOffer coalTrade = new MerchantOffer(
                new ItemCost(Items.COAL, 15),
                Optional.empty(),
                new ItemStack(Items.EMERALD, 1),
                16,
                2,
                0.05f
        );
        offers.add(coalTrade);

        if ("armorer".equals(profName)) {
            // 5 Iron Ingots -> 1 Emerald
            MerchantOffer ironTrade = new MerchantOffer(
                    new ItemCost(Items.IRON_INGOT, 5),
                    Optional.empty(),
                    new ItemStack(Items.EMERALD, 1),
                    12,
                    2,
                    0.05f
            );
            offers.add(ironTrade);
        } else if ("toolsmith".equals(profName)) {
            // 24 Flint -> 1 Emerald
            MerchantOffer flintTrade = new MerchantOffer(
                    new ItemCost(Items.FLINT, 24),
                    Optional.empty(),
                    new ItemStack(Items.EMERALD, 1),
                    12,
                    2,
                    0.05f
            );
            offers.add(flintTrade);
        } else if ("weaponsmith".equals(profName)) {
            // 4 Iron Ingots -> 1 Emerald
            MerchantOffer ironTrade = new MerchantOffer(
                    new ItemCost(Items.IRON_INGOT, 4),
                    Optional.empty(),
                    new ItemStack(Items.EMERALD, 1),
                    12,
                    2,
                    0.05f
            );
            offers.add(ironTrade);
        }
    }

    public static boolean isEquipmentForProfession(VillagerProfession profession, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

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

    public static boolean isCustomPlayerTrade(MerchantOffer offer) {
        if (offer == null) return false;
        ItemStack result = offer.getResult();
        if (result.isEmpty()) return false;

        Item item = result.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return false;
        String path = itemId.getPath();

        boolean isEquipment = isArmor(path) || isTool(path) || isWeapon(path) || path.equals("shield");
        if (!isEquipment) return false;

        // Custom player trades cost raw materials (NOT Emeralds)
        return offer.getItemCostA().item().value() != Items.EMERALD;
    }

    public static int getTotalEnchantmentLevels(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        int totalLevels = 0;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchantments.isEmpty()) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                totalLevels += entry.getIntValue();
            }
        }

        return totalLevels;
    }

    public static void applyMastercraftQuality(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        // Set custom name: "Mastercrafted <Item Name>" in Gold & Bold
        Component currentName = stack.getHoverName();
        Component mastercraftName = Component.literal("Mastercrafted ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(currentName);
        stack.set(DataComponents.CUSTOM_NAME, mastercraftName);

        // Add lore line in Gold & Italic
        ItemLore lore = new ItemLore(List.of(
                Component.literal("Crafted with legendary skill by a Master Smith")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
        ));
        stack.set(DataComponents.LORE, lore);

        // Apply item-specific Mastercraft attribute boosts
        ItemAttributeModifiers currentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = itemId != null ? itemId.getPath() : "";

        if (path.endsWith("_chestplate")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_armor"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_toughness"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MAX_HEALTH,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_health"), 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_knockback_res"), 0.05D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
        } else if (path.endsWith("_leggings")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_armor"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_toughness"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.SNEAKING_SPEED,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_sneak"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_speed"), 0.03D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.ARMOR);
        } else if (path.endsWith("_helmet")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_armor"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_toughness"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.OXYGEN_BONUS,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_oxygen"), 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.SUBMERGED_MINING_SPEED,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_water_mining"), 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.ARMOR);
        } else if (path.endsWith("_boots")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_armor"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_speed"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.SAFE_FALL_DISTANCE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_fall"), 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.STEP_HEIGHT,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_step"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.ARMOR);
        } else if (path.equals("shield")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_armor"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_knockback_res"), 0.10D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MAX_HEALTH,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_health"), 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND);
        } else if (path.endsWith("_pickaxe")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MINING_EFFICIENCY,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_mining"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_reach"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        } else if (path.endsWith("_shovel")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MINING_EFFICIENCY,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_mining"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_reach"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_speed"), 0.03D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
        } else if (path.endsWith("_hoe")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MINING_EFFICIENCY,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_mining"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_entity_reach"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        } else if (path.endsWith("_sword")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_damage"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ATTACK_SPEED,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_attack_speed"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.SWEEPING_DAMAGE_RATIO,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_sweeping"), 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ATTACK_KNOCKBACK,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_knockback"), 0.2D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        } else if (path.endsWith("_axe")) {
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_damage"), 0.5D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.MINING_EFFICIENCY,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_mining"), 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    EquipmentSlotGroup.MAINHAND);
            currentModifiers = currentModifiers.withModifierAdded(Attributes.ATTACK_KNOCKBACK,
                    new AttributeModifier(ArmorToolsmithsOverhaul.id("mastercraft_knockback"), 0.2D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, currentModifiers);
    }

    public static MerchantOffer createOrderOffer(ItemStack equipmentStack, boolean isMasterVillager) {
        Item item = equipmentStack.getItem();
        Item materialItem = getMaterialItem(item);
        
        int baseCount = getBaseMaterialCount(item);
        int enchantLevels = getTotalEnchantmentLevels(equipmentStack);
        int extraCost = enchantLevels * 2;
        int count = Math.min(64, baseCount + extraCost);

        ItemStack sellStack = equipmentStack.copy();
        sellStack.setCount(1);

        // Tiered catalysts based on total enchantment levels
        Optional<ItemCost> itemCostB = Optional.empty();
        if (enchantLevels >= 6) {
            itemCostB = Optional.of(new ItemCost(Items.GHAST_TEAR, 1));
        } else if (enchantLevels >= 3) {
            itemCostB = Optional.of(new ItemCost(Items.AMETHYST_SHARD, 1));
        } else if (enchantLevels >= 1) {
            itemCostB = Optional.of(new ItemCost(Items.LAPIS_LAZULI, 2));
        }

        // No price inflation for non-enchanted items
        float priceMultiplier = (enchantLevels > 0) ? 0.05f : 0.0f;

        // 6 uses (blueprint stability), 10 villager xp, price multiplier
        return new MerchantOffer(
                new ItemCost(materialItem, count),
                itemCostB,
                sellStack,
                6,
                10,
                priceMultiplier
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
               path.endsWith("_axe");
    }

    private static Item getMaterialItem(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return Items.IRON_INGOT;
        String path = id.getPath();

        if (path.contains("diamond")) return Items.DIAMOND;
        if (path.contains("iron")) return Items.IRON_INGOT;
        if (path.contains("golden") || path.contains("gold")) return Items.GOLD_INGOT;
        if (path.contains("copper")) return Items.COPPER_INGOT;
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
