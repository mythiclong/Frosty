package xyz.whatsyouss.frosty.modules.impl.render;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BufferSource;
import xyz.whatsyouss.frosty.utility.RenderLayers;
import xyz.whatsyouss.frosty.utility.Utils;

/**
 * 自定义名牌渲染（参考 OpenMyau 1.8.9 的 NameTags 设计，使用 MC 26.2 渲染 API 重写）。
 *
 * 功能：距离文本（None/Default/Vape）、血量显示（None/HP/Hearts/Tab 计分板）、
 * 血量渐变文字颜色、背景透明度、文字阴影、距离自动缩放、潜行/隐身紫色背景、
 * 玩家/假人/自己/怪物/动物/Boss 过滤。
 *
 * 原版名牌由 EntityRendererMixin 在本模块接管目标上抑制。
 */
public class Nametags extends Module {

    private static final DecimalFormat HEALTH_FORMAT =
            new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final double MAX_RENDER_DISTANCE = 512.0;

    public SliderSetting scale;
    public ButtonSetting autoScale;
    public SliderSetting background;
    public ButtonSetting shadow;
    public SelectSetting distanceMode;
    public SelectSetting healthMode;
    public ButtonSetting players, bots, self, mobs, animals, bosses;

    private final String[] distanceModes = new String[]{"None", "Default", "Vape"};
    private final String[] CNdistanceModes = new String[]{"无", "默认", "Vape"};
    private final String[] healthModes = new String[]{"None", "HP", "Hearts", "Tab"};
    private final String[] CNhealthModes = new String[]{"无", "血量", "心", "计分板"};

    public Nametags() {
        super("Nametags", "名字标签", category.Render);

        this.registerSetting(scale = new SliderSetting("Scale", 1, 0.05, 5, 0.05, "大小"));
        this.registerSetting(autoScale = new ButtonSetting("Auto scale", "距离缩放", true));
        this.registerSetting(background = new SliderSetting("Background", "%", 25, 0, 100, 5, "背景透明度"));
        this.registerSetting(shadow = new ButtonSetting("Shadow", "阴影", true));
        this.registerSetting(distanceMode = new SelectSetting("Distance", "距离显示", 0, distanceModes, CNdistanceModes));
        this.registerSetting(healthMode = new SelectSetting("Health", "血量显示", 2, healthModes, CNhealthModes));
        this.registerSetting(players = new ButtonSetting("Players", "玩家", true));
        this.registerSetting(bots = new ButtonSetting("Bots", "假人", false));
        this.registerSetting(self = new ButtonSetting("Self", "自己", false));
        this.registerSetting(mobs = new ButtonSetting("Mobs", "怪物", false));
        this.registerSetting(animals = new ButtonSetting("Animals", "动物", false));
        this.registerSetting(bosses = new ButtonSetting("Bosses", "Boss", false));
    }

