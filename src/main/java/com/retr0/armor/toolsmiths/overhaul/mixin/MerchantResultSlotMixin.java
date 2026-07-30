package com.retr0.armor.toolsmiths.overhaul.mixin;

import com.retr0.armor.toolsmiths.overhaul.util.EquipmentTradeHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

    @Shadow
    @Final
    private MerchantContainer slots;

    @Shadow
    @Final
    private Merchant merchant;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTakeItem(Player player, ItemStack stack, CallbackInfo ci) {
        if (!player.level().isClientSide()) {
            if (this.merchant instanceof Villager villager) {
                VillagerProfession profession = villager.getVillagerData().profession().value();
                boolean isMaster = villager.getVillagerData().level() >= 5;
                boolean isSmith = EquipmentTradeHelper.isSmithProfession(profession);

                if (isSmith && isMaster) {
                    MerchantOffer offer = this.slots.getActiveOffer();
                    if (offer != null && EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                        // Roll 20% Mastercraft quality dynamically on purchase
                        if (Math.random() < 0.20D) {
                            EquipmentTradeHelper.applyMastercraftQuality(stack);
                        }
                    }
                }
            }
        }
    }
}
