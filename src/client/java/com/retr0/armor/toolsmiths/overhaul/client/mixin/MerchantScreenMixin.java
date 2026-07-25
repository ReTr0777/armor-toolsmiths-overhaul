package com.retr0.armor.toolsmiths.overhaul.client.mixin;

import com.retr0.armor.toolsmiths.overhaul.network.MoveTradePayload;
import com.retr0.armor.toolsmiths.overhaul.network.RemoveTradePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    @Shadow
    private int scrollOff;

    public MerchantScreenMixin(MerchantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "extractScroller", at = @At("HEAD"), cancellable = true)
    private void hideScrollbar(GuiGraphicsExtractor graphics, int x, int y, int index, int count, MerchantOffers offers, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void renderTradeControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MerchantOffers offers = this.menu.getOffers();
        if (offers == null || offers.isEmpty()) return;

        Font font = this.font;
        int baseX = this.leftPos + 95;
        int removeX = this.leftPos + 105;
        int baseY = this.topPos + 16;

        for (int i = 0; i < 7; i++) {
            int realIndex = i + this.scrollOff;
            if (realIndex >= offers.size()) break;

            int slotY = baseY + (i * 20);
            int upY = slotY + 1;
            int downY = slotY + 10;
            int removeY = slotY + 5;

            boolean upHovered = mouseX >= baseX && mouseX < baseX + 9 && mouseY >= upY && mouseY < upY + 8;
            boolean downHovered = mouseX >= baseX && mouseX < baseX + 9 && mouseY >= downY && mouseY < downY + 8;
            boolean removeHovered = mouseX >= removeX && mouseX < removeX + 9 && mouseY >= removeY && mouseY < removeY + 9;

            int upColor = (realIndex > 0) ? (upHovered ? 0xFFFFAA00 : 0xFFFFFFFF) : 0x55888888;
            int downColor = (realIndex < offers.size() - 1) ? (downHovered ? 0xFFFFAA00 : 0xFFFFFFFF) : 0x55888888;
            int removeColor = removeHovered ? 0xFFFF3333 : 0xFFAAAA66;

            // Render Up/Down Arrows
            graphics.text(font, "▲", baseX, upY, upColor, false);
            graphics.text(font, "▼", baseX, downY, downColor, false);

            // Render Trade Removal Button (✕)
            graphics.text(font, "✕", removeX, removeY, removeColor, false);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleTradeControlClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;

        MerchantOffers offers = this.menu.getOffers();
        if (offers == null || offers.isEmpty()) return;

        double mouseX = event.x();
        double mouseY = event.y();
        int baseX = this.leftPos + 95;
        int removeX = this.leftPos + 105;
        int baseY = this.topPos + 16;

        for (int i = 0; i < 7; i++) {
            int realIndex = i + this.scrollOff;
            if (realIndex >= offers.size()) break;

            int slotY = baseY + (i * 20);
            int upY = slotY + 1;
            int downY = slotY + 10;
            int removeY = slotY + 5;

            // Check Up/Down arrows
            if (mouseX >= baseX && mouseX < baseX + 9) {
                if (mouseY >= upY && mouseY < upY + 8) {
                    if (realIndex > 0) {
                        ClientPlayNetworking.send(new MoveTradePayload(realIndex, false));
                        cir.setReturnValue(true);
                        return;
                    }
                } else if (mouseY >= downY && mouseY < downY + 8) {
                    if (realIndex < offers.size() - 1) {
                        ClientPlayNetworking.send(new MoveTradePayload(realIndex, true));
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }

            // Check Removal button (✕)
            if (mouseX >= removeX && mouseX < removeX + 9 && mouseY >= removeY && mouseY < removeY + 9) {
                ClientPlayNetworking.send(new RemoveTradePayload(realIndex));
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
