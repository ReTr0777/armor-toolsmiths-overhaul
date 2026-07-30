package com.retr0.armor.toolsmiths.overhaul;

import com.retr0.armor.toolsmiths.overhaul.network.MoveTradePayload;
import com.retr0.armor.toolsmiths.overhaul.network.RemoveTradePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.retr0.armor.toolsmiths.overhaul.mixin.MerchantMenuAccessor;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.npc.villager.Villager;

public class ArmorToolsmithsOverhaul implements ModInitializer {
	public static final String MOD_ID = "armortoolsmiths-overhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Armor & Toolsmiths Overhaul Initialized!");

		// Register C2S network payloads for play phase
		PayloadTypeRegistry.serverboundPlay().register(MoveTradePayload.ID, MoveTradePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RemoveTradePayload.ID, RemoveTradePayload.STREAM_CODEC);

		// Handle C2S trade position move requests on the server
		ServerPlayNetworking.registerGlobalReceiver(MoveTradePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof MerchantMenu menu) {
				MerchantOffers offers = menu.getOffers();
				int index = payload.tradeIndex();

				if (offers != null && index >= 0 && index < offers.size()) {
					MerchantOffer offer = offers.remove(index);
					int targetIndex = payload.moveDown() ? Math.min(offers.size(), index + 1) : Math.max(0, index - 1);
					offers.add(targetIndex, offer);

					menu.setOffers(offers);
					Merchant merchant = ((MerchantMenuAccessor) menu).getTrader();
					int level = (merchant instanceof Villager villager) ? villager.getVillagerData().level() : 1;
					player.sendMerchantOffers(
							menu.containerId,
							offers,
							level,
							merchant.getVillagerXp(),
							merchant.showProgressBar(),
							merchant.canRestock()
					);
				}
			}
		});

		// Handle C2S trade removal requests on the server
		ServerPlayNetworking.registerGlobalReceiver(RemoveTradePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof MerchantMenu menu) {
				MerchantOffers offers = menu.getOffers();
				int index = payload.tradeIndex();

				if (offers != null && index >= 0 && index < offers.size()) {
					offers.remove(index);

					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);

					if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
						serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
					}

					menu.setOffers(offers);
					Merchant merchant = ((MerchantMenuAccessor) menu).getTrader();
					int level = (merchant instanceof Villager villager) ? villager.getVillagerData().level() : 1;
					player.sendMerchantOffers(
							menu.containerId,
							offers,
							level,
							merchant.getVillagerXp(),
							merchant.showProgressBar(),
							merchant.canRestock()
					);
				}
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
