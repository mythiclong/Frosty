package xyz.whatsyouss.frosty.modules.impl.render;

import com.mojang.blaze3d.vertex.QuadInstance;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.BufferSource;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Xray extends Module {
    private static final ThreadLocal<Boolean> TRANSLUCENT_LAYER =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final long SCAN_INTERVAL_MS = 1_500L;
    private static final long REBUILD_DEBOUNCE_MS = 200L;
    private static final int SCAN_BLOCK_BUDGET = 65_536;

    public final SliderSetting distance = new SliderSetting("Distance", " blocks", 48.0, 8.0, 96.0, 4.0);
    public final SliderSetting opacity = new SliderSetting("Opacity", 160.0, 10.0, 255.0, 5.0);
    public final ButtonSetting esp = new ButtonSetting("ESP", true);
    public final ButtonSetting outline = new ButtonSetting("Outline", true);
    public final ButtonSetting polishedDiorite = new ButtonSetting("Polished Diorite", true);

    private long lastScan;
    private long rebuildAt;
    private int lastRange = -1;
    private String observedRenderSignature = "";
    private String appliedRenderSignature = "";
    private String lastOreSignature = "";
    private WeakReference<ClientLevel> scannedLevel = new WeakReference<>(null);
    private List<RenderUtils.ColoredBox> targets = List.of();
    private ScanTask scanTask;
    private boolean lastEspEnabled;
    private BufferSource boxBuffer;

    public Xray() {
        super("Xray", "矿物透视", category.Render);
        registerSetting(distance);
        registerSetting(opacity);
        registerSetting(esp);
        registerSetting(outline);
        registerSetting(polishedDiorite);
    }

    @Override
    public String getDesc() {
        return "See selected blocks through walls with configurable transparency and ESP.";
    }

    @Override
    public void onEnable() {
        lastScan = 0L;
        rebuildAt = 0L;
        lastRange = -1;
        observedRenderSignature = settingSignature();
        appliedRenderSignature = observedRenderSignature;
        lastOreSignature = oreSettingSignature();
        scannedLevel.clear();
        scanTask = null;
        lastEspEnabled = esp.isToggled();
        reloadWorldRenderer();
    }

    @Override
    public void onDisable() {
        targets = List.of();
        scannedLevel.clear();
        scanTask = null;
        TRANSLUCENT_LAYER.remove();
        closeBoxBuffer();
        reloadWorldRenderer();
    }

    @Override
    public void onUpdate() {
        if (esp.isToggled() != lastEspEnabled) {
            lastEspEnabled = esp.isToggled();
            targets = List.of();
            scanTask = null;
            lastScan = 0L;
            lastRange = -1;
            if (!lastEspEnabled) {
                closeBoxBuffer();
            }
        }

        String signature = settingSignature();
        String oreSignature = oreSettingSignature();
        long now = System.currentTimeMillis();
        if (!oreSignature.equals(lastOreSignature)) {
            lastOreSignature = oreSignature;
            targets = List.of();
            scanTask = null;
            lastScan = 0L;
        }
        if (!signature.equals(observedRenderSignature)) {
            observedRenderSignature = signature;
            rebuildAt = signature.equals(appliedRenderSignature) ? 0L : now + REBUILD_DEBOUNCE_MS;
        }
        if (rebuildAt != 0L && now >= rebuildAt) {
            appliedRenderSignature = observedRenderSignature;
            rebuildAt = 0L;
            reloadWorldRenderer();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!esp.isToggled() || !Utils.nullCheck()) {
            return;
        }

        if (scannedLevel.get() != mc.level) {
            scannedLevel = new WeakReference<>(mc.level);
            targets = List.of();
            scanTask = null;
            lastScan = 0L;
            lastRange = -1;
        }

        int range = (int) Math.round(distance.getInput());
        long now = System.currentTimeMillis();
        if (scanTask != null && scanTask.range != range) {
            scanTask = null;
        }
        if (scanTask == null && (now - lastScan >= SCAN_INTERVAL_MS || range != lastRange)) {
            scanTask = new ScanTask(mc.level, mc.player.getBlockX(), mc.player.getBlockY(),
                    mc.player.getBlockZ(), range);
        }
        if (scanTask != null && scanTask.step(mc.level, SCAN_BLOCK_BUDGET)) {
            targets = scanTask.results();
            lastScan = now;
            lastRange = scanTask.range;
            scanTask = null;
        }

        if (boxBuffer == null) {
            boxBuffer = BufferSource.reusable();
        }
        RenderUtils.drawBoxes(
                boxBuffer,
                event.getMatrix(),
                targets,
                opacityAlpha() / 255.0f,
                230.0f / 255.0f,
                2.0f,
                true,
                outline.isToggled(),
                false
        );
    }

    public static boolean isActive() {
        return ModuleManager.xray != null && ModuleManager.xray.isEnabled();
    }

    public static boolean shouldShowBlock(BlockState state) {
        return state != null && isActive() && ModuleManager.xray.oreFor(state.getBlock()) != null;
    }

    public static boolean shouldTransparentBlock(BlockState state) {
        return state != null && isActive() && !shouldShowBlock(state);
    }

    public static int transparentAlphaFor(BlockState state) {
        return shouldTransparentBlock(state) ? ModuleManager.xray.opacityAlpha() : -1;
    }

    public static void prepareQuad(BlockState state, QuadInstance quad) {
        TRANSLUCENT_LAYER.set(Boolean.FALSE);
        if (quad == null) {
            return;
        }

        int alpha = transparentAlphaFor(state);
        if (alpha < 0) {
            return;
        }

        TRANSLUCENT_LAYER.set(Boolean.TRUE);
        for (int i = 0; i < 4; i++) {
            quad.setColor(i, alpha << 24 | quad.getColor(i) & 0x00FFFFFF);
        }
    }

    public static boolean useTranslucentLayer() {
        return TRANSLUCENT_LAYER.get();
    }

    public static void clearLayerOverride() {
        TRANSLUCENT_LAYER.set(Boolean.FALSE);
    }

    private Ore oreFor(Block block) {
        if (block == Blocks.POLISHED_DIORITE) {
            return polishedDiorite.isToggled() ? Ore.POLISHED_DIORITE : null;
        }
        return null;
    }

    private int opacityAlpha() {
        return Math.clamp((int) Math.round(opacity.getInput()), 10, 255);
    }

    private String settingSignature() {
        return opacityAlpha() + ":" + oreSettingSignature();
    }

    private String oreSettingSignature() {
        return String.valueOf(polishedDiorite.isToggled());
    }

    private void reloadWorldRenderer() {
        if (mc.level != null && mc.levelRenderer != null) {
            mc.levelRenderer.invalidateCompiledGeometry(
                    mc.level,
                    mc.options,
                    mc.gameRenderer.mainCamera(),
                    mc.getBlockColors()
            );
        }
    }

    private void closeBoxBuffer() {
        if (boxBuffer != null) {
            boxBuffer.close();
            boxBuffer = null;
        }
    }

    private final class ScanTask {
        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final int range;
        private final int rangeSq;
        private final int minZ;
        private final int maxZ;
        private final int worldMinY;
        private final int worldMaxY;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private final ArrayList<RenderUtils.ColoredBox> found = new ArrayList<>();

        private int nextX;
        private int nextZ;
        private int columnX;
        private int columnZ;
        private int nextY;
        private int columnMaxY;
        private boolean columnReady;

        private ScanTask(ClientLevel level, int centerX, int centerY, int centerZ, int range) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.range = range;
            this.rangeSq = range * range;
            this.nextX = centerX - range;
            this.minZ = centerZ - range;
            this.maxZ = centerZ + range;
            this.nextZ = minZ;
            this.worldMinY = level.getMinY();
            this.worldMaxY = level.getMaxY() - 1;
        }

        private boolean step(ClientLevel level, int budget) {
            int checked = 0;
            while (checked < budget) {
                if (!columnReady && !prepareNextColumn(level)) {
                    return true;
                }

                cursor.set(columnX, nextY, columnZ);
                Ore ore = oreFor(level.getBlockState(cursor).getBlock());
                if (ore != null) {
                    found.add(new RenderUtils.ColoredBox(
                            new AABB(columnX, nextY, columnZ,
                                    columnX + 1.0, nextY + 1.0, columnZ + 1.0),
                            ore.color
                    ));
                }

                checked++;
                nextY++;
                if (nextY > columnMaxY) {
                    columnReady = false;
                }
            }
            return false;
        }

        private boolean prepareNextColumn(ClientLevel level) {
            int maxX = centerX + range;
            while (nextX <= maxX) {
                while (nextZ <= maxZ) {
                    int z = nextZ++;
                    int dx = nextX - centerX;
                    int dz = z - centerZ;
                    int horizontalSq = dx * dx + dz * dz;
                    if (horizontalSq > rangeSq || !level.hasChunk(nextX >> 4, z >> 4)) {
                        continue;
                    }

                    int verticalRange = (int) Math.sqrt(rangeSq - horizontalSq);
                    int minY = Math.max(worldMinY, centerY - verticalRange);
                    int maxY = Math.min(worldMaxY, centerY + verticalRange);
                    if (minY > maxY) {
                        continue;
                    }

                    columnX = nextX;
                    columnZ = z;
                    nextY = minY;
                    columnMaxY = maxY;
                    columnReady = true;
                    return true;
                }

                nextX++;
                nextZ = minZ;
            }
            return false;
        }

        private List<RenderUtils.ColoredBox> results() {
            return List.copyOf(found);
        }
    }

    private enum Ore {
        POLISHED_DIORITE(new Color(0xD8CDC7));

        private final Color color;

        Ore(Color color) {
            this.color = color;
        }
    }
}
