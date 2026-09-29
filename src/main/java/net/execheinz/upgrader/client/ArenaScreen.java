package net.execheinz.upgrader.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.execheinz.upgrader.battle.BattleManager;
import net.execheinz.upgrader.cases.CaseDefinition;
import net.execheinz.upgrader.cases.CaseLoot;
import net.execheinz.upgrader.contract.ContractService;
import net.execheinz.upgrader.duel.DuelManager;
import net.execheinz.upgrader.menu.ArenaMenu;
import net.execheinz.upgrader.network.ClientboundArenaLobbyPacket;
import net.execheinz.upgrader.network.ClientboundBattleStatePacket;
import net.execheinz.upgrader.network.ClientboundContractResultPacket;
import net.execheinz.upgrader.network.ClientboundDuelStatePacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.network.ServerboundArenaActionPacket;
import net.execheinz.upgrader.network.ServerboundOpenUpgraderPacket;
import net.execheinz.upgrader.value.ItemValues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * PvP Arena: Battle cases, Contracts, Duels — player vs player only (no bots).
 */
public class ArenaScreen extends AbstractContainerScreen<ArenaMenu> {
    private enum Tab { BATTLE, CONTRACT, DUEL }

    private static final int PANEL_PAD = 12;
    private static final int LEFT_W = 168;
    private static final int RIGHT_X = 188;
    private static final int RIGHT_W = 156;
    private static final int INVITE_ROW_H = 20;
    private static final int INVITE_MAX = 7;

    private static List<ClientboundArenaLobbyPacket.PlayerView> lobbyPlayers = List.of();
    private static List<ClientboundArenaLobbyPacket.BattleInvite> battleInvites = List.of();
    private static List<ClientboundArenaLobbyPacket.DuelInvite> duelInvites = List.of();
    private static ClientboundBattleStatePacket battle = ClientboundBattleStatePacket.idle();
    private static ClientboundDuelStatePacket duel = ClientboundDuelStatePacket.idle();
    private static ClientboundContractResultPacket lastContract;

    private Tab tab = Tab.BATTLE;
    private int caseIndex;
    private final Set<Integer> contractSlots = new HashSet<>();
    private EditBox stakeBox;

    private StyledButton backButton;
    private StyledButton battleTab;
    private StyledButton contractTab;
    private StyledButton duelTab;
    private StyledButton primaryButton;
    private StyledButton cancelButton;
    private StyledButton casePrev;
    private StyledButton caseNext;

    private long revealPulseMs;
    private int lastBattlePhase = -1;
    private int lastDuelPhase = -1;

    private static final int BATTLE_CARD_W = 160;
    private static final int BATTLE_REEL_H = 44;
    private static final int BATTLE_CELL_W = 50;
    private static final int BATTLE_STRIP = 56;
    private static final int BATTLE_WINNER_INDEX = 46;
    private static final long BATTLE_REEL_MS = 5200L;
    private static final long BATTLE_REEL_STAGGER_MS = 300L;
    private final Random reelRandom = new Random();
    private BattleReel hostReel;
    private BattleReel guestReel;
    private String battleReelKey;
    private ItemStack pendingTooltip = ItemStack.EMPTY;

    private static final int DUEL_SEGMENTS = 12;
    private static final int DUEL_TURNS = 5;
    private static final long DUEL_SPIN_MS = DuelManager.SPIN_TICKS * 50L;
    private long duelSpinStartMs;
    private float duelTargetDeg;
    private int duelLastSeg = -1;

    public ArenaScreen(ArenaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 360;
        this.imageHeight = 300;
        this.inventoryLabelY = 10000;
    }

    public static void applyLobby(ClientboundArenaLobbyPacket packet) {
        lobbyPlayers = List.copyOf(packet.players());
        battleInvites = List.copyOf(packet.battles());
        duelInvites = List.copyOf(packet.duels());
    }

    public static void applyBattle(ClientboundBattleStatePacket packet) {
        battle = packet;
    }

    public static void applyDuel(ClientboundDuelStatePacket packet) {
        duel = packet;
    }

