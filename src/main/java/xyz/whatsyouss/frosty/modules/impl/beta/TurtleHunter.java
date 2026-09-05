/*
 * This file is part of Frosty Client
 * Copyright (C) 2024 mythiclong
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ---
 *
 * Portions of this code are derived from Booter Client
 * Original concept from booter-client-1.0.0.jar
 * Reimplemented from bytecode analysis for educational purposes
 */

package xyz.whatsyouss.frosty.modules.impl.beta;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.mixin.InventoryAccessor;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RotationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurtleHunter extends Module {

    // Settings
    private final SliderSetting range = new SliderSetting("范围", 1, 32, 20, 1);
    private final SliderSetting minDistance = new SliderSetting("最小距离", 1, 10, 2, 0.5);
    private final ButtonSetting autoWeapon = new ButtonSetting("自动选择武器", true);
    private final ButtonSetting onlyBabies = new ButtonSetting("仅攻击幼体", false);

    // State
    private enum State {
        IDLE, SCANNING, CHASING, ATTACKING
    }

    private State state = State.IDLE;
    private Entity target = null;
    private int scanCooldown = 0;
    private int attackTicks = 0;
    private final Map<Integer, Integer> blacklist = new HashMap<>();

    // Constants
    private static final int SCAN_INTERVAL = 10;
    private static final int ATTACK_TIMEOUT_TICKS = 150;
    private static final int BLACKLIST_TICKS = 200;
    private static final float AIM_TOLERANCE = 15.0f;

    public TurtleHunter() {
        super("TurtleHunter", "海龟猎手", category.Beta);
        this.registerSetting(range);
        this.registerSetting(minDistance);
        this.registerSetting(autoWeapon);
        this.registerSetting(onlyBabies);
    }

    @Override
    public void onEnable() {
        state = State.SCANNING;
        target = null;
        scanCooldown = 0;
        attackTicks = 0;
        blacklist.clear();
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        target = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }

        // Update blacklist
        tickBlacklist();

        // Auto select weapon
        if (autoWeapon.isToggled()) {
            selectBestWeapon();
        }

        // Validate current target
        if (!isValidTarget(target)) {
            clearTarget(mc);
            state = State.SCANNING;
        }

        // Find new target if needed
        if (target == null) {
            if (--scanCooldown <= 0) {
                scanCooldown = SCAN_INTERVAL;
                target = findTarget(mc);
                if (target != null) {
                    attackTicks = 0;
                    state = State.CHASING;
                }
            }
            return;
        }

        // Distance check
        double distanceSq = mc.player.distanceToSqr(target);
        double minDist = minDistance.getInput();
        double maxDist = range.getInput();

        // Too close - back away
        if (distanceSq < minDist * minDist) {
            backAway(mc);
            return;
        }

        // In range - attack
        if (distanceSq <= maxDist * maxDist) {
            attackTarget(mc);
        } else {
            // Out of range - chase
            chaseTarget(mc);
        }
    }

    private Entity findTarget(Minecraft mc) {
        Vec3 playerPos = mc.player.position();
        double scanRadius = range.getInput();

        AABB searchBox = new AABB(
            playerPos.x - scanRadius, playerPos.y - scanRadius, playerPos.z - scanRadius,
            playerPos.x + scanRadius, playerPos.y + scanRadius, playerPos.z + scanRadius
        );

        List<Entity> entities = mc.level.getEntities(mc.player, searchBox, this::isTurtleTarget);

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            // Skip blacklisted
            if (blacklist.containsKey(entity.getId())) {
                continue;
            }

            double dist = mc.player.distanceToSqr(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }

        return closest;
    }

    private boolean isTurtleTarget(Entity entity) {
        if (entity.getType() != EntityType.TURTLE) {
            return false;
        }

        // Filter by baby if setting enabled
        if (onlyBabies.isToggled() && entity instanceof AgeableMob ageableMob) {
            return ageableMob.isBaby();
        }

        return true;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        if (!isTurtleTarget(entity)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        double dist = mc.player.distanceToSqr(entity);
        double maxDist = range.getInput() * 1.5; // Allow some overshoot

        return dist <= maxDist * maxDist;
    }

    private void attackTarget(Minecraft mc) {
        state = State.ATTACKING;

        // Rotate to target
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        float[] rotation = RotationUtils.getYawPitchTo(playerPos, targetPos);

        mc.player.setYRot(rotation[0]);
        mc.player.setXRot(rotation[1]);
        mc.player.yRotO = rotation[0];
        mc.player.xRotO = rotation[1];

        // Check if aimed correctly
        if (isAimedAt(mc, target)) {
            // Attack
            if (mc.gameMode != null) {
                mc.gameMode.attack(mc.player, target);
            }
            attackTicks++;

            // Timeout check
            if (attackTicks > ATTACK_TIMEOUT_TICKS) {
                blacklist.put(target.getId(), BLACKLIST_TICKS);
                clearTarget(mc);
            }
        }
    }

    private void chaseTarget(Minecraft mc) {
        state = State.CHASING;

        // Simple chase: look at target and move forward
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = target.position();
        float[] rotation = RotationUtils.getYawPitchTo(playerPos, targetPos);

        mc.player.setYRot(rotation[0]);
        mc.player.setXRot(rotation[1]);

        // Move forward
        mc.options.keyUp.setDown(true);
    }

    private void backAway(Minecraft mc) {
        // Look at target but move backward
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = target.position();
        float[] rotation = RotationUtils.getYawPitchTo(playerPos, targetPos);

        mc.player.setYRot(rotation[0]);
        mc.player.setXRot(rotation[1]);

        // Move backward
        mc.options.keyDown.setDown(true);
    }

    private boolean isAimedAt(Minecraft mc, Entity entity) {
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = entity.position().add(0, entity.getEyeHeight() * 0.5, 0);
        float[] neededRot = RotationUtils.getYawPitchTo(playerPos, targetPos);

        float yawDiff = Math.abs(angleDifference(mc.player.getYRot(), neededRot[0]));
        float pitchDiff = Math.abs(angleDifference(mc.player.getXRot(), neededRot[1]));

        return yawDiff < AIM_TOLERANCE && pitchDiff < AIM_TOLERANCE;
    }

    private float angleDifference(float a, float b) {
        float diff = a - b;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;
        return diff;
    }

    private void clearTarget(Minecraft mc) {
        target = null;
        attackTicks = 0;
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }

    private void selectBestWeapon() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int bestSlot = -1;
        float bestDamage = 0;

        // Find best weapon in hotbar
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            // Simple damage check
            var item = stack.getItem();
            float damage = 1.0f; // Default

            if (item.toString().contains("sword")) {
                damage = 7.0f;
            } else if (item.toString().contains("axe")) {
                damage = 9.0f;
            } else if (item.toString().contains("trident")) {
                damage = 9.0f;
            }

            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }

        if (bestSlot >= 0) {
            ((InventoryAccessor) mc.player.getInventory()).setSelected(bestSlot);
        }
    }

    private void tickBlacklist() {
        blacklist.entrySet().removeIf(entry -> {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                return true;
            }
            entry.setValue(remaining);
            return false;
        });
    }

    @Override
    public String getDesc() {
        String targetType = onlyBabies.isToggled() ? "幼体海龟" : "所有海龟";
        return "自动狩猎附近的" + targetType + "。状态: " + state.name();
    }
}
