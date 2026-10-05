/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.registries.ForgeRegistries
 */
package net.execheinz.upgrader.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.client.StyledButton;
import net.execheinz.upgrader.client.WheelRenderer;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.execheinz.upgrader.network.ClientboundUpgradeResultPacket;
import net.execheinz.upgrader.network.ClientboundUpgraderSyncPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.ServerboundOpenArenaPacket;
import net.execheinz.upgrader.network.ServerboundOpenCasesPacket;
import net.execheinz.upgrader.network.ServerboundOpenDoublePacket;
import net.execheinz.upgrader.network.ServerboundOpenMarketPacket;
import net.execheinz.upgrader.network.ServerboundSetTargetPacket;
import net.execheinz.upgrader.network.ServerboundStartUpgradePacket;
import net.execheinz.upgrader.value.ItemValues;
import net.execheinz.upgrader.value.UpgradeOdds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.math.Axis;

public class UpgraderScreen
extends AbstractContainerScreen<UpgraderMenu> {
    static final int WIDTH = 320;
    static final int HEIGHT = 292;
    private static final int CARD_L_X1 = 10;
    private static final int CARD_R_X1 = 218;
    private static final int CARD_W = 92;
    private static final int CARD_Y1 = 40;
    private static final int CARD_Y2 = 154;
    static final int INPUT_X = UpgraderMenu.INPUT_X;
    static final int INPUT_Y = UpgraderMenu.INPUT_Y;
    private static final int TARGET_X = UpgraderMenu.TARGET_X;
    private static final int TARGET_Y = UpgraderMenu.TARGET_Y;
    private static final float WHEEL_CX = 160.0f;
    private static final float WHEEL_CY = 96.0f;
    private static final float RING_IN = 30.0f;
    private static final float RING_OUT = 41.0f;
    private static final float TICK_IN = 43.0f;
    private static final float TICK_OUT = 47.0f;
    private static final ItemStack NEEDLE_SWORD = new ItemStack(Items.DIAMOND_SWORD);
    private static final int ROW_Y = 156;
    private static final int ROW_H = 20;
    static final int INV_X = UpgraderMenu.INV_X;
    static final int INV_Y = UpgraderMenu.INV_Y;
    static final int HOTBAR_Y = UpgraderMenu.HOTBAR_Y;
    private static final int PICKER_X1 = 10;
    private static final int PICKER_Y1 = 20;
    private static final int PICKER_X2 = 310;
    private static final int PICKER_Y2 = 280;
    private static final int GRID_X = 18;
    private static final int GRID_Y = 64;
    private static final int GRID_COLS = 15;
    private static final int GRID_ROWS = 9;
    private static final int CELL = 18;
    private static final int SCROLL_X = 296;
    private static final int SCROLL_W = 6;
    private static final int TEXT = UiTheme.TEXT;
    private static final int TEXT_DIM = UiTheme.TEXT_DIM;
    private static final int GOLD = UiTheme.NEON_GOLD;

    private enum PickerSort {
        VALUE_DESC, VALUE_ASC, NAME
    }

    private long inputValue;
    private long targetValue;
    private float chance;
    private boolean spinning;
    private long spinStartMs;
    private long spinDurationMs;
    private float spinTotalAngle;
    private float spinChance;
    private boolean spinWin;
    private int spinTicks;
    private long resultFlashUntil;
    private boolean lastResultWin;
    private boolean spun;
    private boolean pickerOpen;
    private EditBox searchBox;
    private EditBox minPriceBox;
    private EditBox maxPriceBox;
    private List<ItemStack> catalog;
    private List<ItemStack> filtered = List.of();
    private int scrollRow;
    private boolean scrollDragging;
    private PickerSort pickerSort = PickerSort.VALUE_DESC;
    private StyledButton pickerSortButton;
    private StyledButton upgradeButton;
    private StyledButton fastUpgradeButton;
    private StyledButton minusButton;
    private StyledButton plusButton;
    private StyledButton marketButton;
    private StyledButton casesButton;
    private StyledButton doubleButton;
    private StyledButton arenaButton;
    private StyledButton chanceMinus;
    private StyledButton chancePlus;
    private StyledButton chanceLockButton;
    private EditBox chanceBox;
    private double preferredChance = Config.defaultPreferredChance;
    /** When true, preferred % is re-applied whenever input value changes. */
    private boolean autoPreferredChance = false;
    private long lastAutoInputValue = -1L;

    public UpgraderScreen(UpgraderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelX = INV_X;
        this.inventoryLabelY = INV_Y - 12;
        this.preferredChance = Config.defaultPreferredChance;
    }

    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        this.upgradeButton = StyledButton.gold(x + 12, y + ROW_Y, 100, ROW_H, Component.translatable("gui.flashstake.upgrade"), b -> ModNetwork.sendToServer(new ServerboundStartUpgradePacket(false)));
        this.addRenderableWidget(this.upgradeButton);
        this.fastUpgradeButton = StyledButton.neon(x + 116, y + ROW_Y, 22, ROW_H, Component.literal("\u26A1"), UiTheme.NEON_GOLD, b -> ModNetwork.sendToServer(new ServerboundStartUpgradePacket(true)));
        this.fastUpgradeButton.setTooltip(Tooltip.create(Component.translatable("gui.flashstake.fast_upgrade")));
        this.addRenderableWidget(this.fastUpgradeButton);
        // Nav — full labels, wider chips
        int navY = y + 1;
        int navH = 12;
        int navGap = 2;
        int navW = 50;
        int navX = x + 3;
        this.marketButton = StyledButton.neon(navX, navY, navW, navH, Component.translatable("gui.flashstake.market"), UiTheme.NEON_GOLD, b -> {
            FlashFx.click();
            UiCursor.captureIfInFlashStakeUi();
            ModNetwork.sendToServer(new ServerboundOpenMarketPacket());
        });
        this.addRenderableWidget(this.marketButton);
        navX += navW + navGap;
        this.casesButton = StyledButton.neon(navX, navY, navW, navH, Component.translatable("gui.flashstake.cases"), UiTheme.NEON_MAGENTA, b -> {
            FlashFx.click();
            UiCursor.captureIfInFlashStakeUi();
            ModNetwork.sendToServer(new ServerboundOpenCasesPacket());
        });
        this.addRenderableWidget(this.casesButton);
        navX += navW + navGap;
        this.doubleButton = StyledButton.neon(navX, navY, navW, navH, Component.translatable("gui.flashstake.double"), UiTheme.NEON_LIME, b -> {
            FlashFx.click();
            UiCursor.captureIfInFlashStakeUi();
            ModNetwork.sendToServer(new ServerboundOpenDoublePacket());
        });
        this.addRenderableWidget(this.doubleButton);
        navX += navW + navGap;
        this.arenaButton = StyledButton.neon(navX, navY, navW, navH, Component.translatable("gui.flashstake.arena"), UiTheme.NEON_PURPLE, b -> {
            FlashFx.click();
            UiCursor.captureIfInFlashStakeUi();
            ModNetwork.sendToServer(new ServerboundOpenArenaPacket());
        });
        this.addRenderableWidget(this.arenaButton);
        UiCursor.scheduleRestore();
        this.minusButton = StyledButton.neon(x + TARGET_X - 19, y + CARD_Y2 - 22, 18, 16, Component.literal("-"), UiTheme.NEON_CYAN, b -> this.adjustTargetCount(-1, hasShiftDown()));
        this.plusButton = StyledButton.neon(x + TARGET_X + 19, y + CARD_Y2 - 22, 18, 16, Component.literal("+"), UiTheme.NEON_CYAN, b -> this.adjustTargetCount(1, hasShiftDown()));
        this.addRenderableWidget(this.minusButton);
        this.addRenderableWidget(this.plusButton);
        // Preferred % — right side of the action row (X / - / box / +)
        int chanceRight = x + WIDTH - 8;
        this.chancePlus = StyledButton.neon(chanceRight - 16, y + ROW_Y, 16, ROW_H, Component.literal("+"), UiTheme.TEXT, b -> this.adjustPreferredChance(hasShiftDown() ? 0.05 : 0.01));
        this.chanceBox = new EditBox(this.font, chanceRight - 16 - 4 - 46, y + ROW_Y + 3, 46, 14, Component.literal("%"));
        this.chanceMinus = StyledButton.neon(chanceRight - 16 - 4 - 46 - 4 - 16, y + ROW_Y, 16, ROW_H, Component.literal("-"), UiTheme.TEXT, b -> this.adjustPreferredChance(hasShiftDown() ? -0.05 : -0.01));
        this.chanceLockButton = StyledButton.neon(chanceRight - 16 - 4 - 46 - 4 - 16 - 4 - 18, y + ROW_Y, 18, ROW_H, this.lockLabel(), this.lockColor(), b -> this.toggleAutoPreferredChance());
        this.addRenderableWidget(this.chanceLockButton);
        this.addRenderableWidget(this.chanceMinus);
        this.addRenderableWidget(this.chancePlus);
        this.chanceBox.setMaxLength(5);
        this.chanceBox.setTextColor(UiTheme.NEON_CYAN);
        this.chanceBox.setValue(formatChancePercent(this.preferredChance));
        this.chanceBox.setResponder(this::parseChanceBox);
        this.addRenderableWidget(this.chanceBox);
        this.searchBox = new EditBox(this.font, x + 16, y + 26, 118, 16, Component.translatable("gui.flashstake.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(UiTheme.TEXT);
        this.searchBox.setHint(Component.translatable("gui.flashstake.search").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(text -> {
            this.scrollRow = 0;
            this.refilter();
        });
        this.minPriceBox = new EditBox(this.font, x + 140, y + 26, 36, 16, Component.literal("min"));
        this.minPriceBox.setMaxLength(10);
        this.minPriceBox.setHint(Component.translatable("gui.flashstake.market.min"));
        this.minPriceBox.setTextColor(UiTheme.NEON_LIME);
        this.minPriceBox.setResponder(t -> {
            this.scrollRow = 0;
            this.refilter();
        });
        this.maxPriceBox = new EditBox(this.font, x + 180, y + 26, 36, 16, Component.literal("max"));
        this.maxPriceBox.setMaxLength(10);
        this.maxPriceBox.setHint(Component.translatable("gui.flashstake.market.max"));
        this.maxPriceBox.setTextColor(UiTheme.NEON_MAGENTA);
        this.maxPriceBox.setResponder(t -> {
            this.scrollRow = 0;
            this.refilter();
        });
        this.pickerSortButton = StyledButton.neon(x + 222, y + 26, 72, 16, this.pickerSortLabel(), UiTheme.NEON_PURPLE, b -> {
            this.pickerSort = switch (this.pickerSort) {
                case VALUE_DESC -> PickerSort.VALUE_ASC;
                case VALUE_ASC -> PickerSort.NAME;
                case NAME -> PickerSort.VALUE_DESC;
            };
            this.pickerSortButton.setMessage(this.pickerSortLabel());
            this.scrollRow = 0;
            this.refilter();
        });
        this.pickerSortButton.visible = false;
        this.addRenderableWidget(this.pickerSortButton);
    }

    private Component pickerSortLabel() {
        return Component.translatable(switch (this.pickerSort) {
            case VALUE_DESC -> "gui.flashstake.market.sort_high";
            case VALUE_ASC -> "gui.flashstake.market.sort_low";
            case NAME -> "gui.flashstake.market.sort_name";
        });
    }

    private Component lockLabel() {
        return Component.literal(this.autoPreferredChance ? "V" : "X");
    }

    private int lockColor() {
        return this.autoPreferredChance ? UiTheme.NEON_LIME : UiTheme.NEON_MAGENTA;
    }

    private void refreshLockButton() {
        if (this.chanceLockButton == null) {
            return;
        }
        int x = this.chanceLockButton.getX();
        int y = this.chanceLockButton.getY();
        this.removeWidget(this.chanceLockButton);
        this.chanceLockButton = StyledButton.neon(x, y, 18, ROW_H, this.lockLabel(), this.lockColor(), b -> this.toggleAutoPreferredChance());
        this.addRenderableWidget(this.chanceLockButton);
    }

    private void toggleAutoPreferredChance() {
        this.autoPreferredChance = !this.autoPreferredChance;
        this.refreshLockButton();
        if (this.autoPreferredChance) {
            this.applyPreferredChance();
        }
        this.playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
    }

    private void adjustPreferredChance(double delta) {
        this.preferredChance = Mth.clamp(this.preferredChance + delta, Config.minChance, Config.maxChance);
        if (this.chanceBox != null) {
            this.chanceBox.setValue(formatChancePercent(this.preferredChance));
        }
        if (this.autoPreferredChance) {
            this.applyPreferredChance();
        }
    }

    private void parseChanceBox(String text) {
        try {
            String cleaned = text.replace("%", "").trim().replace(',', '.');
            if (cleaned.isEmpty()) {
                return;
            }
            double percent = Double.parseDouble(cleaned);
            this.preferredChance = Mth.clamp(percent / 100.0, Config.minChance, Config.maxChance);
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyPreferredChance() {
        if (this.inputValue <= 0L) {
            return;
        }
        this.applyPreset(this.targetValueForChance(this.preferredChance));
    }

    private static String formatChancePercent(double chance) {
        double pct = chance * 100.0;
        if (Math.abs(pct - Math.rint(pct)) < 0.05) {
            return Integer.toString((int) Math.rint(pct));
        }
        return String.format(Locale.ROOT, "%.1f", pct);
    }

    public void onSync(ClientboundUpgraderSyncPacket packet) {
        long previousInput = this.inputValue;
        this.inputValue = packet.inputValue();
        this.targetValue = packet.targetValue();
        this.chance = packet.chance();
        if (this.inputValue > 0L && this.inputValue != previousInput && this.inputValue != this.lastAutoInputValue && !this.spinning && this.autoPreferredChance) {
            this.lastAutoInputValue = this.inputValue;
            this.applyPreferredChance();
        }
    }

    public void onResult(ClientboundUpgradeResultPacket packet) {
        this.spinning = true;
        this.spun = true;
        this.spinStartMs = System.currentTimeMillis();
        this.spinDurationMs = Math.max(1L, (long)packet.durationTicks() * 50L);
        this.spinChance = packet.chance();
        this.spinWin = packet.success();
        this.spinTicks = 0;
        // Fast spins: ~1s whirl; normal: long multi-spin
        float spins = packet.durationTicks() <= 30 ? 2.0f : 5.0f;
        this.spinTotalAngle = spins * 360.0f + packet.landingAngle();
        this.resultFlashUntil = 0L;
        if (this.pickerOpen) {
            this.closePicker();
        }
    }

    protected void containerTick() {
        super.containerTick();
        if (this.spinning) {
            float progress = this.progress();
            if (progress >= 1.0f) {
                this.spinning = false;
                this.lastResultWin = this.spinWin;
                this.resultFlashUntil = System.currentTimeMillis() + 2000L;
                this.playUi(this.spinWin ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ANVIL_LAND, this.spinWin ? 1.5f : 0.7f);
            } else {
                int interval = progress < 0.55f ? 2 : (progress < 0.85f ? 4 : 7);
                if (this.spinTicks % interval == 0) {
                    this.playUi(SoundEvents.NOTE_BLOCK_HAT.value(), 1.7f);
                }
                ++this.spinTicks;
            }
        }
    }

    private void playUi(SoundEvent sound, float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)sound, (float)pitch, (float)0.35f));
        }
    }

    private float progress() {
        return Mth.clamp((float)((float)(System.currentTimeMillis() - this.spinStartMs) / (float)this.spinDurationMs), (float)0.0f, (float)1.0f);
    }

    private float needleAngle() {
        // Idle / after spin — always park at the top; only move while spinning
        if (!this.spinning) {
            return 0.0f;
        }
        float eased = 1.0f - (float) Math.pow(1.0f - this.progress(), 3.0);
        return eased * this.spinTotalAngle % 360.0f;
    }

    private float displayedChance() {
        return this.spinning || this.resultFlashUntil > System.currentTimeMillis() ? this.spinChance : this.chance;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiCursor.tickInRender();
        boolean ready;
        boolean bl = ready = !this.spinning && !this.pickerOpen;
        UpgraderMenu menu = this.menu;
        if (this.upgradeButton != null) {
            this.upgradeButton.active = ready && !menu.getInputStack().isEmpty() && menu.getTarget() != null;
            this.upgradeButton.visible = !this.pickerOpen;
        }
        if (this.fastUpgradeButton != null) {
            this.fastUpgradeButton.active = ready && !menu.getInputStack().isEmpty() && menu.getTarget() != null;
            this.fastUpgradeButton.visible = !this.pickerOpen;
        }
        if (this.marketButton != null) {
            this.marketButton.active = ready;
            this.marketButton.visible = !this.pickerOpen;
        }
        if (this.casesButton != null) {
            this.casesButton.active = ready;
            this.casesButton.visible = !this.pickerOpen;
        }
        if (this.doubleButton != null) {
            this.doubleButton.active = ready;
            this.doubleButton.visible = !this.pickerOpen;
        }
        if (this.chanceLockButton != null) {
            this.chanceLockButton.active = ready;
            this.chanceLockButton.visible = !this.pickerOpen;
        }
        if (this.chanceMinus != null) {
            this.chanceMinus.active = ready && this.inputValue > 0L;
            this.chanceMinus.visible = !this.pickerOpen;
        }
        if (this.chancePlus != null) {
            this.chancePlus.active = ready && this.inputValue > 0L;
            this.chancePlus.visible = !this.pickerOpen;
        }
        if (this.chanceBox != null) {
            this.chanceBox.visible = !this.pickerOpen;
            this.chanceBox.setEditable(ready);
        }
        boolean qtyReady = ready && menu.getTarget() != null;
        if (this.minusButton != null) {
            this.minusButton.active = qtyReady && menu.getTargetCount() > 1;
            this.minusButton.visible = !this.pickerOpen && menu.getTarget() != null;
        }
        if (this.plusButton != null) {
            this.plusButton.active = qtyReady && menu.getTargetCount() < menu.getTarget().getMaxStackSize();
            this.plusButton.visible = !this.pickerOpen && menu.getTarget() != null;
        }
        if (this.arenaButton != null) {
            this.arenaButton.active = ready;
            this.arenaButton.visible = !this.pickerOpen;
        }
        // Cheap dim — skip vanilla blur/dirt (full screen, not scaled)
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        float fit = this.fitScale();
        int mx = UiFit.mouseXi(mouseX, this.leftPos, WIDTH, fit);
        int my = UiFit.mouseYi(mouseY, this.topPos, HEIGHT, fit);
        UiFit.push(graphics, this.leftPos, this.topPos, WIDTH, HEIGHT, fit);
        try {
            if (this.pickerOpen) {
                // Picker only — do not render the upgrade UI / inventory underneath.
                this.renderPickerOnly(graphics, mx, my, partialTick);
                return;
            }
            super.render(graphics, mx, my, partialTick);
            this.renderTargetTooltip(graphics, mx, my);
        } finally {
            UiFit.pop(graphics, fit);
        }
    }

    private float fitScale() {
        return UiFit.scale(WIDTH, HEIGHT, this.width, this.height);
    }

    private double fitX(double mouseX) {
        return UiFit.mouseX(mouseX, this.leftPos, WIDTH, this.fitScale());
    }

    private double fitY(double mouseY) {
        return UiFit.mouseY(mouseY, this.topPos, HEIGHT, this.fitScale());
    }


    /** Full-screen picker mode: no inventory bleed, no upgrade widgets behind. */
    private void renderPickerOnly(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        // Cover the entire upgrader window (incl. inventory area)
        WheelRenderer.neonPanel(graphics, x, y, x + WIDTH, y + HEIGHT, 8, UiTheme.NEON_PURPLE);
        // Manually render only picker-related widgets (search / filters / sort)
        if (this.searchBox != null) {
            this.searchBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.minPriceBox != null) {
            this.minPriceBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.maxPriceBox != null) {
            this.maxPriceBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.pickerSortButton != null && this.pickerSortButton.visible) {
            this.pickerSortButton.render(graphics, mouseX, mouseY, partialTick);
        }
        this.renderPicker(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (this.pickerOpen) {
            return;
        }
        int x = this.leftPos;
        int y = this.topPos;
        WheelRenderer.neonPanel(graphics, x, y, x + WIDTH, y + HEIGHT, 8, UiTheme.NEON_CYAN);
        this.renderCard(graphics, x + CARD_L_X1, y + CARD_Y1, UiTheme.NEON_BLUE);
        this.renderCard(graphics, x + CARD_R_X1, y + CARD_Y1, UiTheme.NEON_MAGENTA);
        WheelRenderer.neonSlot(graphics, x + INPUT_X, y + INPUT_Y);
        WheelRenderer.neonSlot(graphics, x + TARGET_X, y + TARGET_Y);
        this.renderCardText(graphics);
        this.renderWheel(graphics);
        this.renderInventoryBg(graphics);
    }

    private void renderCard(GuiGraphics graphics, int x1, int y1, int neon) {
        int x2 = x1 + CARD_W;
        int y2 = this.topPos + CARD_Y2;
        WheelRenderer.card(graphics, x1, y1, x2, y2, 5, neon, UiTheme.PANEL_TOP, UiTheme.PANEL_BOTTOM);
        graphics.fill(x1 + 4, y1 + 1, x2 - 4, y1 + 2, (neon & 0x00FFFFFF) | 0x88000000);
        WheelRenderer.hexGrid(graphics, x1 + 2, y1 + 2, x2 - 2, y2 - 2, 9.0f, 0x22FFFFFF);
    }

    private void renderCardText(GuiGraphics graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        int leftCx = x + CARD_L_X1 + CARD_W / 2;
        int rightCx = x + CARD_R_X1 + CARD_W / 2;
        int textW = CARD_W - 14;
        int textTop = y + CARD_Y1 + 42;
        int textBottom = y + CARD_Y2 - 6;

        if (((UpgraderMenu)this.menu).getInputStack().isEmpty()) {
            graphics.enableScissor(x + CARD_L_X1 + 3, textTop - 1, x + CARD_L_X1 + CARD_W - 3, textBottom);
            int next = this.wrapped(graphics, Component.translatable("gui.flashstake.pick_input"), leftCx, textTop, textW, TEXT);
            this.wrapped(graphics, Component.translatable("gui.flashstake.pick_input_hint"), leftCx, next + 1, textW, TEXT_DIM);
            graphics.disableScissor();
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.value", format(this.inputValue)), leftCx, y + CARD_Y1 + 46, TEXT_DIM);
        }

        Item target = this.menu.getTarget();
        if (target == null) {
            graphics.enableScissor(x + CARD_R_X1 + 3, textTop - 1, x + CARD_R_X1 + CARD_W - 3, textBottom);
            int next = this.wrapped(graphics, Component.translatable("gui.flashstake.pick_target"), rightCx, textTop, textW, TEXT);
            this.wrapped(graphics, Component.translatable("gui.flashstake.pick_target_hint"), rightCx, next + 1, textW, TEXT_DIM);
            graphics.disableScissor();
        } else {
            ItemStack targetStack = new ItemStack(target, this.menu.getTargetCount());
            graphics.renderItem(targetStack, x + TARGET_X + 1, y + TARGET_Y + 1);
            graphics.renderItemDecorations(this.font, targetStack, x + TARGET_X + 1, y + TARGET_Y + 1);
            graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.value", format(this.targetValue)), rightCx, y + CARD_Y1 + 46, TEXT_DIM);
            graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.amount", this.menu.getTargetCount()), rightCx, y + CARD_Y1 + 62, TEXT);
        }
    }

    private void renderWheel(GuiGraphics graphics) {
        float cx = (float)this.leftPos + WHEEL_CX;
        float cy = (float)this.topPos + WHEEL_CY;
        float shown = this.displayedChance();
        float needle = this.needleAngle();
        WheelRenderer.disc(graphics, cx, cy, 49.0f, 0xFF0A101C);
        WheelRenderer.tickRing(graphics, cx, cy, TICK_IN, TICK_OUT, 0.0f, 60, 5, UiTheme.PANEL_BORDER, UiTheme.NEON_CYAN);
        WheelRenderer.arc(graphics, cx, cy, RING_IN, RING_OUT, 0.0f, 360.0f, 0xFF1A2238);
        WheelRenderer.arc(graphics, cx, cy, RING_IN, RING_OUT, 0.0f, Math.max(shown * 360.0f, 1.2f), UiTheme.NEON_MAGENTA, UiTheme.NEON_CYAN);
        WheelRenderer.disc(graphics, cx, cy, 29.0f, 0xFF0A101C);
        this.renderNeedleSword(graphics, cx, cy, needle);
        MutableComponent percent = Component.literal(String.format(Locale.ROOT, "%.2f%%", shown * 100.0f));
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy - 12.0f, 0.0f);
        graphics.pose().scale(1.4f, 1.4f, 1.0f);
        graphics.drawCenteredString(this.font, percent, 0, 0, WheelRenderer.chanceColor(shown));
        graphics.pose().popPose();
        long now = System.currentTimeMillis();
        MutableComponent caption = this.spinning ? Component.translatable("gui.flashstake.spinning").withStyle(ChatFormatting.YELLOW) : (this.resultFlashUntil > now ? (this.lastResultWin ? Component.translatable("gui.flashstake.success").withStyle(ChatFormatting.GREEN) : Component.translatable("gui.flashstake.failure").withStyle(ChatFormatting.RED)) : Component.translatable("gui.flashstake.chance"));
        graphics.drawCenteredString(this.font, caption, (int) cx, (int) cy + 4, TEXT_DIM);
    }

    /** Diamond sword on the rim — tip toward center; only drawn on the wheel. */
    private void renderNeedleSword(GuiGraphics graphics, float cx, float cy, float angleDeg) {
        double angle = Math.toRadians(angleDeg);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float r = TICK_OUT + 1.0f;
        float px = cx + sin * r;
        float py = cy - cos * r;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(px, py, 50.0f);
            // Texture tip ≈ top-left; +135° → tip points inward (down at angle 0)
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDeg + 135.0f));
            graphics.pose().scale(1.15f, 1.15f, 1.0f);
            graphics.renderItem(NEEDLE_SWORD, -8, -8);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    private void renderInventoryBg(GuiGraphics graphics) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                WheelRenderer.neonSlot(graphics, this.leftPos + INV_X + col * 18, this.topPos + INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; ++col) {
            WheelRenderer.neonSlot(graphics, this.leftPos + INV_X + col * 18, this.topPos + HOTBAR_Y);
        }
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.pickerOpen) {
            return;
        }
        // Title centered above the upgrade wheel, clear of nav buttons
        graphics.pose().pushPose();
        graphics.pose().translate(WHEEL_CX, 18.0f, 0.0f);
        graphics.pose().scale(1.25f, 1.25f, 1.0f);
        graphics.drawCenteredString(this.font, this.title, 0, 0, UiTheme.NEON_CYAN);
        graphics.pose().popPose();
        int labelY = this.inventoryLabelY;
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, labelY, TEXT_DIM, false);
        Component balance = Component.translatable("gui.flashstake.market.balance", format(MarketScreen.getClientBalance()));
        int balX = INV_X + 9 * 18 - this.font.width(balance);
        graphics.drawString(this.font, balance, balX, labelY, UiTheme.NEON_GOLD, false);
    }

    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.pickerOpen) {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderTargetTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Item target = ((UpgraderMenu)this.menu).getTarget();
        if (target != null && UpgraderScreen.inBox(mouseX, mouseY, this.leftPos + TARGET_X, this.topPos + TARGET_Y, 18, 18)) {
            graphics.renderTooltip(this.font, new ItemStack((ItemLike)target), mouseX, mouseY);
        }
    }

    private void renderPicker(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int index;
        // Inner content only — outer panel already drawn by renderPickerOnly
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.pick_target"),
            this.leftPos + WIDTH / 2, this.topPos + 8, UiTheme.NEON_PURPLE);
        graphics.drawString(this.font, Component.translatable("gui.flashstake.market.filters"),
            this.leftPos + 140, this.topPos + 48, TEXT_DIM, false);
        int first = this.scrollRow * GRID_COLS;
        ItemStack hovered = null;
        for (int i = 0; i < GRID_COLS * GRID_ROWS && (index = first + i) < this.filtered.size(); ++i) {
            int cx = this.leftPos + GRID_X + i % GRID_COLS * CELL;
            int cy = this.topPos + GRID_Y + i / GRID_COLS * CELL;
            boolean over = UpgraderScreen.inBox(mouseX, mouseY, cx, cy, CELL, CELL);
            graphics.fill(cx, cy, cx + CELL, cy + CELL, over ? 0xFF1A2840 : UiTheme.SLOT_BG);
            ItemStack stack = this.filtered.get(index);
            graphics.renderItem(stack, cx + 1, cy + 1);
            if (!over) continue;
            hovered = stack;
        }
        int pages = Math.max(1, this.maxScrollRow() + 1);
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.picker_footer", this.filtered.size(), this.scrollRow + 1, pages), this.leftPos + WIDTH / 2, this.topPos + HEIGHT - 14, TEXT_DIM);
        this.renderPickerScrollbar(graphics, mouseX, mouseY);
        if (hovered != null) {
            ArrayList<Component> lines = new ArrayList<>();
            lines.add(hovered.getHoverName());
            long value = Math.round(ItemValues.unitValue(this.minecraft.level, hovered.getItem()));
            lines.add(Component.translatable("gui.flashstake.value", format(value)).withStyle(ChatFormatting.GRAY));
            ItemStack input = this.menu.getInputStack();
            if (!input.isEmpty()) {
                double estimate = UpgradeOdds.chance(this.minecraft.level, input, hovered.getItem(), 1);
                lines.add(Component.translatable("gui.flashstake.estimate", String.format(Locale.ROOT, "%.2f%%", estimate * 100.0)).withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private void ensureCatalog() {
        if (this.catalog != null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        this.catalog = new ArrayList<ItemStack>();
        for (Item item : ItemValues.catalog((Level)this.minecraft.level)) {
            this.catalog.add(new ItemStack((ItemLike)item));
        }
    }

    private void openPicker() {
        this.ensureCatalog();
        if (this.catalog == null) {
            return;
        }
        this.pickerOpen = true;
        this.scrollRow = 0;
        this.searchBox.setValue("");
        this.minPriceBox.setValue("");
        this.maxPriceBox.setValue("");
        this.searchBox.setFocused(true);
        this.minPriceBox.setFocused(false);
        this.maxPriceBox.setFocused(false);
        if (this.pickerSortButton != null) {
            this.pickerSortButton.visible = true;
        }
        this.refilter();
    }

    private void closePicker() {
        this.pickerOpen = false;
        this.scrollDragging = false;
        this.searchBox.setFocused(false);
        this.minPriceBox.setFocused(false);
        this.maxPriceBox.setFocused(false);
        if (this.pickerSortButton != null) {
            this.pickerSortButton.visible = false;
        }
    }

    private long parsePrice(EditBox box) {
        if (box == null) {
            return -1L;
        }
        String raw = box.getValue().trim().replace(" ", "").replace(',', '.');
        if (raw.isEmpty()) {
            return -1L;
        }
        try {
            if (raw.endsWith("k") || raw.endsWith("K")) {
                return Math.round(Double.parseDouble(raw.substring(0, raw.length() - 1)) * 1000.0);
            }
            if (raw.endsWith("m") || raw.endsWith("M")) {
                return Math.round(Double.parseDouble(raw.substring(0, raw.length() - 1)) * 1_000_000.0);
            }
            return Math.round(Double.parseDouble(raw));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private void refilter() {
        if (this.catalog == null || this.minecraft == null || this.minecraft.level == null) {
            this.filtered = List.of();
            return;
        }
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        long min = this.parsePrice(this.minPriceBox);
        long max = this.parsePrice(this.maxPriceBox);
        ArrayList<ItemStack> matches = new ArrayList<>();
        for (ItemStack stack : this.catalog) {
            long value = Math.round(ItemValues.unitValue(this.minecraft.level, stack.getItem()));
            if (min >= 0L && value < min) {
                continue;
            }
            if (max >= 0L && value > max) {
                continue;
            }
            if (!query.isEmpty()) {
                String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
                if (!name.contains(query) && !id.contains(query)) {
                    continue;
                }
            }
            matches.add(stack);
        }
        Comparator<ItemStack> byValue = Comparator.comparingLong(
            s -> Math.round(ItemValues.unitValue(this.minecraft.level, s.getItem())));
        switch (this.pickerSort) {
            case VALUE_DESC -> matches.sort(byValue.reversed());
            case VALUE_ASC -> matches.sort(byValue);
            case NAME -> matches.sort(Comparator.comparing(s -> s.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
        }
        this.filtered = matches;
        this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScrollRow());
    }

    private int maxScrollRow() {
        int rows = (this.filtered.size() + GRID_COLS - 1) / GRID_COLS;
        return Math.max(0, rows - GRID_ROWS);
    }

    private int scrollTrackTop() {
        return this.topPos + GRID_Y;
    }

    private int scrollTrackHeight() {
        return GRID_ROWS * CELL;
    }

    private void renderPickerScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int max = this.maxScrollRow();
        int trackX = this.leftPos + SCROLL_X;
        int trackY = this.scrollTrackTop();
        int trackH = this.scrollTrackHeight();
        graphics.fill(trackX, trackY, trackX + SCROLL_W, trackY + trackH, 0xFF12141C);
        if (max <= 0) {
            return;
        }
        int thumbH = Math.max(16, trackH * GRID_ROWS / (max + GRID_ROWS));
        int thumbY = trackY + (int) ((trackH - thumbH) * ((double) this.scrollRow / (double) max));
        boolean over = inBox(mouseX, mouseY, trackX - 2, thumbY, SCROLL_W + 4, thumbH);
        graphics.fill(trackX, thumbY, trackX + SCROLL_W, thumbY + thumbH, over || this.scrollDragging ? UiTheme.NEON_CYAN : 0xFF4A5568);
    }

    private boolean applyScrollbarDrag(double mouseY) {
        int max = this.maxScrollRow();
        if (max <= 0) {
            return false;
        }
        int trackY = this.scrollTrackTop();
        int trackH = this.scrollTrackHeight();
        int thumbH = Math.max(16, trackH * GRID_ROWS / (max + GRID_ROWS));
        double rel = (mouseY - trackY - thumbH / 2.0) / Math.max(1, trackH - thumbH);
        this.scrollRow = Mth.clamp((int) Math.round(rel * max), 0, max);
        return true;
    }

    private double targetValueFor(double wanted) {
        return wanted;
    }

    private double targetValueForChance(double wantedChance) {
        return Config.houseEdge * (double)this.inputValue / wantedChance;
    }

    private void applyPreset(double wantedValue) {
        this.ensureCatalog();
        if (this.catalog == null || wantedValue <= 0.0) {
            return;
        }
        Item best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemStack stack : this.catalog) {
            double value = ItemValues.unitValue((Level)this.minecraft.level, stack.getItem());
            double distance = Math.abs(value - wantedValue);
            if (!(distance < bestDistance)) continue;
            bestDistance = distance;
            best = stack.getItem();
        }
        if (best != null) {
            ModNetwork.sendToServer(ServerboundSetTargetPacket.of(best, 1));
            this.playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        }
    }

    private void adjustTargetCount(int direction, boolean shifted) {
        Item target = this.menu.getTarget();
        if (target == null || this.spinning) {
            return;
        }
        int step = shifted ? 8 : 1;
        int next = UpgraderMenu.clampCount(target, this.menu.getTargetCount() + direction * step);
        if (next == this.menu.getTargetCount()) {
            return;
        }
        ModNetwork.sendToServer(ServerboundSetTargetPacket.of(target, next));
        this.playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
    }

    private void selectTarget(Item item, int count) {
        ModNetwork.sendToServer(ServerboundSetTargetPacket.of(item, count));
        this.playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = this.fitX(mouseX);
        mouseY = this.fitY(mouseY);
        if (this.pickerOpen) {
            int index;
            if (this.pickerSortButton != null && this.pickerSortButton.visible
                && this.pickerSortButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
                this.searchBox.setFocused(true);
                this.minPriceBox.setFocused(false);
                this.maxPriceBox.setFocused(false);
                return true;
            }
            if (this.minPriceBox.mouseClicked(mouseX, mouseY, button)) {
                this.minPriceBox.setFocused(true);
                this.searchBox.setFocused(false);
                this.maxPriceBox.setFocused(false);
                return true;
            }
            if (this.maxPriceBox.mouseClicked(mouseX, mouseY, button)) {
                this.maxPriceBox.setFocused(true);
                this.searchBox.setFocused(false);
                this.minPriceBox.setFocused(false);
                return true;
            }
            this.searchBox.setFocused(false);
            this.minPriceBox.setFocused(false);
            this.maxPriceBox.setFocused(false);
            int trackX = this.leftPos + SCROLL_X;
            int trackY = this.scrollTrackTop();
            int trackH = this.scrollTrackHeight();
            if (button == 0 && inBox((int) mouseX, (int) mouseY, trackX - 2, trackY, SCROLL_W + 4, trackH)) {
                this.scrollDragging = true;
                this.applyScrollbarDrag(mouseY);
                return true;
            }
            int first = this.scrollRow * GRID_COLS;
            for (int i = 0; i < GRID_COLS * GRID_ROWS && (index = first + i) < this.filtered.size(); ++i) {
                int cx = this.leftPos + GRID_X + i % GRID_COLS * CELL;
                int cy = this.topPos + GRID_Y + i / GRID_COLS * CELL;
                if (!UpgraderScreen.inBox((int)mouseX, (int)mouseY, cx, cy, CELL, CELL)) continue;
                this.selectTarget(this.filtered.get(index).getItem(), 1);
                this.closePicker();
                return true;
            }
            // Click outside the whole upgrader panel closes the picker
            if (!UpgraderScreen.inBox((int)mouseX, (int)mouseY, this.leftPos, this.topPos, WIDTH, HEIGHT)) {
                this.closePicker();
            }
            return true; // swallow clicks so inventory behind never receives them
        }
        if (!this.spinning && UpgraderScreen.inBox((int)mouseX, (int)mouseY, this.leftPos + TARGET_X, this.topPos + TARGET_Y, 18, 18)) {
            if (button == 1) {
                ModNetwork.sendToServer(ServerboundSetTargetPacket.clear());
            } else {
                this.openPicker();
            }
            this.playUi((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX = this.fitX(mouseX);
        mouseY = this.fitY(mouseY);
        if (button == 0 && this.scrollDragging) {
            this.scrollDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float fit = this.fitScale();
        mouseX = UiFit.mouseX(mouseX, this.leftPos, WIDTH, fit);
        mouseY = UiFit.mouseY(mouseY, this.topPos, HEIGHT, fit);
        dragX = UiFit.delta(dragX, fit);
        dragY = UiFit.delta(dragY, fit);
        if (this.pickerOpen && this.scrollDragging && button == 0) {
            this.applyScrollbarDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        mouseX = this.fitX(mouseX);
        mouseY = this.fitY(mouseY);
        if (this.pickerOpen) {
            this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(delta), 0, this.maxScrollRow());
            return true;
        }
        if (!this.spinning && this.menu.getTarget() != null
            && inBox((int) mouseX, (int) mouseY, this.leftPos + CARD_R_X1, this.topPos + CARD_Y1, CARD_W, CARD_Y2 - CARD_Y1)) {
            this.adjustTargetCount(delta > 0 ? 1 : -1, hasShiftDown());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.pickerOpen) {
            if (keyCode == 256) {
                this.closePicker();
                return true;
            }
            if (this.minPriceBox.isFocused() && this.minPriceBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (this.maxPriceBox.isFocused() && this.maxPriceBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (this.chanceBox != null && this.chanceBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                this.parseChanceBox(this.chanceBox.getValue());
                this.chanceBox.setValue(formatChancePercent(this.preferredChance));
                if (this.autoPreferredChance) {
                    this.applyPreferredChance();
                }
                return true;
            }
            if (this.chanceBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (this.pickerOpen) {
            if (this.minPriceBox.isFocused()) {
                return this.minPriceBox.charTyped(codePoint, modifiers);
            }
            if (this.maxPriceBox.isFocused()) {
                return this.maxPriceBox.charTyped(codePoint, modifiers);
            }
            this.searchBox.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 19, y + 19, -12761506);
        graphics.fill(x, y, x + 18, y + 18, -16052974);
    }

    private static boolean inBox(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int wrapped(GuiGraphics graphics, Component text, int cx, int y, int width, int color) {
        for (FormattedCharSequence line : this.font.split((FormattedText)text, width)) {
            graphics.drawString(this.font, line, cx - this.font.width(line) / 2, y, color, false);
            y += 10;
        }
        return y;
    }

    private static String format(long value) {
        if (value >= 1000000L) {
            return String.format(Locale.ROOT, "%.1fM", (double)value / 1000000.0);
        }
        if (value >= 10000L) {
            return String.format(Locale.ROOT, "%.1fK", (double)value / 1000.0);
        }
        return Long.toString(value);
    }
}
