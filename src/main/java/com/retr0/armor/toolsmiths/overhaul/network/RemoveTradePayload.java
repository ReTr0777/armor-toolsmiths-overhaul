package com.retr0.armor.toolsmiths.overhaul.network;

import com.retr0.armor.toolsmiths.overhaul.ArmorToolsmithsOverhaul;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RemoveTradePayload(int tradeIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoveTradePayload> ID =
            new CustomPacketPayload.Type<>(ArmorToolsmithsOverhaul.id("remove_trade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveTradePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RemoveTradePayload::tradeIndex,
                    RemoveTradePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
