package net.execheinz.upgrader.client;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;
import java.util.Locale;
import net.execheinz.upgrader.doublegame.DoubleColor;
import net.execheinz.upgrader.doublegame.DoubleGame;
import net.execheinz.upgrader.menu.DoubleMenu;
import net.execheinz.upgrader.network.ClientboundDoubleStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.ServerboundDoubleBetPacket;
import net.execheinz.upgrader.network.ServerboundOpenUpgraderPacket;
import net.execheinz.upgrader.client.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DoubleScreen extends AbstractContainerScreen<DoubleMenu> {
    private static final int REEL_LEN = 56;
    private static final int CELL_W = 32;
    private static final int REEL_X = 16;
    private static final int REEL_Y = 78;
    private static final int REEL_H = 48;
    private static final int REEL_W = 348;

    private static long clientRoundId;
    private static int clientPhase;
    private static int clientTicksLeft;
    private static int clientResult;
    private static List<ClientboundDoubleStatePacket.BetView> clientBets = List.of();
    private static List<Integer> clientHistory = List.of();

    private DoubleColor selected = DoubleColor.WHITE;
    private EditBox betBox;
    private StyledButton backButton;
    private StyledButton betButton;
    private final StyledButton[] colorButtons = new StyledButton[4];
    private final StyledButton[] quickBets = new StyledButton[4];

    private long spinStartMs;
    private long spinDurationMs = 4000L;
    private double spinTarget;
    private long lastRoundSeen;
    private boolean spinningVisual;

    public DoubleScreen(DoubleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 380;
        this.imageHeight = 300;
        this.inventoryLabelY = 10000;
    }

    public static void applyState(ClientboundDoubleStatePacket packet) {
        clientRoundId = packet.roundId();
        clientPhase = packet.phaseOrdinal();
        clientTicksLeft = packet.ticksLeft();
        clientResult = packet.resultOrdinal();
        clientBets = List.copyOf(packet.bets());
        clientHistory = List.copyOf(packet.history());
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        this.backButton = this.addRenderableWidget(StyledButton.chip(x + 8, y + 6, 50, 16,
            Component.translatable("gui.flashstake.market.back"), -1643790,
            b -> {
                UiCursor.captureIfInFlashStakeUi();
                ModNetwork.sendToServer(new ServerboundOpenUpgraderPacket());
            }));
        UiCursor.scheduleRestore();

        DoubleColor[] colors = DoubleColor.values();
        for (int i = 0; i < 4; ++i) {
            DoubleColor color = colors[i];
            this.colorButtons[i] = this.addRenderableWidget(StyledButton.chip(
                x + 16 + i * 88, y + 148, 82, 24,
                Component.translatable("gui.flashstake.double.color." + color.name().toLowerCase(Locale.ROOT), color.multiplier()),
                color.argb(),
                b -> this.selected = color
            ));
        }

        this.betBox = new EditBox(this.font, x + 16, y + 182, 90, 18, Component.literal("bet"));
        this.betBox.setMaxLength(8);
        this.betBox.setValue("100");
        this.betBox.setTextColor(UiTheme.TEXT);
        this.addRenderableWidget(this.betBox);

        long[] quick = {50L, 100L, 500L, 1000L};
        for (int i = 0; i < 4; ++i) {
            long amt = quick[i];
            this.quickBets[i] = this.addRenderableWidget(StyledButton.chip(
                x + 114 + i * 48, y + 182, 44, 18,
                Component.literal(Long.toString(amt)), UiTheme.NEON_CYAN,
                b -> this.betBox.setValue(Long.toString(amt))
            ));
        }

        this.betButton = this.addRenderableWidget(StyledButton.gold(x + 16, y + 208, 348, 22,
            Component.translatable("gui.flashstake.double.bet"), b -> this.sendBet()));

        this.lastRoundSeen = clientRoundId;
        if (clientPhase == DoubleGame.Phase.SPINNING.ordinal()) {
            this.startSpinVisual();
        }
        this.refreshBetControls();
    }

    private boolean hasOwnBet() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        String name = this.minecraft.player.getGameProfile().getName();
        for (ClientboundDoubleStatePacket.BetView bet : clientBets) {
            if (name.equalsIgnoreCase(bet.playerName())) {
                return true;
            }
        }
        return false;
    }

    private void sendBet() {
        if (this.hasOwnBet() || clientPhase != DoubleGame.Phase.BETTING.ordinal()) {
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(this.betBox.getValue().trim());
        } catch (NumberFormatException e) {
            return;
        }
        FlashFx.confirm();
        ModNetwork.sendToServer(new ServerboundDoubleBetPacket(this.selected.ordinal(), amount));
    }

    private void refreshBetControls() {
        boolean betting = clientPhase == DoubleGame.Phase.BETTING.ordinal();
        boolean own = this.hasOwnBet();
        boolean canBet = betting && !own;
        this.betButton.active = canBet;
        this.betButton.setMessage(Component.translatable(
            own ? "gui.flashstake.double.bet_placed" : "gui.flashstake.double.bet"));
        this.betBox.setEditable(canBet);
        for (StyledButton btn : this.colorButtons) {
            btn.active = canBet;
        }
        for (StyledButton btn : this.quickBets) {
            btn.active = canBet;
        }
    }

    private void startSpinVisual() {
        this.spinningVisual = true;
        this.spinStartMs = System.currentTimeMillis();
        this.spinDurationMs = Math.max(800L, clientTicksLeft * 50L);
        DoubleColor result = DoubleColor.byOrdinalSafe(clientResult);
        int winnerIndex = 40 + result.ordinal();
        double jitter = (Math.random() - 0.5) * CELL_W * 0.4;
        this.spinTarget = winnerIndex * CELL_W + CELL_W / 2.0 - REEL_W / 2.0 + jitter;
        FlashFx.whoosh();
        UiMotion.pulsePanel();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (clientRoundId != this.lastRoundSeen) {
            this.lastRoundSeen = clientRoundId;
            this.spinningVisual = false;
        }
        if (clientPhase == DoubleGame.Phase.SPINNING.ordinal() && !this.spinningVisual) {
            this.startSpinVisual();
        }
        if (this.spinningVisual) {
            long elapsed = System.currentTimeMillis() - this.spinStartMs;
            if (elapsed < this.spinDurationMs) {
                float tt = elapsed / (float) this.spinDurationMs;
                FlashFx.tick(1.2f + tt * 0.8f);
            } else {
                this.spinningVisual = false;
                FlashFx.win();
            }
        }
        this.refreshBetControls();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        UiCursor.tickInRender();
        GuiGraphics graphics = GuiGraphics.of(poseStack);        this.renderBackground(poseStack);
        super.render(graphics.pose(), mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font,
            Component.translatable("gui.flashstake.market.balance", format(MarketScreen.getClientBalance())),
            this.leftPos + this.imageWidth - 56, this.topPos + 10, UiTheme.NEON_GOLD);
        this.renderTooltip(graphics.pose(), mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        GuiGraphics graphics = GuiGraphics.of(poseStack);        int x = this.leftPos;
        int y = this.topPos;
        WheelRenderer.neonPanel(graphics, x, y, x + this.imageWidth, y + this.imageHeight, 8, UiTheme.NEON_GOLD);
        graphics.drawCenteredString(this.font, Component.translatable("gui.flashstake.double.title"),
            x + this.imageWidth / 2, y + 8, UiTheme.NEON_GOLD);

        this.renderHistory(graphics, x + 16, y + 26);

        String phaseKey = switch (DoubleGame.Phase.values()[Math.min(clientPhase, 2)]) {
            case BETTING -> "gui.flashstake.double.phase_bet";
            case SPINNING -> "gui.flashstake.double.phase_spin";
            case RESULT -> "gui.flashstake.double.phase_result";
        };
        float sec = clientTicksLeft / 20f;
        graphics.drawCenteredString(this.font,
            Component.translatable(phaseKey, String.format(Locale.ROOT, "%.1f", sec)),
            x + this.imageWidth / 2, y + 62, UiTheme.TEXT);

        this.renderReel(graphics, x + REEL_X, y + REEL_Y);

        DoubleColor last = DoubleColor.byOrdinalSafe(clientResult);
        if (clientPhase == DoubleGame.Phase.RESULT.ordinal()) {
            graphics.drawString(this.font,
                Component.translatable("gui.flashstake.double.last",
                    Component.translatable("gui.flashstake.double.color." + last.name().toLowerCase(Locale.ROOT), last.multiplier())),
                x + 16, y + 132, last.argb(), false);
        } else if (clientPhase == DoubleGame.Phase.SPINNING.ordinal()) {
            graphics.drawString(this.font, Component.translatable("gui.flashstake.double.spinning"),
                x + 16, y + 132, UiTheme.TEXT_DIM, false);
        } else {
            graphics.drawString(this.font,
                Component.translatable("gui.flashstake.double.prev",
                    Component.translatable("gui.flashstake.double.color." + last.name().toLowerCase(Locale.ROOT), last.multiplier())),
                x + 16, y + 132, UiTheme.TEXT_MUTED, false);
        }

        graphics.drawString(this.font, Component.translatable("gui.flashstake.double.players"),
            x + 16, y + 238, UiTheme.TEXT_DIM, false);
        int row = 0;
        for (ClientboundDoubleStatePacket.BetView bet : clientBets) {
            if (row >= 4) {
                break;
            }
            DoubleColor c = DoubleColor.byOrdinalSafe(bet.colorOrdinal());
            String line = bet.playerName() + " · " + format(bet.amount()) + " → ×" + c.multiplier();
            int col = row / 2;
            int local = row % 2;
            graphics.drawString(this.font, line, x + 16 + col * 180, y + 252 + local * 12, c.argb(), false);
            row++;
        }
        if (clientBets.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.flashstake.double.no_bets"),
                x + 16, y + 252, UiTheme.TEXT_MUTED, false);
        }

        for (int i = 0; i < 4; ++i) {
            if (DoubleColor.values()[i] == this.selected) {
                StyledButton btn = this.colorButtons[i];
                graphics.fill(btn.x - 1, btn.y - 1, btn.x + btn.getWidth() + 1, btn.y + btn.getHeight() + 1, 0x88FFFFFF);
            }
        }
    }

    /** Newest on the left — colour chips for the last N rounds. */
    private void renderHistory(GuiGraphics graphics, int hx, int hy) {
        graphics.drawString(this.font, Component.translatable("gui.flashstake.double.history"),
            hx, hy, UiTheme.TEXT_DIM, false);
        int chip = 12;
        int gap = 2;
        int startX = hx;
        int startY = hy + 12;
        if (clientHistory.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.flashstake.double.history_empty"),
                startX, startY + 2, UiTheme.TEXT_MUTED, false);
            return;
        }
        int max = Math.min(clientHistory.size(), DoubleGame.HISTORY_SIZE);
        for (int i = 0; i < max; ++i) {
            DoubleColor c = DoubleColor.byOrdinalSafe(clientHistory.get(i));
            int cx = startX + i * (chip + gap);
            if (cx + chip > this.leftPos + this.imageWidth - 16) {
                break;
            }
            graphics.fill(cx, startY, cx + chip, startY + chip, c.argb());
            if (i == 0 && clientPhase == DoubleGame.Phase.RESULT.ordinal()) {
                graphics.fill(cx - 1, startY - 1, cx + chip + 1, startY, UiTheme.NEON_CYAN);
                graphics.fill(cx - 1, startY + chip, cx + chip + 1, startY + chip + 1, UiTheme.NEON_CYAN);
                graphics.fill(cx - 1, startY, cx, startY + chip, UiTheme.NEON_CYAN);
                graphics.fill(cx + chip, startY, cx + chip + 1, startY + chip, UiTheme.NEON_CYAN);
            }
            String m = Integer.toString(c.multiplier());
            graphics.drawCenteredString(this.font, m, cx + chip / 2, startY + 2, 0xFF101010);
        }
    }

    private void renderReel(GuiGraphics graphics, int rx, int ry) {
        graphics.fill(rx - 2, ry - 2, rx + REEL_W + 2, ry + REEL_H + 2, 0xFF0A0E18);
        double offset;
        long now = System.currentTimeMillis();
        if (this.spinningVisual) {
            float t = Mth.clamp((now - this.spinStartMs) / (float) this.spinDurationMs, 0f, 1f);
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);
            offset = this.spinTarget * eased;
        } else if (clientPhase == DoubleGame.Phase.BETTING.ordinal()) {
            offset = (now / 18.0) % (CELL_W * 4);
        } else {
            DoubleColor result = DoubleColor.byOrdinalSafe(clientResult);
            offset = (40 + result.ordinal()) * CELL_W + CELL_W / 2.0 - REEL_W / 2.0;
        }

        graphics.enableScissor(rx, ry, rx + REEL_W, ry + REEL_H);
        for (int i = 0; i < REEL_LEN; ++i) {
            DoubleColor color = DoubleColor.values()[i % 4];
            int cx = rx + (int) (i * CELL_W - offset);
            if (cx + CELL_W < rx || cx > rx + REEL_W) {
                continue;
            }
            graphics.fill(cx + 1, ry + 3, cx + CELL_W - 1, ry + REEL_H - 3, color.argb());
            graphics.drawCenteredString(this.font, "×" + color.multiplier(), cx + CELL_W / 2, ry + 17, 0xFF101010);
        }
        graphics.disableScissor();

        int mid = rx + REEL_W / 2;
        graphics.fill(mid - 1, ry - 5, mid + 1, ry + REEL_H + 5, UiTheme.NEON_CYAN);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        GuiGraphics graphics = GuiGraphics.of(poseStack);    }

    private static String format(long value) {
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fK", value / 1000.0);
        }
        return Long.toString(value);
    }
}
