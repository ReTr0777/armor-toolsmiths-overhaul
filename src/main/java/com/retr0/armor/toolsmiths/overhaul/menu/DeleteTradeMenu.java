package com.retr0.armor.toolsmiths.overhaul.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DeleteTradeMenu extends ChestMenu {
    private final Villager villager;
    private final List<Integer> tradeIndexes;

    public DeleteTradeMenu(int containerId, Inventory playerInventory, Container container, Villager villager, List<Integer> tradeIndexes) {
        super(MenuType.GENERIC_9x2, containerId, playerInventory, container, 2);
        this.villager = villager;
        this.tradeIndexes = tradeIndexes;
    }

    @Override
    public void clicked(int slotId, int dragType, ContainerInput containerInput, Player player) {
        if (!player.level().isClientSide()) {
            if (slotId >= 0 && slotId < 18) {
                int listIndex = slotId;
                if (listIndex < tradeIndexes.size()) {
                    int originalOfferIndex = tradeIndexes.get(listIndex);
                    MerchantOffers offers = villager.getOffers();
                    if (offers != null && originalOfferIndex >= 0 && originalOfferIndex < offers.size()) {
                        offers.remove(originalOfferIndex);
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.closeContainer();
                        }
                        player.level().playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                                net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                                    villager.getX(), villager.getY() + 1.0, villager.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                        }
                    }
                }
                return;
            }
        }
        super.clicked(slotId, dragType, containerInput, player);
    }
}
