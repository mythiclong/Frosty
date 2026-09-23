package xyz.whatsyouss.frosty.modules.impl.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.impl.other.AntiBot;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

/**
 * 实体透视上色（参考 OpenMyau 1.8.9 的 Chams，重写为 MC 26.2）。
 *
 * 1.8.9 靠 GL_POLYGON_OFFSET 让模型穿透墙体；26.2 管线不再暴露固定功能 GL，
 * 改为在 LivingEntityRenderer#getRenderType 处把 RenderType 换成无深度测试的
 * entityTranslucentNoDepth（RenderLayers），视觉效果等价。
 *
 * 过滤逻辑与 OpenMyau 一致：deathTime / 512 格距离 / 分类开关。
 * friends/enemies 依赖 TeamUtil 体系，Frosty 没有该基础设施，故不移植；
 * bots 复用 AntiBot.isBot（AntiBot 未开启时所有玩家走 players 开关）。
 */
public class Chams extends Module {

    public ButtonSetting players;
    public ButtonSetting bots;
    public ButtonSetting self;
    public ButtonSetting bosses;
    public ButtonSetting mobs;
    public ButtonSetting creepers;
    public ButtonSetting endermen;
    public ButtonSetting blazes;
    public ButtonSetting animals;

    public Chams() {
        super("Chams", "实体透视", category.Render);

        this.registerSetting(players = new ButtonSetting("Players", "玩家", true));
        this.registerSetting(bots = new ButtonSetting("Bots", "假人", false));
        this.registerSetting(self = new ButtonSetting("Self", "自己", false));
        this.registerSetting(bosses = new ButtonSetting("Bosses", "Boss", false));
        this.registerSetting(mobs = new ButtonSetting("Mobs", "怪物", false));
        this.registerSetting(creepers = new ButtonSetting("Creepers", "苦力怕", false));
        this.registerSetting(endermen = new ButtonSetting("Endermen", "末影人", false));
        this.registerSetting(blazes = new ButtonSetting("Blazes", "烈焰人", false));
        this.registerSetting(animals = new ButtonSetting("Animals", "动物", false));
    }

    /**
     * OpenMyau shouldRenderChams 的 26.2 移植版。由
     * LivingEntityRendererMixin 在 getRenderType 返回值处调用。
     */
    public boolean shouldRender(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (living.deathTime > 0) return false;

        Entity camera = mc.getCameraEntity();
        if (camera != null && camera.distanceTo(entity) > 512.0F) return false;

        if (entity instanceof Player player) {
            if (entity != mc.player && entity != camera) {
                if (AntiBot.isBot(player)) {
                    return bots.isToggled();
                }
                return players.isToggled();
            }
            return self.isToggled() && !mc.options.getCameraType().isFirstPerson();
        }

        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return !entity.isInvisible() && bosses.isToggled();
        }

        if (!(entity instanceof Monster) && !(entity instanceof Slime)) {
            return (entity instanceof Animal
                    || entity instanceof AmbientCreature
                    || entity instanceof Squid
                    || entity instanceof AbstractVillager) && animals.isToggled();
        }

        if (entity instanceof Creeper) return creepers.isToggled();
        if (entity instanceof EnderMan) return endermen.isToggled();
        return entity instanceof Blaze ? blazes.isToggled() : mobs.isToggled();
    }
}
