package xyz.whatsyouss.frosty.utility;

import net.minecraft.network.chat.Component;

public interface ChatComponentAccessor {
    Component frosty$getMessageAt(double mouseX, double mouseY, int screenHeight);
}