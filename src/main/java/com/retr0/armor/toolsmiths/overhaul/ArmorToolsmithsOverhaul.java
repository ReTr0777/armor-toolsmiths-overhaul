package com.retr0.armor.toolsmiths.overhaul;

import com.retr0.armor.toolsmiths.overhaul.network.MoveTradePayload;
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

public class ArmorToolsmithsOverhaul implements ModInitializer {
	public static final String MOD_ID = "armortoolsmiths-overhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Armor & Toolsmiths Overhaul Initialized!");

		// Register C2S network payload for play phase
		PayloadTypeRegistry.serverboundPlay().register(MoveTradePayload.ID, MoveTradePayload.STREAM_CODEC);

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
					player.sendMerchantOffers(
							menu.containerId,
							offers,
							menu.getTraderLevel(),
							menu.getTraderXp(),
							menu.showProgressBar(),
							menu.canRestock()
					);
				}
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
