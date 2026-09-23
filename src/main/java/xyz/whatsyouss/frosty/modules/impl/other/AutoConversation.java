package xyz.whatsyouss.frosty.modules.impl.other;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

/**
 * 自动 NPC 对话（参考 V5-Client 的 AutoConversation.js，重写为 Java / MC 26.2）。
 *
 * 收到以 "[NPC]" 或 "Select an option:" 开头、且带 RUN_COMMAND 点击选项的聊天消息时，
 * 延迟指定 tick 后自动执行第一个选项的命令。actionToken 机制保证：
 * 模块关闭或新选项消息到达后，尚未执行的旧任务自动作废。
 */
public class AutoConversation extends Module {

    public ButtonSetting selectFirst;
    public SliderSetting delay;

    private String pendingCommand;
    private int pendingToken;
    private int ticksRemaining;
    private int actionToken;

    public AutoConversation() {
        super("AutoConversation", "自动对话", category.Other);

        this.registerSetting(selectFirst = new ButtonSetting("Select first", "自动选第一个", true));
        this.registerSetting(delay = new SliderSetting("Delay", "tick", 5, 1, 20, 1, "点击延迟"));
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        if (!Utils.nullCheck()) return;

        // Hypixel 的消息文本内嵌 § 格式码，Component.getString() 不会剥离它们
        // （V5 的 getUnformattedText 会剥离），必须先 stripColor 再做前缀匹配
        String plain = StringUtil.stripColor(event.getMessage().getString());
        if (plain == null) return;
        plain = plain.trim();
        if (!plain.startsWith("[NPC]") && !plain.startsWith("Select an option:")) return;

        List<String> commands = new ArrayList<>();
        collectCommands(event.getMessage(), commands);
        if (commands.isEmpty()) return;

        if (commands.size() == 1 || selectFirst.isToggled()) {
            this.pendingCommand = commands.get(0);
            this.pendingToken = this.actionToken;
            this.ticksRemaining = (int) delay.getInput();
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (pendingCommand == null) return;
        if (!Utils.nullCheck()) {
            pendingCommand = null;
            return;
        }
        if (--ticksRemaining > 0) return;

        String command = pendingCommand;
        int token = pendingToken;
        pendingCommand = null;

        if (token != actionToken) return; // 已被 onDisable 作废

        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (!command.isEmpty()) {
            mc.player.connection.sendCommand(command);
        }
    }

    /**
     * 递归遍历 Component 树，收集所有 RUN_COMMAND 点击事件的命令。
     */
    private static void collectCommands(Component component, List<String> out) {
        if (component == null) return;

        if (component.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
            out.add(run.command());
        }
        for (Component sibling : component.getSiblings()) {
            collectCommands(sibling, out);
        }
    }

    @Override
    public void onDisable() {
        actionToken++;
        pendingCommand = null;
    }
}
