package com.retr0.armor.toolsmiths.overhaul.client.mixin;

import com.retr0.armor.toolsmiths.overhaul.util.EquipmentTradeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void addSmithOrderTooltips(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack == null || stack.isEmpty()) return;

        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return;
        String path = itemId.getPath();

        if (!path.contains("netherite")) {
            if (EquipmentTradeHelper.isArmor(path) || EquipmentTradeHelper.isTool(path) || EquipmentTradeHelper.isWeapon(path) || path.equals("shield")) {
                List<Component> tooltip = cir.getReturnValue();
                if (tooltip != null) {
                    tooltip.add(Component.literal("✔ Can Order Gear (Right-Click Smith)").withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }
}
