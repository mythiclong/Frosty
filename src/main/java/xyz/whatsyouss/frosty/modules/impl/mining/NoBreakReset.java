package xyz.whatsyouss.frosty.modules.impl.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.Utils;

public class NoBreakReset extends Module {

    public NoBreakReset() {
        super("NoBreakReset", "无破坏重置", category.Mining);
    }

    @EventHandler
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) {
            // 只拦截玩家背包（containerId 0），避免影响箱子等容器界面
            if (packet.getContainerId() != 0) {
                return;
            }

            ItemStack stack = packet.getItem();
            if (stack.isEmpty()) {
                return;
            }

            // 直接检查本包内物品的名字，不再缓存到字段（旧实现 holding 有 null 与残留问题）
            Component customName = stack.getCustomName();
            String name = customName != null
                    ? Utils.getLiteralByText(Component.literal(customName.toString()))
                    : "";

            if (stack.getItem() == Items.STONE_AXE && name.contains("Fig")
                    || stack.getItem() == Items.PRISMARINE_SHARD && name.contains("Drill")
                    || stack.getItem() == Items.DIAMOND_PICKAXE) {
                event.setCancelled(true);
            }
        }
    }
}
