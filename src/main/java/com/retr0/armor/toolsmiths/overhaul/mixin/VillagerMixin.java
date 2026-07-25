package com.retr0.armor.toolsmiths.overhaul.mixin;

import com.retr0.armor.toolsmiths.overhaul.util.EquipmentTradeHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {

    public VillagerMixin(net.minecraft.world.entity.EntityType<? extends AbstractVillager> entityType, net.minecraft.world.level.Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract VillagerData getVillagerData();

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void removeEquipmentTrades(ServerLevel serverLevel, CallbackInfo ci) {
        MerchantOffers offers = this.getOffers();
        if (offers != null && !offers.isEmpty()) {
            offers.removeIf(EquipmentTradeHelper::isDefaultEquipmentSellOffer);
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void handleEquipmentOrder(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        VillagerProfession profession = this.getVillagerData().profession().value();

        if (EquipmentTradeHelper.isEquipmentForProfession(profession, heldItem)) {
            if (!this.level().isClientSide()) {
                MerchantOffers offers = this.getOffers();

                if (offers != null) {
                    // Remove default emerald equipment trades if any remain
                    offers.removeIf(EquipmentTradeHelper::isDefaultEquipmentSellOffer);

                    // Find index of existing custom trade for this specific item
                    int existingIndex = -1;
                    for (int i = 0; i < offers.size(); i++) {
                        if (ItemStack.isSameItemSameComponents(offers.get(i).getResult(), heldItem)) {
                            existingIndex = i;
                            break;
                        }
                    }

                    if (existingIndex != -1) {
                        // Re-order existing trade: Standing = Move UP, Sneaking = Move DOWN
                        MerchantOffer existingOffer = offers.remove(existingIndex);
                        boolean moveDown = player.isSecondaryUseActive() || player.isCrouching();

                        if (moveDown) {
                            // Move DOWN by 1 position
                            int targetIndex = Math.min(offers.size(), existingIndex + 1);
                            offers.add(targetIndex, existingOffer);
                        } else {
                            // Move UP by 1 position
                            int targetIndex = Math.max(0, existingIndex - 1);
                            offers.add(targetIndex, existingOffer);
                        }
                    } else {
                        // New order offer: create copy of item in villager's trade list without consuming held item
                        MerchantOffer offer = EquipmentTradeHelper.createOrderOffer(heldItem);
                        offers.add(0, offer);
                    }
                }

                // Villager happy effects
                this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 1.0D, this.getZ(),
                        5, 0.2, 0.2, 0.2, 0.0
                    );
                }

                // Open trading UI for player
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().level());
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
