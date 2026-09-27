/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package net.execheinz.upgrader.network;

import java.util.function.Supplier;
import net.execheinz.upgrader.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundUpgradeResultPacket(boolean success, float landingAngle, int durationTicks, float chance) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.success);
        buf.writeFloat(this.landingAngle);
        buf.writeVarInt(this.durationTicks);
        buf.writeFloat(this.chance);
    }

    public static ClientboundUpgradeResultPacket decode(FriendlyByteBuf buf) {
        return new ClientboundUpgradeResultPacket(buf.readBoolean(), buf.readFloat(), buf.readVarInt(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientPacketHandler.handleResult(this)));
        ctx.setPacketHandled(true);
    }
}
