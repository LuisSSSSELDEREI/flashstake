package net.execheinz.upgrader.network;

import java.util.UUID;
import net.execheinz.upgrader.battle.BattleManager;
import net.execheinz.upgrader.contract.ContractService;
import net.execheinz.upgrader.duel.DuelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ServerboundArenaActionPacket(Action action, String text, long number, int[] slots) {
    public enum Action {
        BATTLE_CREATE,
        BATTLE_CANCEL,
        BATTLE_ACCEPT,
        DUEL_CREATE,
        DUEL_CANCEL,
        DUEL_ACCEPT,
        CONTRACT_SUBMIT,
        REFRESH
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeUtf(this.text == null ? "" : this.text, 64);
        buf.writeVarLong(this.number);
        int n = this.slots == null ? 0 : this.slots.length;
        buf.writeVarInt(n);
        for (int i = 0; i < n; ++i) {
            buf.writeVarInt(this.slots[i]);
        }
    }

    public static ServerboundArenaActionPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        String text = buf.readUtf(64);
        long number = buf.readVarLong();
        int n = buf.readVarInt();
        int[] slots = new int[Math.min(n, 16)];
        for (int i = 0; i < slots.length; ++i) {
            slots[i] = buf.readVarInt();
        }
        return new ServerboundArenaActionPacket(action, text, number, slots);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            BattleManager battles = BattleManager.get();
            DuelManager duels = DuelManager.get();
            switch (this.action) {
                case BATTLE_CREATE -> {
                    if (battles != null) {
                        battles.create(player, this.text);
                    }
                }
                case BATTLE_CANCEL -> {
                    if (battles != null) {
                        battles.cancel(player);
                    }
                }
                case BATTLE_ACCEPT -> {
                    if (battles != null) {
                        try {
                            battles.accept(player, UUID.fromString(this.text));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                case DUEL_CREATE -> {
                    if (duels != null) {
                        duels.create(player, this.number);
                    }
                }
                case DUEL_CANCEL -> {
                    if (duels != null) {
                        duels.cancel(player);
                    }
                }
                case DUEL_ACCEPT -> {
                    if (duels != null) {
                        try {
                            duels.accept(player, UUID.fromString(this.text));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                case CONTRACT_SUBMIT -> ContractService.submit(player, (int) this.number, this.slots);
                case REFRESH -> {
                    if (battles != null) {
                        battles.syncTo(player);
                    }
                    if (duels != null) {
                        duels.syncTo(player);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    public static ServerboundArenaActionPacket battleCreate(String caseId) {
        return new ServerboundArenaActionPacket(Action.BATTLE_CREATE, caseId, 0L, null);
    }

    public static ServerboundArenaActionPacket battleCancel() {
        return new ServerboundArenaActionPacket(Action.BATTLE_CANCEL, "", 0L, null);
    }

    public static ServerboundArenaActionPacket battleAccept(String hostUuid) {
        return new ServerboundArenaActionPacket(Action.BATTLE_ACCEPT, hostUuid, 0L, null);
    }

    public static ServerboundArenaActionPacket duelCreate(long stake) {
        return new ServerboundArenaActionPacket(Action.DUEL_CREATE, "", stake, null);
    }

    public static ServerboundArenaActionPacket duelCancel() {
        return new ServerboundArenaActionPacket(Action.DUEL_CANCEL, "", 0L, null);
    }

    public static ServerboundArenaActionPacket duelAccept(String hostUuid) {
        return new ServerboundArenaActionPacket(Action.DUEL_ACCEPT, hostUuid, 0L, null);
    }

    public static ServerboundArenaActionPacket contract(int source, int[] slots) {
        return new ServerboundArenaActionPacket(Action.CONTRACT_SUBMIT, "", source, slots);
    }

    public static ServerboundArenaActionPacket refresh() {
        return new ServerboundArenaActionPacket(Action.REFRESH, "", 0L, null);
    }
}
