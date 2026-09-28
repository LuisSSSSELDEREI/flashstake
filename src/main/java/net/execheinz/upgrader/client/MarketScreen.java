package net.execheinz.upgrader.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.menu.MarketMenu;
import net.execheinz.upgrader.network.ClientboundMarketStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.ServerboundMarketBuyPacket;
import net.execheinz.upgrader.network.ServerboundMarketSellPacket;
import net.execheinz.upgrader.network.ServerboundOpenUpgraderPacket;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class MarketScreen extends AbstractContainerScreen<MarketMenu> {
    private enum SortMode {
        VALUE_DESC, VALUE_ASC, NAME
    }

    private static final int GRID_COLS = 17;
    private static final int GRID_ROWS = 6;
    private static final int GRID_X = 12;
    private static final int GRID_Y = 58;
    private static final int CELL = 18;
    private static final int FOOTER_Y = 174;
    private static final int BUY_PANEL_Y1 = 28;
    private static final int BUY_PANEL_Y2 = 200;
    private static final int SCROLL_X = 322;
    private static final int SCROLL_W = 6;

    private static long clientBalance;
    private static Map<String, Integer> clientStock = Map.of();
    private static long clientRefreshInMs;
    private static long clientRefreshSyncedAt;
    private static int clientSellRemaining = 100;
    private static long clientSellWindowMsLeft;
    private static long clientSellSyncedAt;

    private boolean buyMode = true;
    private SortMode sortMode = SortMode.VALUE_DESC;
    private EditBox searchBox;
    private EditBox minPriceBox;
    private EditBox maxPriceBox;
    private List<ItemStack> catalog = List.of();
    private List<ItemStack> filtered = List.of();
    private int scrollRow;
    private boolean scrollDragging;
    private StyledButton buyTab;
    private StyledButton sellTab;
    private StyledButton backButton;
    private StyledButton sortButton;

    private Item buyItem;
    private int buyCount = 1;
    private EditBox buyQtyBox;
    private StyledButton buyMinus;
    private StyledButton buyPlus;
    private StyledButton buyMax;
    private StyledButton buyConfirm;
    private StyledButton buyCancel;
    private StyledButton sellConfirm;

    public MarketScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 340;
        this.imageHeight = 320;
        this.inventoryLabelY = 10000;
    }

    public static void setClientBalance(long balance) {
        clientBalance = balance;
    }

    public static long getClientBalance() {
        return clientBalance;
    }

    public static void applyMarketState(ClientboundMarketStatePacket packet) {
        clientStock = Map.copyOf(packet.stock());
        clientRefreshInMs = packet.refreshInMs();
        clientRefreshSyncedAt = System.currentTimeMillis();
        clientSellRemaining = packet.sellRemaining();
        clientSellWindowMsLeft = packet.sellWindowMsLeft();
        clientSellSyncedAt = System.currentTimeMillis();
    }

    private static int stockOf(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return 0;
        }
        return clientStock.getOrDefault(key.toString(), 0);
    }

    private static long refreshLeftMs() {
        return Math.max(0L, clientRefreshInMs - (System.currentTimeMillis() - clientRefreshSyncedAt));
    }

    private static long sellWindowLeftMs() {
        return Math.max(0L, clientSellWindowMsLeft - (System.currentTimeMillis() - clientSellSyncedAt));
    }

    /** Remaining units; when the local timer hits 0, treat as a full reset until the next sync. */
    private static int sellRemaining() {
        return sellWindowLeftMs() <= 0L ? Config.marketSellLimit : clientSellRemaining;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Tabs — flush top corners
        this.buyTab = this.addRenderableWidget(StyledButton.neon(x + 8, y + 4, 60, 16,
            Component.translatable("gui.flashstake.market.buy"), UiTheme.ACCENT_BUY, b -> {
                this.closeBuyPanel();
                this.buyMode = true;
                this.scrollRow = 0;
                this.refreshFilterWidgets();
            }));
        this.sellTab = this.addRenderableWidget(StyledButton.neon(x + 72, y + 4, 60, 16,
            Component.translatable("gui.flashstake.market.sell"), UiTheme.ACCENT_SELL, b -> {
                this.closeBuyPanel();
                this.buyMode = false;
                this.scrollRow = 0;
                this.refreshFilterWidgets();
            }));
        this.backButton = this.addRenderableWidget(StyledButton.neon(x + this.imageWidth - 68, y + 4, 60, 16,
            Component.translatable("gui.flashstake.market.back"), UiTheme.NEON_CYAN,
            b -> {
                UiCursor.captureIfInFlashStakeUi();
                ModNetwork.sendToServer(new ServerboundOpenUpgraderPacket());
            }));
        UiCursor.scheduleRestore();

        this.searchBox = new EditBox(this.font, x + 12, y + 36, 160, 16, Component.translatable("gui.flashstake.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(UiTheme.TEXT);
        this.searchBox.setHint(Component.translatable("gui.flashstake.search").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(t -> {
            this.scrollRow = 0;
            this.refilter();
        });
        this.addRenderableWidget(this.searchBox);

        this.minPriceBox = new EditBox(this.font, x + 210, y + 36, 48, 16, Component.literal("min"));
        this.minPriceBox.setMaxLength(10);
        this.minPriceBox.setHint(Component.translatable("gui.flashstake.market.min"));
        this.minPriceBox.setTextColor(UiTheme.NEON_LIME);
        this.minPriceBox.setResponder(t -> {
            this.scrollRow = 0;
            this.refilter();
        });
        this.addRenderableWidget(this.minPriceBox);

        this.maxPriceBox = new EditBox(this.font, x + 270, y + 36, 48, 16, Component.literal("max"));
        this.maxPriceBox.setMaxLength(10);
        this.maxPriceBox.setHint(Component.translatable("gui.flashstake.market.max"));
        this.maxPriceBox.setTextColor(UiTheme.NEON_MAGENTA);
        this.maxPriceBox.setResponder(t -> {
            this.scrollRow = 0;
            this.refilter();
        });
        this.addRenderableWidget(this.maxPriceBox);

        this.sortButton = this.addRenderableWidget(StyledButton.neon(x + 12, y + FOOTER_Y, 96, 14,
            this.sortLabel(), UiTheme.NEON_PURPLE, b -> {
                this.sortMode = switch (this.sortMode) {
                    case VALUE_DESC -> SortMode.VALUE_ASC;
                    case VALUE_ASC -> SortMode.NAME;
                    case NAME -> SortMode.VALUE_DESC;
                };
                this.sortButton.setMessage(this.sortLabel());
                this.refilter();
            }));

        // Buy qty — same pattern as upgrader preferred-% row: X / - / box / + / MAX
        int qtyY = y + BUY_PANEL_Y1 + 78;
        int btnY = y + BUY_PANEL_Y1 + 106;
        int qx = x + 40;
        this.buyCancel = this.addRenderableWidget(StyledButton.neon(qx, qtyY, 18, 20,
            Component.literal("X"), UiTheme.NEON_MAGENTA, b -> this.closeBuyPanel()));
        this.buyMinus = this.addRenderableWidget(StyledButton.neon(qx + 22, qtyY, 18, 20,
            Component.literal("-"), UiTheme.TEXT, b -> this.adjustBuyCount(hasShiftDown() ? -10 : -1)));
        this.buyQtyBox = new EditBox(this.font, qx + 44, qtyY + 3, 46, 14, Component.literal("qty"));
        this.buyQtyBox.setMaxLength(4);
        this.buyQtyBox.setTextColor(UiTheme.NEON_CYAN);
        this.buyQtyBox.setResponder(this::parseBuyQty);
        this.addRenderableWidget(this.buyQtyBox);
        this.buyPlus = this.addRenderableWidget(StyledButton.neon(qx + 94, qtyY, 18, 20,
            Component.literal("+"), UiTheme.TEXT, b -> this.adjustBuyCount(hasShiftDown() ? 10 : 1)));
        this.buyMax = this.addRenderableWidget(StyledButton.neon(qx + 116, qtyY, 44, 20,
            Component.translatable("gui.flashstake.market.max_btn"), UiTheme.NEON_GOLD, b -> this.setBuyCount(this.maxBuyable())));
        this.buyConfirm = this.addRenderableWidget(StyledButton.gold(qx, btnY, 160, 22,
            Component.translatable("gui.flashstake.market.confirm"), b -> this.confirmBuy()));

        this.sellConfirm = this.addRenderableWidget(StyledButton.gold(x + MarketMenu.SELL_X, y + MarketMenu.SELL_Y + 24, 162, 22,
            Component.translatable("gui.flashstake.market.sell_confirm"), b -> this.confirmSell()));

        this.ensureCatalog();
        this.refilter();
        this.refreshFilterWidgets();
        this.setBuyPanelVisible(false);
        this.refreshSellButton();
    }

    private Component sortLabel() {
        return Component.translatable(switch (this.sortMode) {
            case VALUE_DESC -> "gui.flashstake.market.sort_high";
            case VALUE_ASC -> "gui.flashstake.market.sort_low";
            case NAME -> "gui.flashstake.market.sort_name";
        });
    }

    private boolean buyPanelOpen() {
        return this.buyItem != null;
    }

    private void openBuyPanel(Item item) {
        this.buyItem = item;
        int max = this.maxBuyable();
        this.buyCount = Math.max(1, Math.min(1, Math.max(1, max)));
        if (max <= 0) {
            this.buyCount = 1;
        }
        if (this.buyQtyBox != null) {
            this.buyQtyBox.setValue(Integer.toString(this.buyCount));
        }
        this.setBuyPanelVisible(true);
        this.refreshBuyButtons();
        this.refreshFilterWidgets();
    }

    private void closeBuyPanel() {
        this.buyItem = null;
        this.setBuyPanelVisible(false);
        this.refreshFilterWidgets();
    }

    private void setBuyPanelVisible(boolean visible) {
        if (this.buyMinus != null) {
            this.buyMinus.visible = visible;
        }
        if (this.buyPlus != null) {
            this.buyPlus.visible = visible;
        }
        if (this.buyMax != null) {
            this.buyMax.visible = visible;
        }
        if (this.buyConfirm != null) {
            this.buyConfirm.visible = visible;
        }
        if (this.buyCancel != null) {
            this.buyCancel.visible = visible;
        }
        if (this.buyQtyBox != null) {
            this.buyQtyBox.visible = visible;
        }
    }

    private int maxBuyable() {
        if (this.buyItem == null || this.minecraft == null || this.minecraft.level == null) {
            return 0;
        }
        int stock = stockOf(this.buyItem);
        long unit = Math.max(1L, Math.round(ItemValues.unitValue(this.minecraft.level, this.buyItem) * Config.marketBuyRate));
        int afford = clientBalance <= 0L ? 0 : (int) Math.min(stock, clientBalance / unit);
        return Math.max(0, Math.min(stock, afford));
    }

    private void adjustBuyCount(int delta) {
        this.setBuyCount(this.buyCount + delta);
    }

    private void setBuyCount(int count) {
        int max = Math.max(1, this.maxBuyable());
        this.buyCount = Mth.clamp(count, 1, Math.max(1, max));
        if (this.buyQtyBox != null && !this.buyQtyBox.getValue().equals(Integer.toString(this.buyCount))) {
            this.buyQtyBox.setValue(Integer.toString(this.buyCount));
        }
        this.refreshBuyButtons();
    }

    private void parseBuyQty(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.isEmpty()) {
                return;
            }
            this.buyCount = Mth.clamp(Integer.parseInt(cleaned), 1, Math.max(1, this.maxBuyable()));
            this.refreshBuyButtons();
        } catch (NumberFormatException ignored) {
        }
    }

    private void refreshBuyButtons() {
        int max = this.maxBuyable();
        boolean ok = this.buyPanelOpen() && max > 0 && this.buyCount > 0 && this.buyCount <= max;
        if (this.buyConfirm != null) {
            this.buyConfirm.active = ok;
        }
        if (this.buyMinus != null) {
            this.buyMinus.active = this.buyCount > 1;
        }
        if (this.buyPlus != null) {
            this.buyPlus.active = this.buyCount < max;
        }
        if (this.buyMax != null) {
            this.buyMax.active = max > 0;
        }
    }

    private void confirmBuy() {
        if (this.buyItem == null || this.buyCount <= 0) {
            return;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(this.buyItem);
        if (key == null) {
            return;
        }
        ModNetwork.sendToServer(new ServerboundMarketBuyPacket(key.toString(), this.buyCount));
        this.playClick();
        this.closeBuyPanel();
    }

    private void confirmSell() {
        int units = this.menu.sellUnitCount();
        int left = sellRemaining();
        if (units <= 0 || units > left) {
            this.playClick();
            return;
        }
        ModNetwork.sendToServer(new ServerboundMarketSellPacket());
        this.playClick();
    }

    private void refreshSellButton() {
        if (this.sellConfirm == null) {
            return;
        }
        boolean sell = !this.buyMode && !this.buyPanelOpen();
        this.sellConfirm.visible = sell;
        if (!sell) {
            return;
        }
        int units = this.menu.sellUnitCount();
        long value = this.minecraft != null && this.minecraft.level != null
            ? this.menu.sellTotalValue(this.minecraft.level) : 0L;
        int left = sellRemaining();
        this.sellConfirm.active = units > 0 && units <= left && value > 0L;
    }

    private void refreshFilterWidgets() {
        boolean buy = this.buyMode && !this.buyPanelOpen();
        this.menu.sellTrayVisible = !this.buyMode && !this.buyPanelOpen();
        if (this.searchBox != null) {
            this.searchBox.visible = buy;
        }
        if (this.minPriceBox != null) {
            this.minPriceBox.visible = buy;
        }
        if (this.maxPriceBox != null) {
            this.maxPriceBox.visible = buy;
        }
        if (this.sortButton != null) {
            this.sortButton.visible = buy;
        }
        this.refreshSellButton();
    }

    private void ensureCatalog() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        ArrayList<ItemStack> items = new ArrayList<>();
        for (Item item : ItemValues.catalog(this.minecraft.level)) {
            items.add(new ItemStack(item));
        }
        this.catalog = items;
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
        if (this.minecraft == null || this.minecraft.level == null) {
            this.filtered = List.of();
            return;
        }
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        long min = this.parsePrice(this.minPriceBox);
        long max = this.parsePrice(this.maxPriceBox);
        ArrayList<ItemStack> matches = new ArrayList<>();
        for (ItemStack stack : this.catalog) {
            long value = Math.round(ItemValues.unitValue(this.minecraft.level, stack.getItem()) * Config.marketBuyRate);
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
            s -> Math.round(ItemValues.unitValue(this.minecraft.level, s.getItem()) * Config.marketBuyRate));
        switch (this.sortMode) {
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

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int max = this.maxScrollRow();
        int trackX = this.leftPos + SCROLL_X;
        int trackY = this.topPos + GRID_Y;
        int trackH = GRID_ROWS * CELL;
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
        int trackY = this.topPos + GRID_Y;
        int trackH = GRID_ROWS * CELL;
        int thumbH = Math.max(16, trackH * GRID_ROWS / (max + GRID_ROWS));
        double rel = (mouseY - trackY - thumbH / 2.0) / Math.max(1, trackH - thumbH);
        this.scrollRow = Mth.clamp((int) Math.round(rel * max), 0, max);
        return true;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.buyPanelOpen()) {
            this.refreshBuyButtons();
        }
        this.refreshSellButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiCursor.tickInRender();
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        // Force buy qty controls above slots/panel so they stay clickable
        if (this.buyPanelOpen()) {
            this.renderBuyControls(graphics, mouseX, mouseY, partialTick);
        }
        this.renderBalanceBanner(graphics);
        if (!this.buyPanelOpen()) {
            if (this.buyMode) {
                graphics.drawString(this.font, Component.translatable("gui.flashstake.market.filters"),
                    this.leftPos + 180, this.topPos + 40, UiTheme.TEXT_DIM, false);
                this.renderBuyGrid(graphics, mouseX, mouseY);
                this.renderScrollbar(graphics, mouseX, mouseY);
                int rows = Math.max(1, (this.filtered.size() + GRID_COLS - 1) / GRID_COLS);
                int page = this.scrollRow + 1;
                int pages = Math.max(1, rows - GRID_ROWS + 1);
                Component results = Component.translatable("gui.flashstake.market.results", this.filtered.size(), page, pages);
                graphics.drawString(this.font, results,
                    this.leftPos + 118, this.topPos + FOOTER_Y + 3, UiTheme.TEXT_MUTED, false);
                graphics.drawString(this.font,
                    Component.translatable("gui.flashstake.market.restock_in", formatDuration(refreshLeftMs())),
                    this.leftPos + 10, this.topPos + FOOTER_Y + 18, UiTheme.TEXT_DIM, false);
            } else {
                int units = this.menu.sellUnitCount();
                int left = sellRemaining();
                long payout = this.minecraft != null && this.minecraft.level != null
                    ? this.menu.sellTotalValue(this.minecraft.level) : 0L;
                graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.market.sell_hint"),
                    this.leftPos + this.imageWidth / 2, this.topPos + 48, UiTheme.TEXT_DIM);
                graphics.drawCenteredString(this.font,
                    Component.translatable("gui.flashstake.market.sell_quota", left, Config.marketSellLimit, formatDuration(sellWindowLeftMs())),
                    this.leftPos + this.imageWidth / 2, this.topPos + 64, UiTheme.NEON_GOLD);
                graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.market.sell_tray"),
                    this.leftPos + this.imageWidth / 2, this.topPos + MarketMenu.SELL_Y - 12, UiTheme.TEXT_DIM);
                int usesColor = units > left ? UiTheme.NEON_MAGENTA : UiTheme.TEXT_MUTED;
                graphics.drawCenteredString(this.font,
                    Component.translatable("gui.flashstake.market.sell_uses", units, left),
                    this.leftPos + this.imageWidth / 2, this.topPos + MarketMenu.SELL_Y + 50, usesColor);
                if (payout > 0L) {
                    graphics.drawCenteredString(this.font,
                        Component.translatable("gui.flashstake.market.sell_total", format(payout)),
                        this.leftPos + this.imageWidth / 2, this.topPos + MarketMenu.SELL_Y + 62, UiTheme.NEON_LIME);
                }
                this.renderSellHover(graphics, mouseX, mouseY);
            }
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderBuyControls(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.buyCancel != null) {
            this.buyCancel.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.buyMinus != null) {
            this.buyMinus.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.buyQtyBox != null) {
            this.buyQtyBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.buyPlus != null) {
            this.buyPlus.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.buyMax != null) {
            this.buyMax.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.buyConfirm != null) {
            this.buyConfirm.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderBuyPanel(GuiGraphics graphics) {
        if (this.buyItem == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        int x1 = this.leftPos + 20;
        int y1 = this.topPos + BUY_PANEL_Y1;
        int x2 = this.leftPos + this.imageWidth - 20;
        int y2 = this.topPos + BUY_PANEL_Y2;
        WheelRenderer.neonPanel(graphics, x1, y1, x2, y2, 6, UiTheme.NEON_GOLD);
        graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, 0xE008101C);

        ItemStack stack = new ItemStack(this.buyItem);
        graphics.renderItem(stack, x1 + 12, y1 + 12);
        graphics.renderItemDecorations(this.font, stack, x1 + 12, y1 + 12);
        graphics.drawString(this.font, stack.getHoverName(), x1 + 36, y1 + 12, UiTheme.TEXT, false);

        long unit = Math.round(ItemValues.unitValue(this.minecraft.level, this.buyItem) * Config.marketBuyRate);
        int stock = stockOf(this.buyItem);
        long total = unit * (long) Math.max(1, this.buyCount);
        graphics.drawString(this.font, Component.translatable("gui.flashstake.market.unit_price", format(unit)),
            x1 + 36, y1 + 26, UiTheme.TEXT_DIM, false);
        graphics.drawString(this.font, Component.translatable("gui.flashstake.market.stock", stock),
            x1 + 12, y1 + 44, stock > 0 ? UiTheme.NEON_LIME : UiTheme.NEON_MAGENTA, false);
        graphics.drawString(this.font, Component.translatable("gui.flashstake.market.total", format(total)),
            x1 + 12, y1 + 58, UiTheme.NEON_GOLD, false);
    }

    private void renderBalanceBanner(GuiGraphics graphics) {
        // Compact balance between sell and back tabs (top row)
        Component label = Component.translatable("gui.flashstake.market.balance", format(clientBalance));
        int tw = this.font.width(label);
        int cx = this.leftPos + this.imageWidth / 2;
        int y1 = this.topPos + 4;
        int x1 = cx - tw / 2 - 8;
        int x2 = cx + tw / 2 + 8;
        WheelRenderer.neonBanner(graphics, x1, y1, x2, y1 + 16, UiTheme.NEON_GOLD);
        graphics.drawCenteredString(this.font, label, cx, y1 + 4, UiTheme.NEON_GOLD);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        WheelRenderer.neonPanel(graphics, this.leftPos, this.topPos,
            this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 8, UiTheme.NEON_CYAN);
        if (this.buyPanelOpen()) {
            this.renderBuyPanel(graphics);
            return;
        }
        if (!this.buyMode) {
            for (int col = 0; col < MarketMenu.SELL_SLOTS; ++col) {
                WheelRenderer.neonSlot(graphics, this.leftPos + MarketMenu.SELL_X + col * 18, this.topPos + MarketMenu.SELL_Y);
            }
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                WheelRenderer.neonSlot(graphics, this.leftPos + MarketMenu.INV_X + col * 18, this.topPos + MarketMenu.INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; ++col) {
            WheelRenderer.neonSlot(graphics, this.leftPos + MarketMenu.INV_X + col * 18, this.topPos + MarketMenu.HOTBAR_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderBuyGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int first = this.scrollRow * GRID_COLS;
        ItemStack hovered = null;
        for (int i = 0; i < GRID_COLS * GRID_ROWS && first + i < this.filtered.size(); ++i) {
            int cx = this.leftPos + GRID_X + (i % GRID_COLS) * CELL;
            int cy = this.topPos + GRID_Y + (i / GRID_COLS) * CELL;
            boolean over = inBox(mouseX, mouseY, cx, cy, CELL, CELL);
            ItemStack stack = this.filtered.get(first + i);
            int stock = stockOf(stack.getItem());
            graphics.fill(cx, cy, cx + CELL, cy + CELL, over ? 0xFF1A2840 : UiTheme.SLOT_BG);
            if (stock <= 0) {
                graphics.fill(cx, cy, cx + CELL, cy + CELL, 0x66000000);
            }
            if (over) {
                graphics.fill(cx, cy, cx + CELL, cy + 1, UiTheme.NEON_CYAN);
                graphics.fill(cx, cy + CELL - 1, cx + CELL, cy + CELL, UiTheme.NEON_CYAN);
            }
            graphics.renderItem(stack, cx + 1, cy + 1);
            if (stock > 0 && stock < 100) {
                graphics.pose().pushPose();
                graphics.pose().translate(cx + 10, cy + 10, 200);
                graphics.pose().scale(0.7f, 0.7f, 1.0f);
                graphics.drawString(this.font, Integer.toString(stock), 0, 0, UiTheme.NEON_LIME, true);
                graphics.pose().popPose();
            }
            if (over) {
                hovered = stack;
            }
        }
        if (hovered != null && this.minecraft != null && this.minecraft.level != null) {
            long value = Math.round(ItemValues.unitValue(this.minecraft.level, hovered.getItem()) * Config.marketBuyRate);
            int stock = stockOf(hovered.getItem());
            graphics.renderComponentTooltip(this.font, List.of(
                hovered.getHoverName(),
                Component.translatable("gui.flashstake.value", format(value)).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.flashstake.market.stock", stock).withStyle(stock > 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
                Component.translatable("gui.flashstake.market.buy_hint").withStyle(ChatFormatting.DARK_GRAY)
            ), mouseX, mouseY);
        }
    }

    private void renderSellHover(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        int left = sellRemaining();
        for (int i = 0; i < MarketMenu.SELL_SLOTS; ++i) {
            var slot = this.menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (!inBox(mouseX, mouseY, sx, sy, 16, 16)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            long value = Math.round(ItemValues.stackValue(this.minecraft.level, stack) * Config.marketSellRate);
            graphics.renderComponentTooltip(this.font, List.of(
                stack.getHoverName(),
                Component.translatable("gui.flashstake.value", format(value)).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.flashstake.market.sell_for", format(value)).withStyle(ChatFormatting.GREEN),
                Component.translatable("gui.flashstake.market.sell_uses", this.menu.sellUnitCount(), left)
                    .withStyle(this.menu.sellUnitCount() <= left ? ChatFormatting.GRAY : ChatFormatting.RED)
            ), mouseX, mouseY);
            return;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.buyPanelOpen()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.buyMode) {
            int trackX = this.leftPos + SCROLL_X;
            int trackY = this.topPos + GRID_Y;
            int trackH = GRID_ROWS * CELL;
            if (button == 0 && inBox((int) mouseX, (int) mouseY, trackX - 2, trackY, SCROLL_W + 4, trackH)) {
                this.scrollDragging = true;
                this.applyScrollbarDrag(mouseY);
                return true;
            }
            int first = this.scrollRow * GRID_COLS;
            for (int i = 0; i < GRID_COLS * GRID_ROWS && first + i < this.filtered.size(); ++i) {
                int cx = this.leftPos + GRID_X + (i % GRID_COLS) * CELL;
                int cy = this.topPos + GRID_Y + (i / GRID_COLS) * CELL;
                if (!inBox((int) mouseX, (int) mouseY, cx, cy, CELL, CELL)) {
                    continue;
                }
                Item item = this.filtered.get(first + i).getItem();
                if (stockOf(item) <= 0) {
                    this.playClick();
                    return true;
                }
                this.openBuyPanel(item);
                this.playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.scrollDragging) {
            this.scrollDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.buyMode && !this.buyPanelOpen() && this.scrollDragging && button == 0) {
            this.applyScrollbarDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.buyMode && !this.buyPanelOpen()) {
            this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(scrollY) * Math.max(1, (int) Math.abs(scrollY)), 0, this.maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.buyPanelOpen()) {
                this.closeBuyPanel();
                return true;
            }
            this.onClose();
            return true;
        }
        if (this.buyQtyBox != null && this.buyQtyBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                this.confirmBuy();
                return true;
            }
            return this.buyQtyBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        if (this.minPriceBox != null && this.minPriceBox.isFocused()) {
            return this.minPriceBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        if (this.maxPriceBox != null && this.maxPriceBox.isFocused()) {
            return this.maxPriceBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.buyQtyBox != null && this.buyQtyBox.isFocused()) {
            return this.buyQtyBox.charTyped(codePoint, modifiers);
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        if (this.minPriceBox != null && this.minPriceBox.isFocused()) {
            return this.minPriceBox.charTyped(codePoint, modifiers);
        }
        if (this.maxPriceBox != null && this.maxPriceBox.isFocused()) {
            return this.maxPriceBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.35f));
        }
    }

    private static boolean inBox(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String format(long value) {
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fK", value / 1000.0);
        }
        return Long.toString(value);
    }

    private static String formatDuration(long ms) {
        long sec = Math.max(0L, ms / 1000L);
        long min = sec / 60L;
        long rem = sec % 60L;
        if (min >= 60L) {
            long hrs = min / 60L;
            long m = min % 60L;
            return String.format(Locale.ROOT, "%d:%02d:%02d", hrs, m, rem);
        }
        return String.format(Locale.ROOT, "%d:%02d", min, rem);
    }
}
