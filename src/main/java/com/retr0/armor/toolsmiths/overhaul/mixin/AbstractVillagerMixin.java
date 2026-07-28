package com.retr0.armor.toolsmiths.overhaul.mixin;

import com.retr0.armor.toolsmiths.overhaul.util.EquipmentTradeHelper;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {

    @Inject(method = "overrideOffers", at = @At("HEAD"), cancellable = true)
    private void preventCycleOfCustomTrades(MerchantOffers offers, CallbackInfo ci) {
        if (offers == null || offers.isEmpty()) {
            if (isCalledFromTradeCycling()) {
                AbstractVillager self = (AbstractVillager) (Object) this;
                MerchantOffers currentOffers = self.getOffers();
                if (currentOffers != null) {
                    for (MerchantOffer offer : currentOffers) {
                        if (EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                            ci.cancel();
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isCalledFromTradeCycling() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("de.maxhenkel.tradecycling")) {
                return true;
            }
        }
        return false;
    }
}
