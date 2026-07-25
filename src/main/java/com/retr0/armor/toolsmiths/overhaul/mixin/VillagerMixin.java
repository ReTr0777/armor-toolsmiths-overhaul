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
            offers.removeIf(EquipmentTradeHelper::isEquipmentSellOffer);
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void handleEquipmentOrder(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        VillagerProfession profession = this.getVillagerData().profession().value();

        if (EquipmentTradeHelper.isEquipmentForProfession(profession, heldItem)) {
            if (!this.level().isClientSide()) {
                MerchantOffer offer = EquipmentTradeHelper.createOrderOffer(heldItem);
                MerchantOffers offers = this.getOffers();

                if (offers != null) {
                    // Remove default equipment sell trades if any remain
                    offers.removeIf(EquipmentTradeHelper::isEquipmentSellOffer);

                    // Remove existing identical custom trade if present to avoid duplicate entries
                    offers.removeIf(o -> ItemStack.isSameItemSameComponents(o.getResult(), offer.getResult()));

                    // Insert custom order offer at top of tradelist
                    offers.add(0, offer);
                }

                // Consume 1 item from player unless in Creative
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
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
