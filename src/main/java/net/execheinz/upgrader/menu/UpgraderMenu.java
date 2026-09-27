/*
 * Decompiled with CFR 0.152.
 */
package net.execheinz.upgrader.menu;

import javax.annotation.Nullable;
import net.execheinz.upgrader.Config;
import net.execheinz.upgrader.network.ClientboundUpgradeResultPacket;
import net.execheinz.upgrader.network.ClientboundUpgraderSyncPacket;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.registry.ModMenus;
import net.execheinz.upgrader.value.ItemValues;
import net.execheinz.upgrader.value.UpgradeOdds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

public class UpgraderMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    private static final int INV_START = 1;
    private static final int INV_END = 37;
    public static final int INV_X = 79;
    public static final int INV_Y = 206;
    public static final int HOTBAR_Y = 264;
    public static final int INPUT_X = 50;
    public static final int INPUT_Y = 52;
    public static final int TARGET_X = 256;
    public static final int TARGET_Y = 52;

    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            UpgraderMenu.this.slotsChanged(this);
        }
    };

    private final Player player;
    @Nullable
    private Item target;
    private int targetCount = 1;
    private int spinTicksLeft;
    private boolean pendingWin;
    private String lastSyncedTargetId = null;
    private long lastSyncedInputValue = -1L;
    private long lastSyncedTargetValue = -1L;
    private int lastSyncedTargetCount = -1;

    public UpgraderMenu(int windowId, Inventory playerInventory) {
        super((MenuType<?>) ModMenus.UPGRADER.get(), windowId);
        this.player = playerInventory.player;
        this.addSlot(new Slot(this.input, 0, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !UpgraderMenu.this.isSpinning() && !ItemValues.isBlacklisted(stack.getItem());
            }

            @Override
            public boolean mayPickup(Player p) {
                return !UpgraderMenu.this.isSpinning();
            }
        });
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public boolean isSpinning() {
        return this.spinTicksLeft > 0;
    }

    public ItemStack getInputStack() {
        return this.input.getItem(0);
    }

    @Nullable
    public Item getTarget() {
        return this.target;
    }

    public int getTargetCount() {
        return this.targetCount;
    }

    public void setTargetClient(@Nullable Item item, int count) {
        this.target = item;
        this.targetCount = item == null ? 1 : clampCount(item, count);
    }

    public void setTarget(ServerPlayer serverPlayer, @Nullable Item item, int count) {
        if (this.isSpinning()) {
            return;
        }
        if (item != null && ItemValues.isBlacklisted(item)) {
            return;
        }
        this.target = item;
        this.targetCount = item == null ? 1 : clampCount(item, count);
        this.syncToClient(serverPlayer, true);
    }

    public static int clampCount(@Nullable Item item, int count) {
        if (item == null) {
            return 1;
        }
        int max = Math.max(1, item.getMaxStackSize());
        return Mth.clamp(count, 1, max);
    }

    public void startUpgrade(ServerPlayer serverPlayer, boolean fast) {
        if (this.isSpinning()) {
            return;
        }
        ItemStack stack = this.getInputStack();
        if (stack.isEmpty() || this.target == null) {
            return;
        }
        if (ItemValues.isBlacklisted(stack.getItem())) {
            return;
        }
        this.targetCount = clampCount(this.target, this.targetCount);
        double chance = UpgradeOdds.chance(serverPlayer.level(), stack, this.target, this.targetCount);
        RandomSource random = serverPlayer.getRandom();
        this.pendingWin = random.nextDouble() < chance;
        this.spinTicksLeft = fast ? 18 : Config.spinTicks;
        ModNetwork.sendTo(serverPlayer, new ClientboundUpgradeResultPacket(
            this.pendingWin,
            landingAngle(random, chance, this.pendingWin),
            this.spinTicksLeft,
            (float) chance
        ));
    }

    private static float landingAngle(RandomSource random, double chance, boolean win) {
        float zone = (float) (chance * 360.0);
        float span = win ? zone : 360.0f - zone;
        float margin = Math.min(1.5f, span * 0.2f);
        float usable = Math.max(0.01f, span - margin * 2.0f);
        float offset = margin + random.nextFloat() * usable;
        return win ? offset : (zone + offset) % 360.0f;
    }

    private void applyResult() {
        ItemStack stack = this.getInputStack();
        if (stack.isEmpty()) {
            return;
        }
        // Always clear the upgrade slot — win goes to inventory, fail is lost
        this.input.setItem(0, ItemStack.EMPTY);
        this.input.setChanged();
        if (this.pendingWin && this.target != null && this.player instanceof ServerPlayer serverPlayer) {
            ItemStack reward = new ItemStack((ItemLike) this.target, clampCount(this.target, this.targetCount));
            if (!serverPlayer.getInventory().add(reward.copy())) {
                serverPlayer.drop(reward.copy(), false);
            }
        }
        if (this.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                this.pendingWin ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS,
                0.7f,
                this.pendingWin ? 1.4f : 0.8f
            );
        }
    }

    @Override
    public void broadcastChanges() {
        if (this.player instanceof ServerPlayer serverPlayer) {
            if (this.spinTicksLeft > 0 && --this.spinTicksLeft == 0) {
                this.applyResult();
            }
            this.syncToClient(serverPlayer, false);
        }
        super.broadcastChanges();
    }

    private void syncToClient(ServerPlayer serverPlayer, boolean force) {
        ResourceLocation key = this.target == null ? null : ForgeRegistries.ITEMS.getKey(this.target);
        String targetId = key == null ? "" : key.toString();
        long inputValue = Math.round(ItemValues.stackValue(serverPlayer.level(), this.getInputStack()));
        int count = this.target == null ? 1 : clampCount(this.target, this.targetCount);
        long targetValue = this.target == null
            ? 0L
            : Math.round(ItemValues.unitValue(serverPlayer.level(), this.target) * (double) count);
        if (!force
            && targetId.equals(this.lastSyncedTargetId)
            && inputValue == this.lastSyncedInputValue
            && targetValue == this.lastSyncedTargetValue
            && count == this.lastSyncedTargetCount) {
            return;
        }
        this.lastSyncedTargetId = targetId;
        this.lastSyncedInputValue = inputValue;
        this.lastSyncedTargetValue = targetValue;
        this.lastSyncedTargetCount = count;
        float chance = (float) UpgradeOdds.chance(inputValue, targetValue);
        ModNetwork.sendTo(serverPlayer, new ClientboundUpgraderSyncPacket(targetId, inputValue, targetValue, chance, count));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player p) {
        if (this.isSpinning()) {
            return;
        }
        super.clicked(slotId, button, clickType, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (this.isSpinning()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0 ? !this.moveItemStackTo(stack, 1, 37, true) : !this.moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player p) {
        if (this.spinTicksLeft > 0) {
            this.spinTicksLeft = 0;
            this.applyResult();
        }
        super.removed(p);
        this.clearContainer(p, this.input);
    }

    @Override
    public boolean stillValid(Player p) {
        return p.isAlive();
    }
}
