package xyz.whatsyouss.frosty.modules.impl.other;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

/**
 * 防盗号（参考 V5-Client 的 RatProtection.js，重写为 Java / MC 26.2）。
 *
 * 原理：周期性向 Mojang 会话服务器发送 join 请求（自身 accessToken + 档案 UUID +
 * 随机 serverId），占满 Mojang 对该账号的 join 接口速率限制。窃取到 token 的 RAT
 * 再尝试 join 验证时会吃到 429，无法冒充你进入正版验证服务器。
 *
 * 注意：限流是双向的——开启后你自己快速进服/切服也可能被 Mojang 429 拒绝。
 * token 仅发送至硬编码的 Mojang 官方域名，异步请求不阻塞游戏线程，离线模式自动跳过。
 */
public class RatProtection extends Module {

    private static final URI JOIN_URI =
            URI.create("https://sessionserver.mojang.com/session/minecraft/join");

    public SliderSetting interval;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private int tickCounter;

    public RatProtection() {
        super("RatProtection", "防盗号", category.Other);

        this.registerSetting(interval = new SliderSetting("Interval", "s", 1, 1, 10, 1, "请求间隔"));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return; // 仅在已进入世界时工作（与 V5 的 World.isLoaded 对齐）

        if (++tickCounter < (int) interval.getInput() * 20) return;
        tickCounter = 0;

        postJoinRequest();
    }

    private void postJoinRequest() {
        String token = mc.getUser().getAccessToken();
        if (token == null || token.isEmpty()) return; // 离线模式无 token，直接跳过

        String selectedProfile = mc.getUser().getProfileId().toString().replace("-", "");
        String serverId = UUID.randomUUID().toString().replace("-", "");

        String body = "{\"accessToken\":\"" + token
                + "\",\"selectedProfile\":\"" + selectedProfile
                + "\",\"serverId\":\"" + serverId + "\"}";

        HttpRequest request = HttpRequest.newBuilder(JOIN_URI)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        Utils.addChatMessage("§e[RatProtection] §f已启用会话占坑保护。注意：快速进服/切服时你自己也可能被 Mojang 限流（429）。");
    }

    @Override
    public void onDisable() {
        tickCounter = 0;
    }
}
