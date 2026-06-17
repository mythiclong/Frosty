package xyz.whatsyouss.frosty.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.BlockUtils;
import xyz.whatsyouss.frosty.utility.Utils;

public class Eagle extends Module {

    public Eagle() {
        super("Eagle", category.Movement);
    }

    @Override
    public void onDisable() {
        mc.options.keyShift.setDown(false);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!mc.player.onGround()) {
            return;
        }
        mc.options.keyShift.setDown(BlockUtils.isEdgeOfBlock());
    }
}
