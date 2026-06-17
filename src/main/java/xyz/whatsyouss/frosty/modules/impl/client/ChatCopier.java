package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

public class ChatCopier extends Module {

    public ButtonSetting stripColorCode;

    public ChatCopier() {
        super("ChatCopier", category.Client);

        this.registerSetting(stripColorCode = new ButtonSetting("Strip color code", true));
    }

    @Override
    public String getDesc() {
        return "Copy chat message by right click";
    }
}