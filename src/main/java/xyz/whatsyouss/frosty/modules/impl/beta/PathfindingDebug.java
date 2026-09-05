/*
 * This file is part of Frosty Client
 * Copyright (C) 2024 mythiclong
 */

package xyz.whatsyouss.frosty.modules.impl.beta;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.pathfinding.PathManager;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.*;
import java.util.List;

public class PathfindingDebug extends Module {

    private boolean loadAttempted = false;
    private boolean loadSuccess = false;
    private String loadMessage = "";
    private BlockPos targetPos = null;

    public PathfindingDebug() {
        super("PathfindingDebug", "寻路调试", category.Beta);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();

        if (!loadAttempted) {
            loadAttempted = true;
            loadSuccess = PathManager.initialize();

            if (loadSuccess) {
                loadMessage = "§a[Pathfinding] Native library loaded successfully!";
                Utils.addChatMessage(loadMessage);
                Utils.addChatMessage("§7使用示例: 看向一个方块后重新开启此模块来寻路");
            } else {
                String error = PathManager.getLastError();
                loadMessage = "§c[Pathfinding] Failed to load native library:\n" + error;
                Utils.addChatMessage(loadMessage);
            }
        } else {
            if (loadSuccess) {
                Utils.addChatMessage("§a[Pathfinding] Native library already loaded");

                // Test pathfinding to where player is looking
                if (mc.player != null && mc.level != null) {
                    var hitResult = mc.hitResult;
                    if (hitResult != null && hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        BlockPos target = ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();
                        BlockPos start = mc.player.blockPosition();

                        targetPos = target;

                        Utils.addChatMessage("§e[Pathfinding] Finding path from " +
                            start.getX() + "," + start.getY() + "," + start.getZ() + " to " +
                            target.getX() + "," + target.getY() + "," + target.getZ());

                        boolean started = PathManager.findPath(
                            start.getX(), start.getY(), start.getZ(),
                            target.getX(), target.getY(), target.getZ()
                        );

                        if (started) {
                            Utils.addChatMessage("§7Pathfinding started...");

                            // Wait for result (async)
                            new Thread(() -> {
                                if (PathManager.waitForSearch(5000)) {
                                    PathManager.PathSnapshot path = PathManager.getCurrentPath();
                                    if (path != null) {
                                        Utils.addChatMessage("§a[Pathfinding] Path found!");
                                        Utils.addChatMessage("§7- Nodes: " + path.points.size());
                                        Utils.addChatMessage("§7- Time: " + path.timeMs + "ms");
                                        Utils.addChatMessage("§7- Explored: " + path.nodesExplored + " nodes");
                                        Utils.addChatMessage(String.format("§7- Speed: %.2f ns/node", path.nanosecondsPerNode));
                                    } else {
                                        String error = PathManager.getLastError();
                                        Utils.addChatMessage("§c[Pathfinding] Failed: " + error);
                                    }
                                } else {
                                    Utils.addChatMessage("§c[Pathfinding] Timeout (>5s)");
                                }
                            }).start();
                        } else {
                            Utils.addChatMessage("§c[Pathfinding] Failed to start: " + PathManager.getLastError());
                        }
                    } else {
                        Utils.addChatMessage("§c[Pathfinding] 请看向一个方块");
                    }
                }
            } else {
                Utils.addChatMessage(loadMessage);
            }
        }
    }

    @Override
    public void onDisable() {
        PathManager.cancelSearch();
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;
        // Path is visualized in onRender3D
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) return;

        PathManager.PathSnapshot path = PathManager.getCurrentPath();
        if (path == null) return;

        PoseStack poseStack = event.getMatrix();

        // Render full path (cyan lines)
        List<BlockPos> points = path.points;
        for (int i = 0; i < points.size() - 1; i++) {
            BlockPos from = points.get(i);
            BlockPos to = points.get(i + 1);

            Vec3 fromVec = new Vec3(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5);
            Vec3 toVec = new Vec3(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);

            RenderUtils.drawLine3D(poseStack, fromVec, toVec, Color.CYAN, 2.0f, false);
        }

        // Render key points (yellow boxes)
        List<BlockPos> keyPoints = path.keyPoints;
        for (BlockPos keyPoint : keyPoints) {
            RenderUtils.drawBlockOutline(poseStack, keyPoint, Color.YELLOW, 2.0f, false);
        }

        // Render target (red)
        if (targetPos != null) {
            RenderUtils.drawBlockOutline(poseStack, targetPos, Color.RED, 3.0f, false);
        }
    }

    @Override
    public String getDesc() {
        return "测试rdbtv5原生寻路引擎。开启后尝试加载JNI库。看向方块后重新开启来测试寻路。";
    }
}
