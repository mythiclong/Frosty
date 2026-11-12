package xyz.whatsyouss.frosty.mixin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.commands.Command;
import xyz.whatsyouss.frosty.commands.CommandManager;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    @Nullable
    private PacketListener packetListener;

    @Invoker("genericsFtw")
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void frosty$invokeGenericsFtw(Packet packet, PacketListener listener) {
        throw new AssertionError();
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            // 先在副本上过滤被取消的子包；原 bundle.subPackets() 可能是不可变列表，
            // 直接 iterator().remove() 会在 Netty 线程上抛 UnsupportedOperationException 导致断连
            List<Packet<? super ClientGamePacketListener>> kept = new ArrayList<>();
            boolean anyCancelled = false;
            for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
                if (Frosty.EVENT_BUS.post(new ReceivePacketEvent(sub, (Connection) (Object) this)).isCancelled()) {
                    anyCancelled = true;
                } else {
                    kept.add(sub);
                }
            }
            if (anyCancelled) {
                ci.cancel();
                PacketListener listener = this.packetListener;
                if (listener != null) {
                    for (Packet<? super ClientGamePacketListener> sub : kept) {
                        frosty$invokeGenericsFtw(sub, listener);
                    }
                }
            }
        } else if (Frosty.EVENT_BUS.post(new ReceivePacketEvent(packet, (Connection) (Object) this)).isCancelled())
            ci.cancel();
    }

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"))
    private static void onConnect(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        Frosty.EVENT_BUS.post(ServerConnectEndEvent.get(address));
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (Frosty.EVENT_BUS.post(new SendPacketEvent(packet, (Connection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
        if (packet instanceof ServerboundChatPacket && ModuleManager.commands.isEnabled()) {
            String message = ((ServerboundChatPacket) packet).message();
            if (message.startsWith(".")) {
                handleCommand(message.substring(1));
                ci.cancel();
            }
        }
    }

    @Unique
    private void handleCommand(String input) {
        String[] parts = input.split(" ");
        if (parts.length == 0) return;

        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Command command = CommandManager.getCommandByName(commandName.toLowerCase());

        if (command != null) {
            try {
                command.execute(args);
            } catch (Exception e) {
                Utils.addChatMessage("§cError executing command: " + e.getMessage());
            }
        } else {
            Utils.addChatMessage("§cUnknown command: " + commandName);
        }
    }
}