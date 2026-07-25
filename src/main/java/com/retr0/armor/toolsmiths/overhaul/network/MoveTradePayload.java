package com.retr0.armor.toolsmiths.overhaul.network;

import com.retr0.armor.toolsmiths.overhaul.ArmorToolsmithsOverhaul;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MoveTradePayload(int tradeIndex, boolean moveDown) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MoveTradePayload> ID =
            new CustomPacketPayload.Type<>(ArmorToolsmithsOverhaul.id("move_trade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoveTradePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MoveTradePayload::tradeIndex,
                    ByteBufCodecs.BOOL, MoveTradePayload::moveDown,
                    MoveTradePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
