package net.execheinz.upgrader.client;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.cases.CaseLoot;
import net.execheinz.upgrader.economy.CaseStash;
import net.execheinz.upgrader.menu.CasesMenu;
import net.execheinz.upgrader.network.ClientboundCaseResultPacket;
import net.execheinz.upgrader.network.ClientboundCaseStashPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.ServerboundCasePendingActionPacket;
import net.execheinz.upgrader.network.ServerboundCaseStashActionPacket;
import net.execheinz.upgrader.network.ServerboundOpenCasePacket;
import net.execheinz.upgrader.network.ServerboundOpenUpgraderPacket;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.ChatFormatting;
import net.execheinz.upgrader.client.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CasesScreen extends AbstractContainerScreen<CasesMenu> {
    private enum Mode { CASES, DETAIL, STASH }

    private static final int REEL_X = 20;
    private static final int REEL_W = 280;
    private static final int REEL_AREA_Y = 116;
    private static final int REEL_AREA_H = 140;
    private static final int FEED_W = 92;
    private static final int FEED_GAP = 6;
    private static final int FOOTER_Y = 286;
    private static final int STRIP_LENGTH = 56;
    private static final int WINNER_INDEX = 46;
    private static final long SLOW_BASE_MS = 5200L;
    private static final long SLOW_STAGGER_MS = 550L;
    private static final long FAST_BASE_MS = 1100L;
    private static final long FAST_STAGGER_MS = 140L;

    private static List<ItemStack> clientStash = emptyStash();
    private static List<ItemStack> clientPending = List.of();
    private static final Map<ResourceLocation, Boolean> ICON_CACHE = new HashMap<>();

    private Mode mode = Mode.CASES;
    private CaseDefinition selected;
    private boolean fast = false;
    private int openCount = 1;
    private boolean waitingForServer;
    private final List<Reel> reels = new ArrayList<>();
    private long lastTickSoundMs;
    private final Random visualRandom = new Random();

    private StyledButton backButton;
    private StyledButton casesTab;
    private StyledButton stashTab;
    private StyledButton speedButton;
    private StyledButton openButton;
    private StyledButton withdrawAllButton;
    private StyledButton sellAllButton;
    private StyledButton keepButton;
    private StyledButton sellDropButton;
    private final StyledButton[] countButtons = new StyledButton[5];

    public CasesScreen(CasesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 318;
        this.inventoryLabelY = 10000;
    }

    public static void setClientStash(List<ItemStack> slots) {
        ArrayList<ItemStack> copy = new ArrayList<>(CaseStash.SLOTS);
        for (int i = 0; i < CaseStash.SLOTS; ++i) {
            copy.add(i < slots.size() ? slots.get(i).copy() : ItemStack.EMPTY);
        }
        clientStash = copy;
    }

    public static void setClientPending(List<ItemStack> stacks) {
        ArrayList<ItemStack> copy = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }
        clientPending = copy;
    }

    public static List<ItemStack> getClientStash() {
        return clientStash;
    }

    private boolean hasPendingDecision() {
        return !clientPending.isEmpty() && !this.reels.isEmpty() && !this.spinning();
    }

    private long pendingValue() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return 0L;
        }
        long total = 0L;
        for (ItemStack stack : clientPending) {
            total += Math.max(0L, Math.round(ItemValues.stackValue(this.minecraft.level, stack)));
        }
        return total;
    }

    private static List<ItemStack> emptyStash() {
        ArrayList<ItemStack> list = new ArrayList<>(CaseStash.SLOTS);
        for (int i = 0; i < CaseStash.SLOTS; ++i) {
            list.add(ItemStack.EMPTY);
        }
        return list;
    }

    @Override
    protected void init() {
        super.init();
        UiCursor.scheduleRestore();
        // Center panel+drop-feed together so the feed never clips off-screen.
        this.leftPos = (this.width - this.imageWidth - FEED_W - FEED_GAP) / 2 + FEED_W + FEED_GAP;
        int x = this.leftPos;
        int y = this.topPos;
        this.backButton = this.addRenderableWidget(StyledButton.chip(x + 8, y + 6, 50, 16,
            Component.translatable("gui.flashstake.market.back"), -1643790, b -> this.onBack()));
        this.casesTab = this.addRenderableWidget(StyledButton.chip(x + 62, y + 6, 54, 16,
            Component.translatable("gui.flashstake.cases.tab_cases"), -865972, b -> this.setMode(Mode.CASES)));
        this.stashTab = this.addRenderableWidget(StyledButton.chip(x + 120, y + 6, 70, 16,
            Component.translatable("gui.flashstake.cases.tab_stash"), -1550516, b -> this.setMode(Mode.STASH)));

        this.speedButton = this.addRenderableWidget(StyledButton.chip(x + 8, y + 86, 70, 18, this.speedLabel(), -865972, b -> {
            this.fast = !this.fast;
            this.speedButton.setMessage(this.speedLabel());
        }));
        for (int i = 0; i < 5; ++i) {
            int count = i + 1;
            this.countButtons[i] = this.addRenderableWidget(StyledButton.chip(x + 86 + i * 24, y + 86, 22, 18,
                Component.literal(Integer.toString(count)), -1643790, b -> this.openCount = count));
        }
        this.openButton = this.addRenderableWidget(StyledButton.gold(x + 214, y + 86, 56, 18,
            Component.translatable("gui.flashstake.cases.open"), b -> this.requestOpen()));

        this.withdrawAllButton = this.addRenderableWidget(StyledButton.chip(x + 8, y + 28, 100, 18,
            Component.translatable("gui.flashstake.cases.withdraw_all"), -11740828, b ->
                ModNetwork.sendToServer(new ServerboundCaseStashActionPacket(ServerboundCaseStashActionPacket.Action.WITHDRAW_ALL, 0))));
        this.sellAllButton = this.addRenderableWidget(StyledButton.chip(x + 114, y + 28, 100, 18,
            Component.translatable("gui.flashstake.cases.sell_all"), -865972, b ->
                ModNetwork.sendToServer(new ServerboundCaseStashActionPacket(ServerboundCaseStashActionPacket.Action.SELL_ALL, 0))));
        this.keepButton = this.addRenderableWidget(StyledButton.chip(x + 68, y + FOOTER_Y, 90, 20,
            Component.translatable("gui.flashstake.cases.keep"), -11740828, b ->
                ModNetwork.sendToServer(new ServerboundCasePendingActionPacket(ServerboundCasePendingActionPacket.Action.KEEP))));
        this.sellDropButton = this.addRenderableWidget(StyledButton.gold(x + 166, y + FOOTER_Y, 90, 20,
            Component.translatable("gui.flashstake.cases.sell"), b ->
                ModNetwork.sendToServer(new ServerboundCasePendingActionPacket(ServerboundCasePendingActionPacket.Action.SELL))));
        this.updateWidgetStates();
    }

    private Component speedLabel() {
        return Component.translatable(this.fast ? "gui.flashstake.cases.fast" : "gui.flashstake.cases.slow");
    }

    private boolean spinning() {
        for (Reel reel : this.reels) {
            if (!reel.done) {
                return true;
            }
        }
        return false;
    }

    private boolean busy() {
        return this.waitingForServer || this.spinning() || this.hasPendingDecision();
    }

    private void onBack() {
        if (this.busy()) {
            return;
        }
        if (this.mode == Mode.CASES) {
            UiCursor.captureIfInFlashStakeUi();
            ModNetwork.sendToServer(new ServerboundOpenUpgraderPacket());
        } else {
            this.setMode(Mode.CASES);
        }
    }

    private void setMode(Mode next) {
        if (this.busy()) {
            return;
        }
        this.mode = next;
        if (next != Mode.DETAIL) {
            this.selected = null;
            this.reels.clear();
        }
        this.updateWidgetStates();
    }

    private void updateWidgetStates() {
        if (this.openButton == null) {
            return;
        }
        boolean busy = this.busy();
        boolean detail = this.mode == Mode.DETAIL;
        boolean stash = this.mode == Mode.STASH;

        this.backButton.active = !busy;
        this.casesTab.active = this.mode != Mode.CASES && !busy;
        this.stashTab.active = this.mode != Mode.STASH && !busy;

        this.speedButton.visible = detail;
        this.speedButton.active = !busy;
        this.openButton.visible = detail;
        boolean canAfford = this.selected != null
            && MarketScreen.getClientBalance() >= this.selected.price() * (long) this.openCount;
        this.openButton.active = !busy && canAfford;
        for (int i = 0; i < 5; ++i) {
            this.countButtons[i].visible = detail;
            this.countButtons[i].active = !busy && this.openCount != i + 1;
        }

        boolean hasItems = clientStash.stream().anyMatch(s -> !s.isEmpty());
        this.withdrawAllButton.visible = stash;
        this.sellAllButton.visible = stash;
        this.withdrawAllButton.active = hasItems;
        this.sellAllButton.active = hasItems;

        boolean decide = this.hasPendingDecision();
        this.keepButton.visible = decide;
        this.sellDropButton.visible = decide;
        this.keepButton.active = decide;
        this.sellDropButton.active = decide;
    }

    private void requestOpen() {
        if (this.selected == null || this.busy()) {
            return;
        }
        long cost = this.selected.price() * (long) this.openCount;
        if (MarketScreen.getClientBalance() < cost) {
            return;
        }
        this.waitingForServer = true;
        this.reels.clear();
        ModNetwork.sendToServer(new ServerboundOpenCasePacket(this.selected.id(), this.openCount, this.fast));
    }

    public void onCaseResult(ClientboundCaseResultPacket packet) {
        this.waitingForServer = false;
        CaseDefinition def = CaseDefinition.byId(packet.caseId());
        if (def == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        if (this.selected == null || !this.selected.id().equals(def.id())) {
            this.selected = def;
            this.mode = Mode.DETAIL;
        }
        this.reels.clear();
        double[] weights = new double[def.pool().size()];
        for (int i = 0; i < weights.length; ++i) {
            weights[i] = CaseLoot.chancePercent(this.minecraft.level, def, def.pool().get(i));
        }
        long now = System.currentTimeMillis();
        List<ItemStack> rewards = packet.rewards();
        int n = rewards.size();
        int cellW = cellWidth(reelHeight(n));
        for (int i = 0; i < n; ++i) {
            long duration = packet.fast()
                ? FAST_BASE_MS + FAST_STAGGER_MS * i
                : SLOW_BASE_MS + SLOW_STAGGER_MS * i;
            this.reels.add(this.buildReel(def, weights, rewards.get(i), cellW, now, duration));
        }
        this.lastTickSoundMs = 0L;
        this.updateWidgetStates();
    }

    public void onStashSync(ClientboundCaseStashPacket packet) {
        setClientStash(packet.slots());
        this.updateWidgetStates();
    }

    public void onPendingSync(List<ItemStack> stacks) {
        boolean had = !clientPending.isEmpty();
        setClientPending(stacks);
        if (had && clientPending.isEmpty()) {
            this.reels.clear();
        }
        this.updateWidgetStates();
    }

    private Reel buildReel(CaseDefinition def, double[] weights, ItemStack reward, int cellW, long start, long duration) {
        List<CaseDefinition.CaseEntry> pool = def.pool();
        double total = 0.0;
        for (double w : weights) {
            total += w;
        }
        ArrayList<ItemStack> strip = new ArrayList<>(STRIP_LENGTH);
        ArrayList<CaseDefinition.CaseTier> tiers = new ArrayList<>(STRIP_LENGTH);
        for (int k = 0; k < STRIP_LENGTH; ++k) {
            if (k == WINNER_INDEX) {
                strip.add(reward.copy());
                tiers.add(tierOf(def, reward));
                continue;
            }
            CaseDefinition.CaseEntry pick = pool.get(pool.size() - 1);
            double roll = this.visualRandom.nextDouble() * total;
            double cursor = 0.0;
            for (int i = 0; i < pool.size(); ++i) {
                cursor += weights[i];
                if (roll <= cursor) {
                    pick = pool.get(i);
                    break;
                }
            }
            strip.add(new ItemStack(pick.item(), pick.count()));
            tiers.add(pick.tier());
        }
        double jitter = (this.visualRandom.nextDouble() - 0.5) * cellW * 0.7;
        double target = WINNER_INDEX * cellW + cellW / 2.0 - REEL_W / 2.0 + jitter;
        return new Reel(strip, tiers, target, start, duration);
    }

    private static CaseDefinition.CaseTier tierOf(CaseDefinition def, ItemStack stack) {
        CaseDefinition.CaseTier best = null;
        for (CaseDefinition.CaseEntry e : def.pool()) {
            if (e.item() != stack.getItem()) {
                continue;
            }
            if (e.count() == stack.getCount()) {
                return e.tier();
            }
            if (best == null || e.tier().ordinal() > best.ordinal()) {
                best = e.tier();
            }
        }
        return best != null ? best : CaseDefinition.CaseTier.JUNK;
    }

    private static int reelHeight(int count) {
        if (count <= 1) {
            return 44;
        }
        if (count == 2) {
            return 38;
        }
        if (count == 3) {
            return 32;
        }
        if (count == 4) {
            return 28;
        }
        return 24;
    }

    private static int cellWidth(int reelH) {
        return reelH + 6;
    }

    private void skipAnimation() {
        long now = System.currentTimeMillis();
        for (Reel reel : this.reels) {
            if (!reel.done) {
                reel.startMs = now - reel.durationMs;
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.tickReels();
    }

    private void tickReels() {
        if (this.reels.isEmpty() || this.minecraft == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean anyCrossed = false;
        for (Reel reel : this.reels) {
            if (reel.done) {
                continue;
            }
            double offset = reel.offsetAt(now);
            int cellW = cellWidth(reelHeight(this.reels.size()));
            int cell = (int) ((offset + REEL_W / 2.0) / cellW);
            if (cell != reel.lastCell) {
                reel.lastCell = cell;
                anyCrossed = true;
            }
            if (reel.progress(now) >= 1f) {
                reel.done = true;
                this.playStopSound(reel.tiers.get(WINNER_INDEX));
            }
        }
        if (anyCrossed && now - this.lastTickSoundMs > 55L) {
            this.lastTickSoundMs = now;
            this.play(SoundEvents.NOTE_BLOCK_HAT, 1.8f, 0.25f);
        }
        if (!this.spinning()) {
            this.updateWidgetStates();
        }
    }

    private void playStopSound(CaseDefinition.CaseTier tier) {
        switch (tier) {
            case LEGENDARY -> this.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.6f);
            case RARE -> this.play(SoundEvents.PLAYER_LEVELUP, 1.3f, 0.5f);
            case UNCOMMON -> this.play(SoundEvents.NOTE_BLOCK_PLING, 1.4f, 0.5f);
            default -> this.play(SoundEvents.NOTE_BLOCK_BASS, 1.0f, 0.4f);
        }
    }

    private void play(SoundEvent sound, float pitch, float volume) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        GuiGraphics graphics = GuiGraphics.of(poseStack);
        UiCursor.tickInRender();
        this.updateWidgetStates();
        this.renderBackground(poseStack);
        super.render(graphics.pose(), mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font,
            Component.translatable("gui.flashstake.market.balance", format(MarketScreen.getClientBalance())),
            this.leftPos + this.imageWidth - 52, this.topPos + 10, UiTheme.NEON_GOLD);
        this.renderDropFeed(graphics, mouseX, mouseY);
        switch (this.mode) {
            case CASES -> this.renderCaseGrid(graphics, mouseX, mouseY);
            case DETAIL -> this.renderCaseDetail(graphics, mouseX, mouseY);
            case STASH -> this.renderStash(graphics, mouseX, mouseY);
        }
        this.renderTooltip(graphics.pose(), mouseX, mouseY);
    }

    
    private int feedLeft() {
        return this.leftPos - FEED_W - FEED_GAP;
    }

    private int contentWidth() {
        return this.imageWidth + FEED_W + FEED_GAP;
    }

    private void renderDropFeed(GuiGraphics graphics, int mouseX, int mouseY) {
        int feedW = FEED_W;
        int fx = this.feedLeft();
        int fy = this.topPos;
        int fh = Math.min(this.imageHeight, 220);
        WheelRenderer.neonPanel(graphics, fx, fy, fx + feedW, fy + fh, 6, UiTheme.NEON_CYAN);
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.feed"),
            fx + feedW / 2, fy + 6, UiTheme.NEON_CYAN);
        List<DropFeedClient.Entry> entries = DropFeedClient.snapshot();
        if (entries.isEmpty()) {
            graphics.drawWordWrap(this.font, Component.translatable("gui.flashstake.cases.feed_empty"),
                fx + 6, fy + 24, feedW - 12, UiTheme.TEXT_MUTED);
            return;
        }
        int y = fy + 22;
        for (DropFeedClient.Entry e : entries) {
            if (y + 28 > fy + fh - 4) {
                break;
            }
            graphics.fill(fx + 4, y, fx + feedW - 4, y + 26, e.tier().slotTint());
            graphics.renderItem(e.stack(), fx + 6, y + 5);
            String name = e.playerName();
            if (name.length() > 10) {
                name = name.substring(0, 9) + "…";
            }
            graphics.drawString(this.font, name, fx + 26, y + 4, UiTheme.TEXT, false);
            graphics.drawString(this.font, format(e.value()), fx + 26, y + 14, e.tier().stripColor(), false);
            y += 28;
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        GuiGraphics graphics = GuiGraphics.of(poseStack);        WheelRenderer.neonPanel(graphics, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 8, UiTheme.NEON_MAGENTA);
        if (this.mode != Mode.CASES) {
            return;
        }
        graphics.drawString(this.font, Component.translatable("gui.flashstake.cases.stash_hint"),
            this.leftPos + 8, this.topPos + 230, -7892829, false);
        int filled = 0;
        for (int i = 0; i < 9; ++i) {
            int x = this.leftPos + 8 + i * 18;
            int y = this.topPos + 244;
            slot(graphics, x, y);
            ItemStack stack = i < clientStash.size() ? clientStash.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 1, y + 1);
                graphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
            }
        }
        for (ItemStack s : clientStash) {
            if (!s.isEmpty()) {
                ++filled;
            }
        }
        graphics.drawString(this.font, Component.translatable("gui.flashstake.cases.stash_count", filled, CaseStash.SLOTS),
            this.leftPos + 180, this.topPos + 249, -865972, false);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        GuiGraphics graphics = GuiGraphics.of(poseStack);    }

    private void renderCaseGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        List<CaseDefinition> cases = CaseDefinition.all();
        int cols = 5;
        int pad = 8;
        int gap = 3;
        int cellH = 42;
        int rowGap = 4;
        int inner = this.imageWidth - pad * 2;
        int cellW = (inner - (cols - 1) * gap) / cols;
        int used = cols * cellW + (cols - 1) * gap;
        int startX = this.leftPos + pad + (inner - used) / 2;
        int startY = this.topPos + 30;
        for (int i = 0; i < cases.size(); ++i) {
            CaseDefinition def = cases.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cellW + gap);
            int y = startY + row * (cellH + rowGap);
            boolean over = inBox(mouseX, mouseY, x, y, cellW, cellH);
            int border = over ? -1 : def.color() | 0xFF000000;
            graphics.fill(x - 1, y - 1, x + cellW + 1, y + cellH + 1, border);
            if (!this.blitCover(graphics, def, x, y, cellW, cellH)) {
                graphics.fill(x, y, x + cellW, y + cellH, -14802128);
            }
            if (over) {
                graphics.fill(x, y, x + cellW, y + cellH, 0x30FFFFFF);
            }
            this.drawCaseLabel(graphics, Component.translatable(def.nameKey()), x + cellW / 2, y + cellH - 22, cellW - 6, -1);
            graphics.drawCenteredString(this.font, Component.literal(format(def.price())), x + cellW / 2, y + cellH - 11, -865972);
        }
    }

    /** Scale long titles (e.g. «Инструменты») so they stay inside the tile. */
    private void drawCaseLabel(GuiGraphics graphics, Component label, int cx, int y, int maxW, int color) {
        int tw = this.font.width(label);
        if (tw <= maxW) {
            graphics.drawCenteredString(this.font, label, cx, y, color);
            return;
        }
        float scale = (float) maxW / (float) tw;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, y, 0.0);
        pose.scale(scale, scale, 1.0f);
        graphics.drawCenteredString(this.font, label, 0, 0, color);
        pose.popPose();
    }

    /** Draws the case art scaled to cover the box, cropping the centre like CSS object-fit: cover. */
    private boolean blitCover(GuiGraphics graphics, CaseDefinition def, int x, int y, int w, int h) {
        return this.blitCover(graphics, def, x, y, w, h, 0.0);
    }

    /**
     * Cover-crop case art into {@code w×h}. {@code biasX} in [-1,1] shifts the crop:
     * positive values move the visible subject to the right (sample more from the left).
     */
    private boolean blitCover(GuiGraphics graphics, CaseDefinition def, int x, int y, int w, int h, double biasX) {
        if (this.minecraft == null) {
            return false;
        }
        ResourceLocation icon = def.icon();
        boolean present = ICON_CACHE.computeIfAbsent(icon, key -> this.minecraft.getResourceManager().getResource(key).isPresent());
        if (!present) {
            return false;
        }
        int texW = CaseDefinition.ICON_WIDTH;
        int texH = CaseDefinition.ICON_HEIGHT;
        double boxAspect = (double) w / h;
        double texAspect = (double) texW / texH;
        int srcW;
        int srcH;
        if (texAspect > boxAspect) {
            srcH = texH;
            srcW = (int) Math.round(texH * boxAspect);
        } else {
            srcW = texW;
            srcH = (int) Math.round(texW / boxAspect);
        }
        int maxU = Math.max(0, texW - srcW);
        int maxV = Math.max(0, texH - srcH);
        int u = (int) Math.round(maxU * (0.5 - Math.max(-1.0, Math.min(1.0, biasX)) * 0.5));
        u = Math.max(0, Math.min(maxU, u));
        int v = maxV / 2;
        graphics.blit(icon, x, y, w, h, u, v, srcW, srcH, texW, texH);
        return true;
    }

    private void renderCaseDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        CaseDefinition def = this.selected;
        if (def == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        int bx1 = this.leftPos + 10;
        int by1 = this.topPos + 30;
        int bx2 = this.leftPos + this.imageWidth - 10;
        int by2 = this.topPos + 80;
        graphics.fill(bx1 - 1, by1 - 1, bx2 + 1, by2 + 1, def.color() | 0xFF000000);
        // Bias crop right so the case art subject sits nearer the banner centre
        if (!this.blitCover(graphics, def, bx1, by1, bx2 - bx1, by2 - by1, 0.18)) {
            graphics.fill(bx1, by1, bx2, by2, -14802128);
        }
        graphics.fillGradient(bx1, by1, bx2, by2, 0x40000000, 0xC0000000);
        graphics.drawString(this.font, Component.translatable(def.nameKey()), bx1 + 6, by1 + 6, -1, true);
        graphics.drawString(this.font, Component.translatable("gui.flashstake.cases.price", format(def.price())),
            bx1 + 6, by1 + 20, -865972, true);
        double ev = CaseLoot.expectedValue(this.minecraft.level, def);
        graphics.drawString(this.font, Component.translatable("gui.flashstake.cases.ev", format(Math.round(ev))),
            bx1 + 6, by1 + 32, -4144960, true);
        if (this.openCount > 1) {
            Component total = Component.translatable("gui.flashstake.cases.total", format(def.price() * this.openCount));
            graphics.drawString(this.font, total, bx2 - 6 - this.font.width(total), by1 + 20, -865972, true);
        }

        if (this.waitingForServer) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.opening"),
                this.leftPos + this.imageWidth / 2, this.topPos + REEL_AREA_Y + REEL_AREA_H / 2 - 4, -865972);
            return;
        }
        if (!this.reels.isEmpty()) {
            this.renderReels(graphics, mouseX, mouseY);
            return;
        }
        this.renderPreview(graphics, def, mouseX, mouseY);
    }

    private void renderReels(GuiGraphics graphics, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        int n = this.reels.size();
        int reelH = reelHeight(n);
        int cellW = cellWidth(reelH);
        int gap = n >= 4 ? 4 : 6;
        int totalH = n * reelH + (n - 1) * gap;
        int startY = this.topPos + REEL_AREA_Y + Math.max(0, (REEL_AREA_H - totalH) / 2);
        int rx1 = this.leftPos + REEL_X;
        int rx2 = rx1 + REEL_W;
        int centerX = rx1 + REEL_W / 2;
        ItemStack hovered = null;

        for (int r = 0; r < n; ++r) {
            Reel reel = this.reels.get(r);
            int ry1 = startY + r * (reelH + gap);
            int ry2 = ry1 + reelH;
            graphics.fill(rx1 - 1, ry1 - 1, rx2 + 1, ry2 + 1, -12761506);
            graphics.fill(rx1, ry1, rx2, ry2, -16316665);

            double offset = reel.offsetAt(now);
            int firstCell = Math.max(0, (int) (offset / cellW) - 1);
            int lastCell = Math.min(STRIP_LENGTH - 1, (int) ((offset + REEL_W) / cellW) + 1);
            graphics.enableScissor(rx1, ry1, rx2, ry2);
            for (int k = firstCell; k <= lastCell; ++k) {
                int cx = rx1 + (int) Math.round(k * cellW - offset);
                CaseDefinition.CaseTier tier = reel.tiers.get(k);
                boolean winner = reel.done && k == WINNER_INDEX;
                graphics.fill(cx + 1, ry1 + 1, cx + cellW - 1, ry2 - 1, winner ? 0xFF2A2A34 : 0xFF1C1C22);
                graphics.fill(cx + 1, ry1 + 1, cx + cellW - 1, ry2 - 1, tier.slotTint());
                graphics.fill(cx + 1, ry2 - 3, cx + cellW - 1, ry2 - 1, tier.stripColor());
                this.renderScaledItem(graphics, reel.strip.get(k), cx + cellW / 2, ry1 + (reelH - 2) / 2, reelH >= 30 ? 1.5f : 1.1f);
                if (winner) {
                    this.outline(graphics, cx + 1, ry1 + 1, cx + cellW - 1, ry2 - 1, pulse(tier.stripColor(), now));
                }
                if (reel.done && inBox(mouseX, mouseY, Math.max(cx, rx1), ry1, Math.min(cellW, rx2 - cx), reelH)) {
                    hovered = reel.strip.get(k);
                }
            }
            for (int s = 0; s < 10; ++s) {
                int alpha = (int) (200 * (1.0 - s / 10.0));
                int color = alpha << 24 | 0x0B0B10;
                graphics.fill(rx1 + s * 3, ry1, rx1 + s * 3 + 3, ry2, color);
                graphics.fill(rx2 - s * 3 - 3, ry1, rx2 - s * 3, ry2, color);
            }
            graphics.disableScissor();

            int marker = reel.done ? pulse(reel.tiers.get(WINNER_INDEX).stripColor(), now) : 0xFFE4AE39;
            graphics.fill(centerX - 1, ry1 - 2, centerX + 1, ry2 + 2, marker);
            graphics.fill(centerX - 3, ry1 - 3, centerX + 3, ry1 - 1, marker);
            graphics.fill(centerX - 3, ry2 + 1, centerX + 3, ry2 + 3, marker);
        }

        int hintY = this.topPos + FOOTER_Y - 28;
        if (this.spinning()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.skip_hint"),
                this.leftPos + this.imageWidth / 2, hintY, -7892829);
        } else if (this.hasPendingDecision()) {
            graphics.drawCenteredString(this.font,
                Component.translatable("gui.flashstake.cases.drop_value", format(this.pendingValue())),
                this.leftPos + this.imageWidth / 2, hintY, UiTheme.NEON_GOLD);
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.close_hint"),
                this.leftPos + this.imageWidth / 2, hintY, -7892829);
        }

        if (hovered != null && this.minecraft != null && this.minecraft.level != null) {
            long value = Math.round(ItemValues.stackValue(this.minecraft.level, hovered));
            graphics.renderComponentTooltip(this.font, List.of(
                hovered.getHoverName(),
                Component.translatable("gui.flashstake.value", format(value)).withStyle(ChatFormatting.GOLD)
            ), mouseX, mouseY);
        }
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int centerX, int centerY, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(stack, -8, -8);
        graphics.renderItemDecorations(this.font, stack, -8, -8);
        graphics.pose().popPose();
    }

    private void outline(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    private static int pulse(int color, long now) {
        float t = (float) (0.65 + 0.35 * Math.sin(now / 140.0));
        int r = (int) (((color >> 16) & 0xFF) * t);
        int g = (int) (((color >> 8) & 0xFF) * t);
        int b = (int) ((color & 0xFF) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private void renderPreview(GuiGraphics graphics, CaseDefinition def, int mouseX, int mouseY) {
        graphics.drawString(this.font, Component.translatable("gui.flashstake.cases.preview"),
            this.leftPos + 10, this.topPos + 112, -7892829, false);
        ArrayList<CaseDefinition.CaseEntry> sorted = new ArrayList<>(def.pool());
        sorted.sort((a, b) -> Integer.compare(b.tier().ordinal(), a.tier().ordinal()));
        int n = Math.min(sorted.size(), 36);
        if (n <= 0) {
            return;
        }
        int pad = 10;
        int gap = 3;
        int inner = this.imageWidth - pad * 2;
        // Fixed cell size: old "minimize empty" picker chose cols=1 for prime pool sizes
        // (e.g. farm=19) and stretched one slot across the whole panel.
        final int cell = 22;
        int cols = Math.max(1, Math.min(n, (inner + gap) / (cell + gap)));
        int used = cols * cell + (cols - 1) * gap;
        int startX = this.leftPos + pad + Math.max(0, (inner - used) / 2);
        int startY = this.topPos + 120;
        CaseDefinition.CaseEntry hoveredPreview = null;
        ItemStack hoveredPreviewStack = null;
        for (int i = 0; i < n; ++i) {
            CaseDefinition.CaseEntry entry = sorted.get(i);
            int x = startX + (i % cols) * (cell + gap);
            int y = startY + (i / cols) * (cell + 4);
            ItemStack stack = new ItemStack(entry.item(), entry.count());
            boolean over = inBox(mouseX, mouseY, x, y, cell, cell);
            graphics.fill(x, y, x + cell, y + cell, over ? -12827296 : -16052974);
            graphics.fill(x, y, x + cell, y + cell, entry.tier().slotTint());
            graphics.fill(x, y + cell - 3, x + cell, y + cell, entry.tier().stripColor());
            int ix = x + (cell - 16) / 2;
            int iy = y + (cell - 16) / 2 - 1;
            graphics.renderItem(stack, ix, iy);
            graphics.renderItemDecorations(this.font, stack, ix, iy);
            if (over) {
                hoveredPreview = entry;
                hoveredPreviewStack = stack;
            }
        }

        if (hoveredPreview != null && hoveredPreviewStack != null
                && this.minecraft != null && this.minecraft.level != null) {
            double chance = CaseLoot.chancePercent(this.minecraft.level, def, hoveredPreview);
            long value = Math.round(ItemValues.stackValue(this.minecraft.level, hoveredPreviewStack));
            graphics.renderComponentTooltip(this.font, List.of(
                hoveredPreviewStack.getHoverName(),
                Component.translatable("gui.flashstake.cases.tier." + hoveredPreview.tier().name().toLowerCase(Locale.ROOT))
                    .withStyle(tierStyle(hoveredPreview.tier())),
                Component.translatable("gui.flashstake.value", format(value)).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.flashstake.cases.drop_chance", String.format(Locale.ROOT, "%.2f%%", chance))
                    .withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    private static ChatFormatting tierStyle(CaseDefinition.CaseTier tier) {
        return switch (tier) {
            case LEGENDARY -> ChatFormatting.GOLD;
            case RARE -> ChatFormatting.LIGHT_PURPLE;
            case UNCOMMON -> ChatFormatting.DARK_PURPLE;
            case COMMON -> ChatFormatting.BLUE;
            case JUNK -> ChatFormatting.GRAY;
        };
    }

    private void renderStash(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.stash_title"),
            this.leftPos + this.imageWidth / 2, this.topPos + 52, -1643790);
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.stash_help"),
            this.leftPos + this.imageWidth / 2, this.topPos + 64, -7892829);
        long total = 0L;
        ItemStack hovered = null;
        int cols = 9;
        int cell = 18;
        int gridW = cols * cell;
        int startX = this.leftPos + (this.imageWidth - gridW) / 2;
        int startY = this.topPos + 82;
        int rows = (CaseStash.SLOTS + cols - 1) / cols;
        for (int i = 0; i < CaseStash.SLOTS; ++i) {
            int x = startX + (i % cols) * cell;
            int y = startY + (i / cols) * cell;
            slot(graphics, x, y);
            ItemStack stack = i < clientStash.size() ? clientStash.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                continue;
            }
            graphics.renderItem(stack, x + 1, y + 1);
            this.drawStackCount(graphics, stack, x + 1, y + 1);
            if (this.minecraft != null && this.minecraft.level != null) {
                total += Math.round(ItemValues.stackValue(this.minecraft.level, stack));
            }
            if (inBox(mouseX, mouseY, x, y, cell, cell)) {
                hovered = stack;
            }
        }
        int valueY = startY + rows * cell + 6;
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.cases.stash_value", format(total)),
            this.leftPos + this.imageWidth / 2, valueY, -865972);
        graphics.drawCenteredString(this.font,
            Component.translatable("gui.flashstake.cases.stash_count",
                this.filledStashSlots(), CaseStash.SLOTS),
            this.leftPos + this.imageWidth / 2, valueY + 12, -7892829);
        if (hovered != null && this.minecraft != null && this.minecraft.level != null) {
            long value = Math.round(ItemValues.stackValue(this.minecraft.level, hovered));
            graphics.renderComponentTooltip(this.font, List.of(
                hovered.getHoverName(),
                Component.literal("x" + hovered.getCount()).withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.flashstake.value", format(value)).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.flashstake.cases.lmb_withdraw").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.flashstake.cases.rmb_sell").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    private int filledStashSlots() {
        int n = 0;
        for (ItemStack stack : clientStash) {
            if (!stack.isEmpty()) {
                ++n;
            }
        }
        return n;
    }

    /** Show counts above vanilla max (up to 999). */
    private void drawStackCount(GuiGraphics graphics, ItemStack stack, int x, int y) {
        int count = stack.getCount();
        if (count <= 1) {
            return;
        }
        String label = Integer.toString(count);
        float scale = count >= 100 ? 0.7f : 0.8f;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.pose().scale(scale, scale, 1.0f);
        int tx = Math.round((x + 17) / scale) - this.font.width(label);
        int ty = Math.round((y + 9) / scale);
        graphics.drawString(this.font, label, tx + 1, ty + 1, 0xFF000000, false);
        graphics.drawString(this.font, label, tx, ty, 0xFFFFFFFF, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (this.mode == Mode.CASES) {
            if (inBox(mx, my, this.leftPos + 8, this.topPos + 244, 162, 18)) {
                this.setMode(Mode.STASH);
                this.play(SoundEvents.UI_BUTTON_CLICK, 1.0f, 0.35f);
                return true;
            }
            List<CaseDefinition> cases = CaseDefinition.all();
            int cols = 5;
            int pad = 8;
            int gap = 3;
            int cellH = 42;
            int rowGap = 4;
            int inner = this.imageWidth - pad * 2;
            int cellW = (inner - (cols - 1) * gap) / cols;
            int used = cols * cellW + (cols - 1) * gap;
            int startX = this.leftPos + pad + (inner - used) / 2;
            int startY = this.topPos + 30;
            for (int i = 0; i < cases.size(); ++i) {
                int x = startX + (i % cols) * (cellW + gap);
                int y = startY + (i / cols) * (cellH + rowGap);
                if (!inBox(mx, my, x, y, cellW, cellH)) {
                    continue;
                }
                this.selected = cases.get(i);
                this.openCount = 1;
                this.reels.clear();
                this.mode = Mode.DETAIL;
                this.updateWidgetStates();
                this.play(SoundEvents.UI_BUTTON_CLICK, 1.0f, 0.35f);
                return true;
            }
        } else if (this.mode == Mode.DETAIL && !this.reels.isEmpty()
            && inBox(mx, my, this.leftPos + REEL_X, this.topPos + REEL_AREA_Y, REEL_W, REEL_AREA_H)) {
            if (this.spinning()) {
                this.skipAnimation();
                return true;
            }
            // After spin: Keep/Sell buttons handle the drop — don't clear reels on click
            return true;
        } else if (this.mode == Mode.STASH) {
            int cols = 9;
            int cell = 18;
            int gridW = cols * cell;
            int startX = this.leftPos + (this.imageWidth - gridW) / 2;
            int startY = this.topPos + 82;
            for (int i = 0; i < CaseStash.SLOTS; ++i) {
                int x = startX + (i % cols) * cell;
                int y = startY + (i / cols) * cell;
                if (!inBox(mx, my, x, y, cell, cell)) {
                    continue;
                }
                ItemStack stack = i < clientStash.size() ? clientStash.get(i) : ItemStack.EMPTY;
                if (!stack.isEmpty() && (button == 0 || button == 1)) {
                    ServerboundCaseStashActionPacket.Action action = button == 0
                        ? ServerboundCaseStashActionPacket.Action.WITHDRAW_ONE
                        : ServerboundCaseStashActionPacket.Action.SELL_ONE;
                    ModNetwork.sendToServer(new ServerboundCaseStashActionPacket(action, i));
                    this.play(SoundEvents.UI_BUTTON_CLICK, 1.0f, 0.4f);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.spinning()) {
                this.skipAnimation();
                return true;
            }
            if (this.waitingForServer) {
                return true;
            }
            if (this.mode == Mode.DETAIL && !this.reels.isEmpty()) {
                if (this.hasPendingDecision()) {
                    ModNetwork.sendToServer(new ServerboundCasePendingActionPacket(ServerboundCasePendingActionPacket.Action.KEEP));
                    return true;
                }
                this.reels.clear();
                this.updateWidgetStates();
                return true;
            }
            if (this.mode != Mode.CASES) {
                this.setMode(Mode.CASES);
                return true;
            }
            this.onClose();
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
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

    private static String format(long value) {
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fK", value / 1000.0);
        }
        return Long.toString(value);
    }

    private static final class Reel {
        final List<ItemStack> strip;
        final List<CaseDefinition.CaseTier> tiers;
        final double targetOffset;
        long startMs;
        final long durationMs;
        boolean done;
        int lastCell = -1;

        Reel(List<ItemStack> strip, List<CaseDefinition.CaseTier> tiers, double targetOffset, long startMs, long durationMs) {
            this.strip = strip;
            this.tiers = tiers;
            this.targetOffset = targetOffset;
            this.startMs = startMs;
            this.durationMs = durationMs;
        }

        float progress(long now) {
            return Mth.clamp((float) (now - this.startMs) / (float) this.durationMs, 0.0f, 1.0f);
        }

        double offsetAt(long now) {
            double t = this.progress(now);
            double eased = 1.0 - Math.pow(1.0 - t, 4.0);
            return this.targetOffset * eased;
        }
    }
}