    /**
     * 是否应对该实体绘制自定义名牌（同时用于 EntityRendererMixin 抑制原版名牌）。
     */
    public boolean shouldRenderTag(LivingEntity entity) {
        if (!Utils.nullCheck()) return false;
        if (entity.deathTime > 0) return false;
        if (mc.player.distanceTo(entity) > MAX_RENDER_DISTANCE) return false;

        if (entity == mc.player) {
            return self.isToggled() && mc.options.getCameraType() != CameraType.FIRST_PERSON;
        }
        if (entity instanceof Player player) {
            return AntiBot.isBot(player) ? bots.isToggled() : players.isToggled();
        }
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return !entity.isInvisible() && bosses.isToggled();
        }
        if (entity instanceof Monster || entity instanceof Slime) {
            return mobs.isToggled();
        }
        if (entity instanceof Animal || entity instanceof AgeableWaterCreature
                || entity instanceof AmbientCreature || entity instanceof AbstractVillager) {
            return animals.isToggled();
        }
        return false;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && shouldRenderTag(living)) {
                renderTag(event.getMatrix(), living, event.getDelta());
            }
        }
    }

    private void renderTag(PoseStack matrix, LivingEntity entity, float delta) {
        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        double x = Mth.lerp(delta, entity.xOld, entity.getX());
        double y = Mth.lerp(delta, entity.yOld, entity.getY());
        double z = Mth.lerp(delta, entity.zOld, entity.getZ());
        double distance = cam.distanceTo(new Vec3(x, y, z));

        // 文本内容：距离前缀 + 名字 + 血量后缀（OpenMyau 版式）
        String distanceText = switch ((int) distanceMode.getValue()) {
            case 1 -> "§7" + (int) distance + "m§r ";
            case 2 -> "§a[§f" + (int) distance + "§a]§r ";
            default -> "";
        };

        float health = entity.getHealth();
        float absorption = entity.getAbsorptionAmount();
        float max = entity.getMaxHealth();
        float percent = max > 0 ? Mth.clamp((health + absorption) / max, 0.0F, 1.0F) : 1.0F;

        String healthText = switch ((int) healthMode.getValue()) {
            case 1 -> String.format(" %d%s", (int) health,
                    absorption > 0 ? String.format(" §6%d§r", (int) absorption) : "§r");
            case 2 -> String.format(" %s%s", HEALTH_FORMAT.format(health / 2.0),
                    absorption > 0 ? String.format(" §6%s§r", HEALTH_FORMAT.format(absorption / 2.0)) : "§r");
            case 3 -> tabHealthText(entity);
            default -> "";
        };

        String name = displayName(entity);
        if (name == null || name.isBlank()) return;

        String text = distanceText + "§f" + name + "§r" + healthText;

        matrix.pushPose();
        matrix.translate((float) (x - cam.x),
                (float) (y - cam.y + entity.getBbHeight() + (entity.isShiftKeyDown() ? 0.35 : 0.5)),
                (float) (z - cam.z));
        matrix.mulPose(mc.getEntityRenderDispatcher().camera.rotation());

        // OpenMyau 的缩放公式：pow(clamp(distance, 6, 128), 0.75) * 0.0075 * scale
        double s = Math.pow(Mth.clamp(autoScale.isToggled() ? distance : 0.0, 6.0, 128.0), 0.75)
                * 0.0075 * scale.getInput();
        matrix.scale((float) s, (float) -s, (float) s);

        Font font = mc.font;
        int width = font.width(text);
        int baseColor = healthBlend(percent);

        BufferSource bs = new BufferSource();
        Matrix4f pose = matrix.last().pose();

        // 背景矩形（潜行/隐身时为紫色，OpenMyau 风格）
        int bgOpacity = (int) background.getInput();
        if (bgOpacity > 0) {
            int alpha = bgOpacity * 255 / 100;
            boolean sneakingOrInvisible = entity.isShiftKeyDown() || entity.isInvisible();
            int bgColor = sneakingOrInvisible
                    ? (alpha << 24) | (0x55 << 16) | (0x00 << 8) | 0x55
                    : (alpha << 24);

            float x1 = -width / 2f - 1;
            float y1 = -1;
            float x2 = width / 2f + (shadow.isToggled() ? 1 : 0);
            float y2 = font.lineHeight + (shadow.isToggled() ? 1 : 0);

            VertexConsumer quads = bs.getBuffer(RenderLayers.ESP_QUADS);
            // 两种绕序各发一次，规避面剔除
            quads.addVertex(pose, x1, y1, 0).setColor(bgColor);
            quads.addVertex(pose, x1, y2, 0).setColor(bgColor);
            quads.addVertex(pose, x2, y2, 0).setColor(bgColor);
            quads.addVertex(pose, x2, y1, 0).setColor(bgColor);
            quads.addVertex(pose, x2, y1, 0).setColor(bgColor);
            quads.addVertex(pose, x2, y2, 0).setColor(bgColor);
            quads.addVertex(pose, x1, y2, 0).setColor(bgColor);
            quads.addVertex(pose, x1, y1, 0).setColor(bgColor);
        }

        // 文字（SEE_THROUGH 隔障碍物可见，基础色按血量渐变）
        Font.PreparedText prepared = font.prepareText(text, -width / 2f, 0f,
                baseColor, shadow.isToggled(), 0);
        prepared.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptRenderable(TextRenderable renderable) {
                RenderType renderType = renderable.renderType(Font.DisplayMode.SEE_THROUGH);
                renderable.render(pose, bs.getBuffer(renderType), 15728880, false);
            }
        });

        bs.uploadAndDraw();
        matrix.popPose();
    }

    /**
     * 计分板 BELOW_NAME 目标的分数（OpenMyau 的 TAB 血量模式）。
     */
    private String tabHealthText(LivingEntity entity) {
        if (mc.level == null) return "";
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
        if (objective == null) return "";

        String target = entity.getScoreboardName();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (target.equals(entry.owner())) {
                return " §e" + entry.value() + "§r";
            }
        }
        return "";
    }

    private String displayName(LivingEntity entity) {
        Component displayName = entity.getDisplayName();
        if (displayName == null) return null;

        if (entity instanceof Player && ModuleManager.nickHider.isEnabled()
                && !ModuleManager.nickHider.name.getValue().isEmpty()) {
            displayName = NickHider.processText(displayName.copy());
        }
        return displayName.getString();
    }

    /**
     * 血量渐变颜色：满血绿色 → 半血黄色 → 低血红色（OpenMyau ColorUtil.getHealthBlend）。
     */
    private static int healthBlend(float percent) {
        float p = Mth.clamp(percent, 0.0F, 1.0F);
        int r = (int) (255 * Math.min(1.0F, 2.0F * (1.0F - p)));
        int g = (int) (255 * Math.min(1.0F, 2.0F * p));
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
