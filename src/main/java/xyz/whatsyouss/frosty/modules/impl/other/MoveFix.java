package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PostSendMovementPacketsEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.Rotations;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.Objects;

public class MoveFix extends Module {

    public ButtonSetting foragingOnly;

    public MoveFix() {
        super("MoveFix", category.Other);
    }

    @Override
    public String getDesc() {
        return "Correct movement while silent rotating";
    }

    public static boolean shouldApply() {
        return ModuleManager.moveFix.isEnabled() && Rotations.rotating;
    }

    @EventHandler
    public void onPostSendMovementPacket(PostSendMovementPacketsEvent event) {
        if (shouldApply()) {
        }
    }
}
