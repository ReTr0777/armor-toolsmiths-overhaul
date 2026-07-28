package com.retr0.armor.toolsmiths.overhaul.client.mixin;

import com.retr0.armor.toolsmiths.overhaul.client.config.TutorialConfig;
import com.retr0.armor.toolsmiths.overhaul.network.MoveTradePayload;
import com.retr0.armor.toolsmiths.overhaul.network.RemoveTradePayload;
import com.retr0.armor.toolsmiths.overhaul.util.EquipmentTradeHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
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

    private boolean isSmithScreen() {
        if (this.title != null) {
            String titleStr = this.title.getString().toLowerCase();
            if (titleStr.contains("armorer") || titleStr.contains("toolsmith") || titleStr.contains("weaponsmith") || titleStr.contains("smith")) {
                return true;
            }
        }
        MerchantOffers offers = this.menu.getOffers();
        if (offers != null) {
            for (MerchantOffer offer : offers) {
                if (EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHoldingGrindstone() {
        if (Minecraft.getInstance().player == null) return false;
        return Minecraft.getInstance().player.getMainHandItem().is(Items.GRINDSTONE)
            || Minecraft.getInstance().player.getOffhandItem().is(Items.GRINDSTONE);
    }

    @Inject(method = "extractScroller", at = @At("HEAD"), cancellable = true)
    private void hideScrollbar(GuiGraphicsExtractor graphics, int x, int y, int index, int count, MerchantOffers offers, CallbackInfo ci) {
        if (isSmithScreen()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void renderSmithLabels(GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, CallbackInfo ci) {
        if (!isSmithScreen()) return;

        MerchantOffers offers = this.menu.getOffers();
        int customCount = 0;
        if (offers != null) {
            for (MerchantOffer offer : offers) {
                if (EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                    customCount++;
                }
            }
        }

        Font font = Minecraft.getInstance().font;
        Component noteText;
        int textColor;
        if (isHoldingGrindstone()) {
            noteText = Component.literal("Delete Mode");
            textColor = 0xFFD03030;
        } else {
            noteText = Component.literal("Order Slots: " + customCount);
            textColor = 0xFF404040;
        }

        int noteWidth = font.width(noteText);
        int helpX = Math.max(88, 8 + noteWidth + 5);
        Component helpButton = Component.literal("[?]");
        int helpWidth = font.width(helpButton);

        // Cover default "Trades" header with container gray background
        graphicsExtractor.fill(7, 5, helpX + helpWidth + 2, 16, 0xFFC6C6C6);

        // Render header label
        graphicsExtractor.text(font, noteText, 8, 6, textColor, false);

        // Render clickable [?] Help button
        graphicsExtractor.text(font, helpButton, helpX, 6, 0xFF4A90E2, true);
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void renderTradeControlsAndTutorial(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isSmithScreen()) return;

        MerchantOffers offers = this.menu.getOffers();
        if (offers != null && !offers.isEmpty()) {
            Font font = this.font;
            int baseX = this.leftPos + 94;
            int removeX = this.leftPos + 102;
            int baseY = this.topPos + 16;

            boolean isRespec = isHoldingGrindstone();

            for (int i = 0; i < 7; i++) {
                int realIndex = i + this.scrollOff;
                if (realIndex >= offers.size()) break;

                MerchantOffer offer = offers.get(realIndex);
                int slotY = baseY + (i * 20);
                int upY = slotY + 1;
                int downY = slotY + 10;
                int removeY = slotY + 5;

                // Render trade controls strictly for custom player added trades
                if (EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                    boolean upHovered = mouseX >= baseX && mouseX < baseX + 8 && mouseY >= upY && mouseY < upY + 8;
                    boolean downHovered = mouseX >= baseX && mouseX < baseX + 8 && mouseY >= downY && mouseY < downY + 8;

                    int upColor = (realIndex > 0) ? (upHovered ? 0xFFFFAA00 : 0xFFFFFFFF) : 0x55888888;
                    int downColor = (realIndex < offers.size() - 1) ? (downHovered ? 0xFFFFAA00 : 0xFFFFFFFF) : 0x55888888;

                    // Render Up/Down Arrows
                    graphics.text(font, "▲", baseX, upY, upColor, false);
                    graphics.text(font, "▼", baseX, downY, downColor, false);

                    // Render Trade Removal Button (✕) ONLY in Grindstone Respec mode
                    if (isRespec) {
                        boolean removeHovered = mouseX >= removeX && mouseX < removeX + 8 && mouseY >= removeY && mouseY < removeY + 9;
                        int removeColor = removeHovered ? 0xFFFF3333 : 0xFFE53935;
                        graphics.text(font, "✕", removeX, removeY, removeColor, false);
                    }
                }
            }
        }

        // First-time tutorial overlay card for Smiths
        if (!TutorialConfig.hasSeenTutorial) {
            Font font = Minecraft.getInstance().font;
            Minecraft mc = Minecraft.getInstance();

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // 1. Dim background
            graphics.fill(0, 0, screenWidth, screenHeight, 0xD0000000);

            // 2. Exact screen center
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;

            int minX = centerX - 160;
            int maxX = centerX + 160;
            int minY = centerY - 80;
            int maxY = centerY + 80;

            // Tutorial Card Box (320x160)
            graphics.fill(minX, minY, maxX, maxY, 0xF0141418);

            // Blue accent borders
            graphics.fill(minX - 2, minY - 2, maxX + 2, minY, 0xFF4A90E2);
            graphics.fill(minX - 2, maxY, maxX + 2, maxY + 2, 0xFF4A90E2);
            graphics.fill(minX - 2, minY - 2, minX, maxY + 2, 0xFF4A90E2);
            graphics.fill(maxX, minY - 2, maxX + 2, maxY + 2, 0xFF4A90E2);

            // Centered Header (Gold)
            Component title = Component.literal("Armor & Toolsmiths Guide");
            int titleX = centerX - (font.width(title) / 2);
            graphics.text(font, title, titleX, minY + 8, 0xFFFFD700, true);

            int textLeft = minX + 16;

            // Line 1: Order Equipment
            Component b1Header = Component.literal("1. Order Equipment:");
            Component b1Desc1 = Component.literal("   Right-click a Smith holding gear to request");
            Component b1Desc2 = Component.literal("   a custom equipment trade.");
            graphics.text(font, b1Header, textLeft, minY + 22, 0xFFFFFFFF, false);
            graphics.text(font, b1Desc1, textLeft, minY + 32, 0xFFA0A0A0, false);
            graphics.text(font, b1Desc2, textLeft, minY + 41, 0xFFA0A0A0, false);

            // Line 2: Mastercraft Quality
            Component b2Header = Component.literal("2. Mastercraft Quality:");
            Component b2Desc1 = Component.literal("   Master (Lv 5) Smiths have a 20% chance");
            Component b2Desc2 = Component.literal("   to craft Mastercrafted quality gear!");
            graphics.text(font, b2Header, textLeft, minY + 54, 0xFFFFFFFF, false);
            graphics.text(font, b2Desc1, textLeft, minY + 64, 0xFFA0A0A0, false);
            graphics.text(font, b2Desc2, textLeft, minY + 73, 0xFFA0A0A0, false);

            // Line 3: Grindstone Respec
            Component b3Header = Component.literal("3. Grindstone Respec:");
            Component b3Desc1 = Component.literal("   Right-click a Smith with a Grindstone to");
            Component b3Desc2 = Component.literal("   open respec screen & click [✕] to delete.");
            graphics.text(font, b3Header, textLeft, minY + 86, 0xFFFFFFFF, false);
            graphics.text(font, b3Desc1, textLeft, minY + 96, 0xFFA0A0A0, false);
            graphics.text(font, b3Desc2, textLeft, minY + 105, 0xFFA0A0A0, false);

            // Centered Button Box
            int btnWidth = 130;
            int btnMinX = centerX - (btnWidth / 2);
            int btnMaxX = centerX + (btnWidth / 2);
            int btnMinY = maxY - 26;
            int btnMaxY = maxY - 6;

            graphics.fill(btnMinX, btnMinY, btnMaxX, btnMaxY, 0xFF2E7D32);
            graphics.fill(btnMinX - 2, btnMinY - 2, btnMaxX + 2, btnMinY, 0xFF4CAF50);
            graphics.fill(btnMinX - 2, btnMaxY, btnMaxX + 2, btnMaxY + 2, 0xFF4CAF50);

            // Button Label
            Component buttonText = Component.literal("[ Click to Continue ]");
            int buttonX = centerX - (font.width(buttonText) / 2);
            graphics.text(font, buttonText, buttonX, btnMinY + 6, 0xFFFFFFFF, true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleTradeControlClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        if (!isSmithScreen()) return;

        if (!TutorialConfig.hasSeenTutorial) {
            TutorialConfig.hasSeenTutorial = true;
            TutorialConfig.save();
            cir.setReturnValue(true);
            return;
        } else {
            double relX = event.x() - this.leftPos;
            double relY = event.y() - this.topPos;

            Font font = Minecraft.getInstance().font;
            Component noteText = isHoldingGrindstone() ? Component.literal("Delete Mode") : Component.literal("Order Slots: 0");
            int helpX = Math.max(88, 8 + font.width(noteText) + 5);

            if (relX >= helpX - 2 && relX <= helpX + 16 && relY >= 4 && relY <= 16) {
                TutorialConfig.hasSeenTutorial = false;
                cir.setReturnValue(true);
                return;
            }
        }

        MerchantOffers offers = this.menu.getOffers();
        if (offers == null || offers.isEmpty()) return;

        double mouseX = event.x();
        double mouseY = event.y();
        int baseX = this.leftPos + 94;
        int removeX = this.leftPos + 102;
        int baseY = this.topPos + 16;

        boolean isRespec = isHoldingGrindstone();

        for (int i = 0; i < 7; i++) {
            int realIndex = i + this.scrollOff;
            if (realIndex >= offers.size()) break;

            MerchantOffer offer = offers.get(realIndex);
            int slotY = baseY + (i * 20);
            int upY = slotY + 1;
            int downY = slotY + 10;
            int removeY = slotY + 5;

            // Only allow re-ordering or removal strictly for added custom player trades
            if (EquipmentTradeHelper.isCustomPlayerTrade(offer)) {
                // Check Up/Down arrows
                if (mouseX >= baseX && mouseX < baseX + 8) {
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

                // Check Removal button (✕) ONLY in Grindstone Respec mode
                if (isRespec && mouseX >= removeX && mouseX < removeX + 8 && mouseY >= removeY && mouseY < removeY + 9) {
                    ClientPlayNetworking.send(new RemoveTradePayload(realIndex));
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
