package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.AntiTexture;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
    @Shadow @Final private Connection connection;
    @Shadow @Final @Nullable
    protected ServerData serverData;

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void onPackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (!ModuleManager.antiTexture.isEnabled()) {
            AntiTexture.vanillaTooltip = false;
            return;
        }

        if (!packet.url().contains("resourcepacks.hypixel.net") || serverData == null ||
                serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            AntiTexture.vanillaTooltip = false;
            return;
        }

        connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
        connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));

        AntiTexture.vanillaTooltip = true;
        ci.cancel();
    }
}