    public static void applyContract(ClientboundContractResultPacket packet) {
        lastContract = packet;
        // Apply only the touched slots so the Arena grid updates without a full inv dump.
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !packet.ok()) {
            return;
        }
        int[] slots = packet.changedSlots();
        ItemStack[] stacks = packet.changedStacks();
        if (slots == null || stacks == null || slots.length == 0) {
            return;
        }
        Inventory inv = mc.player.getInventory();
        int n = Math.min(slots.length, stacks.length);
        for (int i = 0; i < n; ++i) {
            int idx = slots[i];
            if (idx < 0 || idx >= 36) {
                continue;
            }
            ItemStack s = stacks[i];
            inv.setItem(idx, s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy());
        }
    }

    @Override
    protected void init() {
        super.init();
        UiCursor.scheduleRestore();
        int x = this.leftPos;
        int y = this.topPos;

        this.backButton = this.addRenderableWidget(StyledButton.chip(x + 8, y + 6, 50, 16,
            Component.translatable("gui.flashstake.market.back"), UiTheme.TEXT, b -> {
                FlashFx.click();
                UiCursor.captureIfInFlashStakeUi();
                ModNetwork.sendToServer(new ServerboundOpenUpgraderPacket());
            }));
        this.battleTab = this.addRenderableWidget(StyledButton.neon(x + 64, y + 6, 70, 16,
            Component.translatable("gui.flashstake.arena.tab_battle"), UiTheme.NEON_MAGENTA, b -> this.setTab(Tab.BATTLE)));
        this.contractTab = this.addRenderableWidget(StyledButton.neon(x + 138, y + 6, 80, 16,
            Component.translatable("gui.flashstake.arena.tab_contract"), UiTheme.NEON_CYAN, b -> this.setTab(Tab.CONTRACT)));
        this.duelTab = this.addRenderableWidget(StyledButton.neon(x + 222, y + 6, 70, 16,
            Component.translatable("gui.flashstake.arena.tab_duel"), UiTheme.NEON_GOLD, b -> this.setTab(Tab.DUEL)));

        // Case picker: arrows under the case art preview
        this.casePrev = this.addRenderableWidget(StyledButton.chip(x + PANEL_PAD, y + 148, 26, 18,
            Component.literal("<"), UiTheme.NEON_CYAN, b -> {
                FlashFx.softClick();
                List<CaseDefinition> all = CaseDefinition.all();
                if (!all.isEmpty()) {
                    this.caseIndex = (this.caseIndex - 1 + all.size()) % all.size();
                }
            }));
        this.caseNext = this.addRenderableWidget(StyledButton.chip(x + PANEL_PAD + LEFT_W - 26, y + 148, 26, 18,
            Component.literal(">"), UiTheme.NEON_CYAN, b -> {
                FlashFx.softClick();
                List<CaseDefinition> all = CaseDefinition.all();
                if (!all.isEmpty()) {
                    this.caseIndex = (this.caseIndex + 1) % all.size();
                }
            }));

        this.stakeBox = new EditBox(this.font, x + PANEL_PAD, y + 88, 100, 18, Component.literal("stake"));
        this.stakeBox.setMaxLength(10);
        this.stakeBox.setValue("100");
        this.stakeBox.setTextColor(UiTheme.TEXT);
        this.addRenderableWidget(this.stakeBox);

        this.primaryButton = this.addRenderableWidget(StyledButton.gold(x + PANEL_PAD, y + 260, 160, 22,
            Component.translatable("gui.flashstake.arena.create"), b -> this.onPrimary()));
        this.cancelButton = this.addRenderableWidget(StyledButton.neon(x + 184, y + 260, 160, 22,
            Component.translatable("gui.flashstake.arena.cancel"), UiTheme.NEON_MAGENTA, b -> this.onCancel()));

        ModNetwork.sendToServer(ServerboundArenaActionPacket.refresh());
        this.updateWidgets();
    }

    private void setTab(Tab next) {
        FlashFx.click();
        this.tab = next;
        this.contractSlots.clear();
        this.updateWidgets();
    }

    private void updateWidgets() {
        boolean battleBusy = battle.active() && battle.phaseOrdinal() != BattleManager.Phase.WAITING.ordinal();
        boolean duelBusy = duel.active() && duel.phaseOrdinal() != DuelManager.Phase.WAITING.ordinal();
        boolean waitingBattle = battle.active() && battle.phaseOrdinal() == BattleManager.Phase.WAITING.ordinal() && this.isSelfHost(battle.hostUuid());
        boolean waitingDuel = duel.active() && duel.phaseOrdinal() == DuelManager.Phase.WAITING.ordinal() && this.isSelfHost(duel.hostUuid());

        this.casePrev.visible = this.tab == Tab.BATTLE && !battle.active();
        this.caseNext.visible = this.tab == Tab.BATTLE && !battle.active();
        this.stakeBox.visible = this.tab == Tab.DUEL && !duel.active();

        this.primaryButton.visible = true;
        this.cancelButton.visible = (this.tab == Tab.BATTLE && waitingBattle) || (this.tab == Tab.DUEL && waitingDuel);

        if (this.tab == Tab.BATTLE) {
            this.primaryButton.setMessage(Component.translatable(
                waitingBattle ? "gui.flashstake.arena.waiting" : "gui.flashstake.arena.battle_create"));
            this.primaryButton.active = !battleBusy && !waitingBattle;
        } else if (this.tab == Tab.CONTRACT) {
            this.primaryButton.setMessage(Component.translatable("gui.flashstake.arena.contract_submit"));
            this.primaryButton.active = this.contractSlots.size() >= ContractService.MIN_ITEMS
                && this.contractSlots.size() <= ContractService.MAX_ITEMS;
        } else {
            this.primaryButton.setMessage(Component.translatable(
                waitingDuel ? "gui.flashstake.arena.waiting" : "gui.flashstake.arena.duel_create"));
            this.primaryButton.active = !duelBusy && !waitingDuel;
        }
        this.cancelButton.active = waitingBattle || waitingDuel;
        this.battleTab.active = true;
        this.contractTab.active = true;
        this.duelTab.active = true;
    }

    private boolean isSelfHost(String hostUuid) {
        if (this.minecraft == null || this.minecraft.player == null || hostUuid == null || hostUuid.isEmpty()) {
            return false;
        }
        return hostUuid.equals(this.minecraft.player.getUUID().toString());
    }

    private void onPrimary() {
        FlashFx.confirm();
        if (this.tab == Tab.BATTLE) {
            List<CaseDefinition> all = CaseDefinition.all();
            if (all.isEmpty()) {
                return;
            }
            CaseDefinition def = all.get(Mth.clamp(this.caseIndex, 0, all.size() - 1));
            ModNetwork.sendToServer(ServerboundArenaActionPacket.battleCreate(def.id()));
            UiMotion.pulsePanel();
        } else if (this.tab == Tab.CONTRACT) {
            int[] slots = this.contractSlots.stream().mapToInt(Integer::intValue).toArray();
            ModNetwork.sendToServer(ServerboundArenaActionPacket.contract(ContractService.SOURCE_PLAYER, slots));
            this.contractSlots.clear();
            UiMotion.pulsePanel();
        } else {
            long stake;
            try {
                stake = Long.parseLong(this.stakeBox.getValue().trim());
            } catch (NumberFormatException e) {
                return;
            }
            ModNetwork.sendToServer(ServerboundArenaActionPacket.duelCreate(stake));
            UiMotion.pulsePanel();
        }
        this.updateWidgets();
    }

    private void onCancel() {
        FlashFx.click();
        if (this.tab == Tab.BATTLE) {
            ModNetwork.sendToServer(ServerboundArenaActionPacket.battleCancel());
        } else if (this.tab == Tab.DUEL) {
            ModNetwork.sendToServer(ServerboundArenaActionPacket.duelCancel());
        }
        this.updateWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (battle.active() && battle.phaseOrdinal() != this.lastBattlePhase) {
            this.onBattlePhase(battle.phaseOrdinal());
            this.lastBattlePhase = battle.phaseOrdinal();
        }
        if (duel.active() && duel.phaseOrdinal() != this.lastDuelPhase) {
            this.onDuelPhase(duel.phaseOrdinal());
            this.lastDuelPhase = duel.phaseOrdinal();
        }
        if (!battle.active() || battle.phaseOrdinal() < BattleManager.Phase.OPENING.ordinal()) {
            if (!battle.active()) {
                this.lastBattlePhase = -1;
            }
            this.hostReel = null;
            this.guestReel = null;
            this.battleReelKey = null;
        }
        this.tickBattleReels();
        if (!duel.active()) {
            this.lastDuelPhase = -1;
            this.duelSpinStartMs = 0L;
            this.duelTargetDeg = 0.0f;
        }
        this.updateWidgets();
    }

    private void onBattlePhase(int phase) {
        if (phase == BattleManager.Phase.OPENING.ordinal()) {
            FlashFx.whoosh();
            this.revealPulseMs = System.currentTimeMillis();
        } else if (phase == BattleManager.Phase.REVEAL.ordinal()) {
            boolean win = this.didWinBattle();
            if (win) {
                UiMotion.celebrateWin(true);
            } else if (battle.winner() == 2) {
                FlashFx.confirm();
                UiMotion.pushFloater("TIE", UiTheme.NEON_CYAN);
            } else {
                UiMotion.celebrateLose();
            }
            this.revealPulseMs = System.currentTimeMillis();
        }
    }

    private void onDuelPhase(int phase) {
        if (phase == DuelManager.Phase.SPINNING.ordinal()) {
            FlashFx.whoosh();
            this.startDuelSpin();
            this.revealPulseMs = System.currentTimeMillis();
        } else if (phase == DuelManager.Phase.RESULT.ordinal()) {
            if (this.duelSpinStartMs == 0L && this.duelTargetDeg == 0.0f) {
                this.startDuelSpin();
            }
            if (this.didWinDuel()) {
                UiMotion.celebrateWin(false);
            } else {
                UiMotion.celebrateLose();
            }
            this.revealPulseMs = System.currentTimeMillis();
        }
    }

    private boolean didWinBattle() {
        if (!battle.active() || this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        boolean host = this.isSelfHost(battle.hostUuid());
        return (battle.winner() == 0 && host) || (battle.winner() == 1 && !host);
    }

    private boolean didWinDuel() {
        if (!duel.active() || this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        boolean host = this.isSelfHost(duel.hostUuid());
        return (duel.winner() == 0 && host) || (duel.winner() == 1 && !host);
    }

    private int battleInviteListX() {
        return this.leftPos + RIGHT_X;
    }

    private int battleInviteListY() {
        return this.topPos + 72;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.tab == Tab.BATTLE && !battle.active()) {
            int listX = this.battleInviteListX();
            int y0 = this.battleInviteListY() + 18;
            for (int i = 0; i < Math.min(battleInvites.size(), INVITE_MAX); ++i) {
                ClientboundArenaLobbyPacket.BattleInvite inv = battleInvites.get(i);
                int rowY = y0 + i * INVITE_ROW_H;
                if (inBox(mouseX, mouseY, listX, rowY, RIGHT_W, INVITE_ROW_H - 2)) {
                    if (!this.isSelfHost(inv.hostUuid())) {
                        FlashFx.confirm();
                        ModNetwork.sendToServer(ServerboundArenaActionPacket.battleAccept(inv.hostUuid()));
                        return true;
                    }
                }
            }
        }
        if (this.tab == Tab.DUEL && !duel.active()) {
            int y0 = this.topPos + 160;
            for (int i = 0; i < Math.min(duelInvites.size(), 5); ++i) {
                ClientboundArenaLobbyPacket.DuelInvite inv = duelInvites.get(i);
                int rowY = y0 + i * 18;
                if (inBox(mouseX, mouseY, this.leftPos + 16, rowY, 328, 16)) {
                    if (!this.isSelfHost(inv.hostUuid())) {
                        FlashFx.confirm();
                        ModNetwork.sendToServer(ServerboundArenaActionPacket.duelAccept(inv.hostUuid()));
                        return true;
                    }
                }
            }
        }
        if (this.tab == Tab.CONTRACT) {
            for (int i = 0; i < 36; ++i) {
                int[] xy = this.contractSlotPos(i);
                if (inBox(mouseX, mouseY, xy[0], xy[1], 17, 17)) {
                    FlashFx.softClick();
                    ItemStack stack = this.playerInvStack(i);
                    if (this.contractSlots.contains(i)) {
                        this.contractSlots.remove(i);
                    } else if (!stack.isEmpty() && this.contractSlots.size() < ContractService.MAX_ITEMS) {
                        this.contractSlots.add(i);
                    }
                    this.updateWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ItemStack playerInvStack(int slot) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        return this.minecraft.player.getInventory().getItem(slot);
    }

    /** Visual layout: main 9–35 in 3 rows, hotbar 0–8 under them. */
    private int[] contractSlotPos(int invSlot) {
        int startX = this.leftPos + 16;
        int startY = this.topPos + 88;
        int cell = 18;
        if (invSlot < 9) {
            return new int[] { startX + invSlot * cell, startY + 3 * cell + 6 };
        }
        int main = invSlot - 9;
        return new int[] { startX + (main % 9) * cell, startY + (main / 9) * cell };
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No vanilla blur / world — solid dim only (same idea as fill in render).
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // Same neon blue panel as Cases / Double family
        WheelRenderer.neonPanel(graphics, x, y, x + this.imageWidth, y + this.imageHeight, 8, UiTheme.NEON_CYAN);
        UiMotion.renderPanelPulse(graphics, x, y, x + this.imageWidth, y + this.imageHeight, UiTheme.NEON_GOLD);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiCursor.tickInRender();
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;

        // Clip all arena chrome to the panel so nothing spills past the blue frame
        graphics.enableScissor(x + 4, y + 4, x + this.imageWidth - 4, y + this.imageHeight - 4);

        StyledButton selected = switch (this.tab) {
            case BATTLE -> this.battleTab;
            case CONTRACT -> this.contractTab;
            case DUEL -> this.duelTab;
        };
        if (selected != null) {
            WheelRenderer.roundedRect(graphics,
                selected.getX() - 2, selected.getY() - 2,
                selected.getX() + selected.getWidth() + 2, selected.getY() + selected.getHeight() + 2,
                5, FlashFx.withAlpha(UiTheme.NEON_CYAN, 0.35f));
        }

        Component bal = Component.translatable("gui.flashstake.market.balance", UiMotion.smoothBalance());
        long now = System.currentTimeMillis();
        graphics.drawString(this.font, bal, x + 16, y + 28, UiTheme.TEXT, false);
        UiMotion.renderFloaters(graphics, this.font, x + 200, y + 28);

        String online = Component.translatable("gui.flashstake.arena.online", lobbyPlayers.size()).getString();
        graphics.drawString(this.font, UiText.ellipsize(this.font, online, 100),
            x + this.imageWidth - 16 - this.font.width(UiText.ellipsize(this.font, online, 100)),
            y + 28, UiTheme.TEXT_DIM, false);

        if (this.tab == Tab.BATTLE) {
            this.renderBattle(graphics, x, y, mouseX, mouseY, now);
        } else if (this.tab == Tab.CONTRACT) {
            this.renderContract(graphics, x, y, mouseX, mouseY);
        } else {
            this.renderDuel(graphics, x, y, mouseX, mouseY, now);
        }

        graphics.disableScissor();
        if (!this.pendingTooltip.isEmpty()) {
            graphics.renderTooltip(this.font, this.pendingTooltip, mouseX, mouseY);
            this.pendingTooltip = ItemStack.EMPTY;
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderBattle(GuiGraphics g, int x, int y, int mouseX, int mouseY, long now) {
        if (battle.active() && battle.phaseOrdinal() >= BattleManager.Phase.OPENING.ordinal()) {
            float pulse = FlashFx.pulse(this.revealPulseMs, 700L);
            boolean spinning = battle.phaseOrdinal() == BattleManager.Phase.OPENING.ordinal();
            CaseDefinition cdef = CaseDefinition.byId(battle.caseId());

            UiText.drawCentered(g, this.font, Component.translatable("gui.flashstake.arena.vs",
                battle.hostName(), battle.guestName().isEmpty() ? "…" : battle.guestName()),
                x + this.imageWidth / 2, y + 48, UiTheme.NEON_MAGENTA, 1.05f);

            this.ensureBattleReels(cdef, now);
            String guestName = battle.guestName().isEmpty() ? "…" : battle.guestName();
            this.drawBattleReelCard(g, x + 12, y + 66, battle.hostName(), this.hostReel,
                battle.hostValue(), !spinning && battle.winner() == 0, !spinning && battle.winner() == 1,
                pulse, now, mouseX, mouseY);
            this.drawBattleReelCard(g, x + 12 + BATTLE_CARD_W + 16, y + 66, guestName, this.guestReel,
                battle.guestValue(), !spinning && battle.winner() == 1, !spinning && battle.winner() == 0,
                pulse, now, mouseX, mouseY);

            String status = switch (BattleManager.Phase.values()[Mth.clamp(battle.phaseOrdinal(), 0, 3)]) {
                case OPENING -> Component.translatable("gui.flashstake.arena.opening").getString();
                case REVEAL -> battle.winner() == 2
                    ? Component.translatable("gui.flashstake.arena.tie").getString()
                    : Component.translatable("gui.flashstake.arena.winner",
                        battle.winner() == 0 ? battle.hostName() : battle.guestName()).getString();
                default -> "";
            };
            g.drawString(this.font, UiText.ellipsize(this.font, status, this.imageWidth - 32),
                x + 16, y + 200, UiTheme.NEON_GOLD, false);
            return;
        }

        // LEFT: case picker + art + search
        int lx = x + PANEL_PAD;
        g.drawString(this.font, Component.translatable("gui.flashstake.arena.pick_case"),
            lx, y + 48, UiTheme.TEXT_DIM, false);

        List<CaseDefinition> all = CaseDefinition.all();
        if (!all.isEmpty()) {
            CaseDefinition def = all.get(Mth.clamp(this.caseIndex, 0, all.size() - 1));
            String name = Component.translatable(def.nameKey()).getString();
            UiText.drawCentered(g, this.font, Component.literal(UiText.ellipsize(this.font, name, LEFT_W - 8)),
                lx + LEFT_W / 2, y + 60, def.color(), 1.05f);

            int artX = lx;
            int artY = y + 74;
            int artW = LEFT_W;
            int artH = 70;
            WheelRenderer.roundedRect(g, artX - 1, artY - 1, artX + artW + 1, artY + artH + 1, 5,
                FlashFx.withAlpha(def.color(), 0.55f));
            if (!CaseArt.blitCover(g, def, artX, artY, artW, artH)) {
                WheelRenderer.roundedRect(g, artX, artY, artX + artW, artY + artH, 4, UiTheme.PANEL_TOP);
                UiText.drawCentered(g, this.font, Component.literal("?"),
                    artX + artW / 2, artY + artH / 2 - 4, def.color(), 1.4f);
            }

            UiText.drawCentered(g, this.font, Component.translatable("gui.flashstake.cases.price", def.price()),
                lx + LEFT_W / 2, y + 152, UiTheme.TEXT_DIM, 0.95f);
        }

        String hint = Component.translatable("gui.flashstake.arena.battle_hint").getString();
        this.drawWrapped(g, hint, lx, y + 174, LEFT_W, UiTheme.TEXT_MUTED, 3);

        if (battle.active() && battle.phaseOrdinal() == BattleManager.Phase.WAITING.ordinal()) {
            g.drawString(this.font, Component.translatable("gui.flashstake.arena.waiting_for_rival"),
                lx, y + 210, UiTheme.NEON_GOLD, false);
        }

        // RIGHT: active battle list
        int rx = x + RIGHT_X;
        WheelRenderer.roundedRect(g, rx - 4, y + 48, rx + RIGHT_W + 4, y + 248, 6, 0x33101828);
        g.drawString(this.font, Component.translatable("gui.flashstake.arena.open_battles"),
            rx, y + 54, UiTheme.NEON_CYAN, false);
        g.drawString(this.font, Component.translatable("gui.flashstake.arena.accept_hint"),
            rx, y + 66, UiTheme.TEXT_MUTED, false);

        int y0 = this.battleInviteListY() + 18;
        if (battleInvites.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.flashstake.arena.no_invites"),
                rx, y0, UiTheme.TEXT_MUTED, false);
        } else {
            for (int i = 0; i < Math.min(battleInvites.size(), INVITE_MAX); ++i) {
                ClientboundArenaLobbyPacket.BattleInvite inv = battleInvites.get(i);
                int rowY = y0 + i * INVITE_ROW_H;
                boolean over = inBox(mouseX, mouseY, rx, rowY, RIGHT_W, INVITE_ROW_H - 2);
                int bg = over ? 0x4422FFAA : 0x22101828;
                WheelRenderer.roundedRect(g, rx, rowY, rx + RIGHT_W, rowY + INVITE_ROW_H - 2, 4, bg);
                CaseDefinition cdef = CaseDefinition.byId(inv.caseId());
                String caseName = cdef == null ? inv.caseId() : Component.translatable(cdef.nameKey()).getString();
                String line = inv.hostName() + " · " + caseName + " · " + inv.price();
                g.drawString(this.font, UiText.ellipsize(this.font, line, RIGHT_W - 8),
                    rx + 4, rowY + 5, over ? UiTheme.NEON_LIME : UiTheme.TEXT, false);
            }
        }
    }

    /** Builds both reels once per battle; resumes mid-spin if the screen was opened late. */
    private void ensureBattleReels(CaseDefinition def, long now) {
        String key = battle.hostUuid() + "|" + battle.guestName() + "|" + battle.caseId()
            + "|" + battle.hostLoot().getItem() + "|" + battle.guestLoot().getItem();
        if (key.equals(this.battleReelKey) && this.hostReel != null && this.guestReel != null) {
            return;
        }
        this.battleReelKey = key;
        long elapsed = battle.phaseOrdinal() == BattleManager.Phase.OPENING.ordinal()
            ? Math.max(0L, (long) (BattleManager.OPENING_TICKS - battle.ticksLeft()) * 50L)
            : BATTLE_REEL_MS + BATTLE_REEL_STAGGER_MS;
        long start = now - elapsed;
        this.hostReel = this.buildBattleReel(def, battle.hostLoot(), start, BATTLE_REEL_MS);
        this.guestReel = this.buildBattleReel(def, battle.guestLoot(), start, BATTLE_REEL_MS + BATTLE_REEL_STAGGER_MS);
        this.hostReel.done = this.hostReel.progress(now) >= 1.0f;
        this.guestReel.done = this.guestReel.progress(now) >= 1.0f;
    }

    private BattleReel buildBattleReel(CaseDefinition def, ItemStack reward, long start, long duration) {
        ArrayList<ItemStack> strip = new ArrayList<>(BATTLE_STRIP);
        ArrayList<CaseDefinition.CaseTier> tiers = new ArrayList<>(BATTLE_STRIP);
        List<CaseDefinition.CaseEntry> pool = def == null ? List.of() : def.pool();
        double[] weights = new double[pool.size()];
        double total = 0.0;
        if (this.minecraft != null && this.minecraft.level != null) {
            for (int i = 0; i < pool.size(); ++i) {
                weights[i] = Math.max(0.0, CaseLoot.chancePercent(this.minecraft.level, def, pool.get(i)));
                total += weights[i];
            }
        }
        for (int k = 0; k < BATTLE_STRIP; ++k) {
            if (k == BATTLE_WINNER_INDEX || pool.isEmpty()) {
                strip.add(reward.copy());
                tiers.add(tierOf(def, reward));
                continue;
            }
            CaseDefinition.CaseEntry pick = pool.get(pool.size() - 1);
            if (total > 0.0) {
                double roll = this.reelRandom.nextDouble() * total;
                double cursor = 0.0;
                for (int i = 0; i < pool.size(); ++i) {
                    cursor += weights[i];
                    if (roll <= cursor) {
                        pick = pool.get(i);
                        break;
                    }
                }
            } else {
                pick = pool.get(this.reelRandom.nextInt(pool.size()));
            }
            strip.add(new ItemStack(pick.item(), pick.count()));
            tiers.add(pick.tier());
        }
        int reelW = BATTLE_CARD_W - 8;
        double jitter = (this.reelRandom.nextDouble() - 0.5) * BATTLE_CELL_W * 0.7;
        double target = BATTLE_WINNER_INDEX * BATTLE_CELL_W + BATTLE_CELL_W / 2.0 - reelW / 2.0 + jitter;
        return new BattleReel(strip, tiers, target, start, duration);
    }

    private static CaseDefinition.CaseTier tierOf(CaseDefinition def, ItemStack stack) {
        if (def == null) {
            return CaseDefinition.CaseTier.JUNK;
        }
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

    private void tickBattleReels() {
        if (this.hostReel == null || this.guestReel == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean crossed = false;
        int reelW = BATTLE_CARD_W - 8;
        for (BattleReel reel : new BattleReel[] { this.hostReel, this.guestReel }) {
            if (reel.done) {
                continue;
            }
            int cell = (int) ((reel.offsetAt(now) + reelW / 2.0) / BATTLE_CELL_W);
            if (cell != reel.lastCell) {
                reel.lastCell = cell;
                crossed = true;
            }
            if (reel.progress(now) >= 1.0f) {
                reel.done = true;
                playStopSound(reel.tiers.get(BATTLE_WINNER_INDEX));
            }
        }
        if (crossed) {
            FlashFx.tick(1.8f);
        }
    }

    private static void playStopSound(CaseDefinition.CaseTier tier) {
        switch (tier) {
            case LEGENDARY -> FlashFx.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.6f);
            case RARE -> FlashFx.play(SoundEvents.PLAYER_LEVELUP, 1.3f, 0.5f);
            case UNCOMMON -> FlashFx.play(SoundEvents.NOTE_BLOCK_PLING.value(), 1.4f, 0.5f);
            default -> FlashFx.play(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.4f);
        }
    }

    private void drawBattleReelCard(GuiGraphics g, int x, int y, String name, BattleReel reel, long value,
                                    boolean winner, boolean loser, float pulse, long now, int mouseX, int mouseY) {
        int w = BATTLE_CARD_W;
        int h = 104;
        int border = winner ? FlashFx.withAlpha(UiTheme.NEON_GOLD, 0.55f + 0.4f * pulse)
            : (loser ? UiTheme.PANEL_BORDER : FlashFx.withAlpha(UiTheme.NEON_CYAN, 0.6f));
        WheelRenderer.card(g, x, y, x + w, y + h, 6, border, UiTheme.PANEL_TOP, UiTheme.PANEL_BOTTOM);
        g.drawString(this.font, UiText.ellipsize(this.font, name, w - 16), x + 8, y + 7,
            loser ? UiTheme.TEXT_MUTED : UiTheme.TEXT, false);

        int rx1 = x + 4;
        int rx2 = x + w - 4;
        int ry1 = y + 22;
        int ry2 = ry1 + BATTLE_REEL_H;
        int centerX = (rx1 + rx2) / 2;
        g.fill(rx1 - 1, ry1 - 1, rx2 + 1, ry2 + 1, 0xFF3C4A5E);
        g.fill(rx1, ry1, rx2, ry2, 0xFF070B14);

        if (reel != null) {
            int reelW = rx2 - rx1;
            double offset = reel.offsetAt(now);
            int firstCell = Math.max(0, (int) (offset / BATTLE_CELL_W) - 1);
            int lastCell = Math.min(BATTLE_STRIP - 1, (int) ((offset + reelW) / BATTLE_CELL_W) + 1);
            ItemStack hovered = ItemStack.EMPTY;
            g.enableScissor(rx1, ry1, rx2, ry2);
            for (int k = firstCell; k <= lastCell; ++k) {
                int cx = rx1 + (int) Math.round(k * BATTLE_CELL_W - offset);
                CaseDefinition.CaseTier tier = reel.tiers.get(k);
                boolean win = reel.done && k == BATTLE_WINNER_INDEX;
                g.fill(cx + 1, ry1 + 1, cx + BATTLE_CELL_W - 1, ry2 - 1, win ? 0xFF2A2A34 : 0xFF1C1C22);
                g.fill(cx + 1, ry1 + 1, cx + BATTLE_CELL_W - 1, ry2 - 1, tier.slotTint());
                g.fill(cx + 1, ry2 - 3, cx + BATTLE_CELL_W - 1, ry2 - 1, tier.stripColor());
                this.renderScaledItem(g, reel.strip.get(k), cx + BATTLE_CELL_W / 2, ry1 + (BATTLE_REEL_H - 2) / 2, 1.5f);
                if (win) {
                    outline(g, cx + 1, ry1 + 1, cx + BATTLE_CELL_W - 1, ry2 - 1, reelPulse(tier.stripColor(), now));
                }
                if (reel.done && inBox(mouseX, mouseY, Math.max(cx, rx1), ry1, Math.min(BATTLE_CELL_W, rx2 - cx), BATTLE_REEL_H)) {
                    hovered = reel.strip.get(k);
                }
            }
            for (int s = 0; s < 8; ++s) {
                int alpha = (int) (200 * (1.0 - s / 8.0));
                int color = alpha << 24 | 0x070B14;
                g.fill(rx1 + s * 3, ry1, rx1 + s * 3 + 3, ry2, color);
                g.fill(rx2 - s * 3 - 3, ry1, rx2 - s * 3, ry2, color);
            }
            g.disableScissor();

            int marker = reel.done ? reelPulse(reel.tiers.get(BATTLE_WINNER_INDEX).stripColor(), now) : 0xFFE4AE39;
            g.fill(centerX - 1, ry1 - 2, centerX + 1, ry2 + 2, marker);
            g.fill(centerX - 3, ry1 - 3, centerX + 3, ry1 - 1, marker);
            g.fill(centerX - 3, ry2 + 1, centerX + 3, ry2 + 3, marker);

            if (reel.done) {
                ItemStack drop = reel.strip.get(BATTLE_WINNER_INDEX);
                g.drawString(this.font, UiText.ellipsize(this.font, drop.getHoverName().getString(), w - 16),
                    x + 8, ry2 + 8, reel.tiers.get(BATTLE_WINNER_INDEX).stripColor(), false);
                g.drawString(this.font, Component.translatable("gui.flashstake.value", value),
                    x + 8, ry2 + 20, winner ? UiTheme.NEON_GOLD : UiTheme.TEXT_DIM, false);
            } else {
                g.drawString(this.font, "…", x + 8, ry2 + 8, UiTheme.NEON_CYAN, false);
            }
            if (!hovered.isEmpty()) {
                this.pendingTooltip = hovered;
            }
        }
    }

    private void renderScaledItem(GuiGraphics g, ItemStack stack, int centerX, int centerY, float scale) {
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0.0f);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(stack, -8, -8);
        g.renderItemDecorations(this.font, stack, -8, -8);
        g.pose().popPose();
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private static int reelPulse(int color, long now) {
        float t = (float) (0.65 + 0.35 * Math.sin(now / 140.0));
        int r = (int) (((color >> 16) & 0xFF) * t);
        int gr = (int) (((color >> 8) & 0xFF) * t);
        int b = (int) ((color & 0xFF) * t);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private static final class BattleReel {
        final List<ItemStack> strip;
        final List<CaseDefinition.CaseTier> tiers;
        final double targetOffset;
        final long startMs;
        final long durationMs;
        boolean done;
        int lastCell = -1;

        BattleReel(List<ItemStack> strip, List<CaseDefinition.CaseTier> tiers, double targetOffset, long startMs, long durationMs) {
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
            return this.targetOffset * (1.0 - Math.pow(1.0 - t, 4.0));
        }
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxW, int color, int maxLines) {
        int line = 0;
        String rest = text;
        while (!rest.isEmpty() && line < maxLines) {
            int end = rest.length();
            while (end > 0 && this.font.width(rest.substring(0, end)) > maxW) {
                end--;
            }
            if (end <= 0) {
                break;
            }
            // Prefer break at space
            int space = rest.lastIndexOf(' ', end);
            if (space > 8 && end < rest.length()) {
                end = space;
            }
            String chunk = rest.substring(0, end).trim();
            if (line == maxLines - 1 && end < rest.length()) {
                chunk = UiText.ellipsize(this.font, rest, maxW);
            }
            g.drawString(this.font, chunk, x, y + line * 10, color, false);
            rest = rest.substring(Math.min(end, rest.length())).trim();
            line++;
        }
    }

    private void renderContract(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        g.drawString(this.font, Component.translatable("gui.flashstake.arena.contract_hint",
            ContractService.MIN_ITEMS, ContractService.MAX_ITEMS),
            x + 16, y + 50, UiTheme.TEXT_DIM, false);
        g.drawString(this.font, Component.translatable("gui.flashstake.arena.contract_selected",
            this.contractSlots.size(), this.contractInputValue()),
            x + 16, y + 66, UiTheme.NEON_CYAN, false);

        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < 36; ++i) {
            int[] xy = this.contractSlotPos(i);
            int cx = xy[0];
            int cy = xy[1];
            boolean sel = this.contractSlots.contains(i);
            boolean over = inBox(mouseX, mouseY, cx, cy, 17, 17);
            int bg = sel ? 0x5533F0FF : (over ? 0x442A3858 : 0xFF0A0E18);
            g.fill(cx, cy, cx + 17, cy + 17, bg);
            g.fill(cx, cy, cx + 17, cy + 1, sel ? UiTheme.NEON_CYAN : UiTheme.SLOT_BORDER);
            ItemStack stack = this.playerInvStack(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, cx + 1, cy + 1);
                g.renderItemDecorations(this.font, stack, cx + 1, cy + 1);
            }
            if (over && !stack.isEmpty()) {
                hovered = stack;
            }
        }

        int resultY = this.topPos + 88 + 4 * 18 + 12;
        if (lastContract != null && lastContract.ok()) {
            g.drawString(this.font, Component.translatable("gui.flashstake.arena.contract_result",
                lastContract.inputValue(), lastContract.rewardValue()),
                x + 16, resultY, UiTheme.NEON_GOLD, false);
            if (!lastContract.reward().isEmpty()) {
                g.renderItem(lastContract.reward(), x + 16, resultY + 14);
            }
            g.drawString(this.font, Component.translatable("gui.flashstake.arena.contract_to_inv"),
                x + 40, resultY + 18, UiTheme.NEON_LIME, false);
        }
        if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    private long contractInputValue() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return 0L;
        }
        long total = 0L;
        for (int idx : this.contractSlots) {
            ItemStack stack = this.playerInvStack(idx);
            if (!stack.isEmpty()) {
                total += Math.max(0L, Math.round(ItemValues.stackValue(this.minecraft.level, stack)));
            }
        }
        return total;
    }

    /** Wheel rotation (deg) at time {@code now}; lands the top pointer on a winner-colored segment. */
    private float duelWheelAngle(long now) {
        if (this.duelSpinStartMs <= 0L) {
            return this.duelTargetDeg;
        }
        float t = (now - this.duelSpinStartMs) / (float) DUEL_SPIN_MS;
        if (t >= 1.0f) {
            return this.duelTargetDeg;
        }
        float inv = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
        float eased = 1.0f - inv * inv * inv * inv;
        return this.duelTargetDeg * eased;
    }

    private void startDuelSpin() {
        long elapsed = Math.max(0L, (long) (DuelManager.SPIN_TICKS - duel.ticksLeft()) * 50L);
        this.duelSpinStartMs = System.currentTimeMillis() - Math.min(elapsed, DUEL_SPIN_MS);
        long seed = duel.hostUuid().hashCode() * 31L + duel.stake();
        java.util.Random rnd = new java.util.Random(seed);
        int seg = rnd.nextInt(DUEL_SEGMENTS / 2) * 2 + (duel.winner() == 1 ? 1 : 0);
        float segDeg = 360.0f / DUEL_SEGMENTS;
        float jitter = (rnd.nextFloat() - 0.5f) * segDeg * 0.6f;
        float land = seg * segDeg + segDeg / 2.0f + jitter;
        // Segment i sits at [i*segDeg + rot); pointer at 0° sees segment where -rot ≡ land.
        this.duelTargetDeg = 360.0f * DUEL_TURNS + (360.0f - land);
        this.duelLastSeg = -1;
    }

    private void renderDuelWheel(GuiGraphics g, int x, int y, long now) {
        boolean result = duel.phaseOrdinal() == DuelManager.Phase.RESULT.ordinal();
        if (this.duelSpinStartMs == 0L && this.duelTargetDeg == 0.0f) {
            this.startDuelSpin();
        }
        float rot = result ? this.duelTargetDeg : this.duelWheelAngle(now);
        float segDeg = 360.0f / DUEL_SEGMENTS;

        int seg = Math.floorMod((int) Math.floor((360.0f - (rot % 360.0f)) / segDeg), DUEL_SEGMENTS);
        if (!result && seg != this.duelLastSeg) {
            if (this.duelLastSeg != -1) {
                float t = Mth.clamp((now - this.duelSpinStartMs) / (float) DUEL_SPIN_MS, 0.0f, 1.0f);
                FlashFx.tick(1.0f + 0.8f * t);
            }
            this.duelLastSeg = seg;
        }

        String host = duel.hostName();
        String guest = duel.guestName().isEmpty() ? "…" : duel.guestName();
        int hostColor = UiTheme.NEON_MAGENTA;
        int guestColor = UiTheme.NEON_CYAN;
        float pulse = result ? FlashFx.pulse(this.revealPulseMs, 900L) : 0.0f;
        boolean hostWon = result && duel.winner() == 0;
        boolean guestWon = result && duel.winner() == 1;

        int lx = x + 20;
        int rx = x + this.imageWidth - 20;
        this.drawDuelSide(g, lx, y + 56, host, hostColor, hostWon, result && !hostWon, pulse, false);
        this.drawDuelSide(g, rx, y + 56, guest, guestColor, guestWon, result && !guestWon, pulse, true);
        UiText.drawCentered(g, this.font, Component.translatable("gui.flashstake.arena.duel_stake", duel.stake()),
            x + this.imageWidth / 2, y + 50, UiTheme.NEON_GOLD, 1.0f);

        float cx = x + this.imageWidth / 2.0f;
        float cy = y + 150;
        float r = 62.0f;

        WheelRenderer.disc(g, cx, cy, r + 5.0f, FlashFx.withAlpha(UiTheme.NEON_GOLD, 0.25f + 0.35f * pulse));
        WheelRenderer.disc(g, cx, cy, r + 2.0f, 0xFF070B14);
        for (int i = 0; i < DUEL_SEGMENTS; ++i) {
            boolean hostSeg = (i & 1) == 0;
            int base = hostSeg ? hostColor : guestColor;
            boolean winSeg = result && i == seg;
            float a = result ? (winSeg ? 1.0f : 0.35f) : 0.85f;
            int col = FlashFx.withAlpha(base, a);
            int dark = FlashFx.withAlpha(WheelRenderer.lerpColor(base | 0xFF000000, 0xFF000000, 0.45f), a);
            WheelRenderer.arc(g, cx, cy, 18.0f, r, i * segDeg + rot, segDeg - 1.0f, dark, col);
        }
        WheelRenderer.tickRing(g, cx, cy, r - 4.0f, r, rot, DUEL_SEGMENTS, 1, 0x66FFFFFF, 0xAAFFFFFF);
        WheelRenderer.disc(g, cx, cy, 18.0f, 0xFF0C1220);
        WheelRenderer.disc(g, cx, cy, 15.0f, FlashFx.withAlpha(UiTheme.NEON_GOLD, 0.8f));
        WheelRenderer.disc(g, cx, cy, 12.0f, 0xFF0C1220);
        String vs = "VS";
        g.drawString(this.font, vs, (int) cx - this.font.width(vs) / 2, (int) cy - 4, UiTheme.NEON_GOLD, false);

        WheelRenderer.pointer(g, cx, cy - 1.0f, r - 10.0f, r + 10.0f, 0.0f, 8.0f, 0xFF000000);
        WheelRenderer.pointer(g, cx, cy, r - 8.0f, r + 9.0f, 0.0f, 6.0f, UiTheme.NEON_GOLD);

        if (result) {
            String win = Component.translatable("gui.flashstake.arena.winner_short",
                duel.winner() == 0 ? host : guest).getString();
            int wc = duel.winner() == 0 ? hostColor : guestColor;
            UiText.drawCentered(g, this.font, Component.literal(UiText.ellipsize(this.font, win, this.imageWidth - 40)),
                (int) cx, y + 222, wc, 1.1f + 0.1f * pulse);
            UiText.drawCentered(g, this.font, Component.translatable("gui.flashstake.arena.duel_payout", duel.payout()),
                (int) cx, y + 238, UiTheme.NEON_LIME, 1.0f);
        } else {
            UiText.drawCentered(g, this.font, Component.translatable("gui.flashstake.arena.duel_spinning"),
                (int) cx, y + 226, UiTheme.TEXT_DIM, 1.0f);
        }
    }

    private void drawDuelSide(GuiGraphics g, int anchorX, int y, String name, int color, boolean won,
                              boolean lost, float pulse, boolean alignRight) {
        int w = 96;
        int x1 = alignRight ? anchorX - w : anchorX;
        int border = won ? FlashFx.withAlpha(UiTheme.NEON_GOLD, 0.6f + 0.4f * pulse)
            : FlashFx.withAlpha(color, lost ? 0.25f : 0.7f);
        WheelRenderer.card(g, x1, y, x1 + w, y + 30, 5, border, UiTheme.PANEL_TOP, UiTheme.PANEL_BOTTOM);
        WheelRenderer.roundedRect(g, x1 + 4, y + 4, x1 + 10, y + 26, 2, FlashFx.withAlpha(color, lost ? 0.35f : 1.0f));
        String shown = UiText.ellipsize(this.font, name, w - 20);
        g.drawString(this.font, shown, x1 + 14, y + 11, lost ? UiTheme.TEXT_MUTED : UiTheme.TEXT, false);
    }

    private void renderDuel(GuiGraphics g, int x, int y, int mouseX, int mouseY, long now) {
        if (duel.active() && duel.phaseOrdinal() >= DuelManager.Phase.SPINNING.ordinal()) {
            this.renderDuelWheel(g, x, y, now);
            return;
        }

        this.drawWrapped(g, Component.translatable("gui.flashstake.arena.duel_hint").getString(),
            x + 16, y + 50, this.imageWidth - 32, UiTheme.TEXT_DIM, 2);
        g.drawString(this.font, Component.translatable("gui.flashstake.arena.duel_min", DuelManager.MIN_STAKE),
            x + 124, y + 92, UiTheme.TEXT_MUTED, false);

        if (duel.active() && duel.phaseOrdinal() == DuelManager.Phase.WAITING.ordinal()) {
            g.drawString(this.font, Component.translatable("gui.flashstake.arena.waiting_for_rival"),
                x + 16, y + 120, UiTheme.NEON_GOLD, false);
        }

        g.drawString(this.font, Component.translatable("gui.flashstake.arena.open_duels"),
            x + 16, y + 148, UiTheme.TEXT_DIM, false);
        int y0 = y + 160;
        if (duelInvites.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.flashstake.arena.no_invites"),
                x + 16, y0, UiTheme.TEXT_MUTED, false);
        } else {
            for (int i = 0; i < Math.min(duelInvites.size(), 5); ++i) {
                ClientboundArenaLobbyPacket.DuelInvite inv = duelInvites.get(i);
                int rowY = y0 + i * 18;
                boolean over = inBox(mouseX, mouseY, x + 16, rowY, 328, 16);
                int bg = over ? 0x44FFC84A : 0x22101828;
                WheelRenderer.roundedRect(g, x + 16, rowY, x + 344, rowY + 16, 4, bg);
                g.drawString(this.font, UiText.ellipsize(this.font, inv.hostName() + " · " + inv.stake(), 310),
                    x + 22, rowY + 4, over ? UiTheme.NEON_GOLD : UiTheme.TEXT, false);
            }
        }
    }
}
