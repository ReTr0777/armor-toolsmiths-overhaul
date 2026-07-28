package com.retr0.armor.toolsmiths.overhaul.mixin;

import com.retr0.armor.toolsmiths.overhaul.util.EquipmentTradeHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
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
        if (offers != null) {
            offers.removeIf(EquipmentTradeHelper::isDefaultEquipmentSellOffer);

            VillagerProfession profession = this.getVillagerData().profession().value();
            if (EquipmentTradeHelper.isSmithProfession(profession)) {
                boolean hasResourceTrade = false;
                for (MerchantOffer offer : offers) {
                    if (!EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                        hasResourceTrade = true;
                        break;
                    }
                }
                if (!hasResourceTrade) {
                    EquipmentTradeHelper.addStarterSmithTrades(profession, offers);
                }
            }
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void handleEquipmentOrder(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        VillagerProfession profession = this.getVillagerData().profession().value();

        if (EquipmentTradeHelper.isSmithProfession(profession)) {
            // Feature 1: Grindstone Respec for Smiths
            // Right-clicking a Smith with a Grindstone opens the trading GUI allowing players to select which trade to delete.
            if (heldItem.is(Items.GRINDSTONE)) {
                if (!this.level().isClientSide()) {
                    MerchantOffers offers = this.getOffers();
                    boolean hasCustomTrade = false;
                    if (offers != null) {
                        for (MerchantOffer offer : offers) {
                            if (EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                                hasCustomTrade = true;
                                break;
                            }
                        }
                    }

                    if (hasCustomTrade) {
                        this.setTradingPlayer(player);
                        this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().level());
                    } else {
                        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // Feature 2: Equipment Order Creation / Re-ordering
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
                            // New order offer: Master villagers (Level 5) apply Mastercraft quality to ordered equipment
                            boolean isMaster = this.getVillagerData().level() >= 5;
                            MerchantOffer offer = EquipmentTradeHelper.createOrderOffer(heldItem, isMaster);
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
                return;
            }

            // Feature 3: Normal Trading Interaction (Empty Hand, Emeralds, etc.)
            // Open trading screen directly so villagers with 0 trades or empty offer lists still open GUI cleanly.
            if (!this.level().isClientSide()) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), this.getVillagerData().level());
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
    }

    /**
     * Triggers a challenge fanfare sound and particle celebration when an Armorer, Toolsmith, or Weaponsmith reaches Level 5 (Master).
     */
    @Inject(method = "setVillagerData", at = @At("HEAD"))
    private void onLevelUpToMaster(VillagerData newVillagerData, CallbackInfo ci) {
        VillagerData currentData = this.getVillagerData();
        VillagerProfession prof = currentData.profession().value();

        if (EquipmentTradeHelper.isSmithProfession(prof)) {
            if (currentData.level() < 5 && newVillagerData.level() >= 5) {
                Level level = this.level();
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 1.0f, 1.0f);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2, this.getZ(), 40, 0.5, 0.5, 0.5, 0.15);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }
}
