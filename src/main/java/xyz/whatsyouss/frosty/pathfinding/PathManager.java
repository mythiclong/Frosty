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
 * Portions of this code are derived from rdbtv5 (V5-Client/V5Loader)
 * Copyright (c) 2024 V5-Client
 * Source: https://github.com/V5-Client/V5Loader
 * Licensed under GNU GPL v3.0
 *
 * Original code has been simplified: removed Hypixel-specific features,
 * removed etherwarp landing candidates, simplified avoid zones.
 */

package xyz.whatsyouss.frosty.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import xyz.whatsyouss.frosty.pathfinding.native_.NativePathResult;
import xyz.whatsyouss.frosty.pathfinding.native_.NativePathfinderBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-level pathfinding manager using rdbtv5's native C++ pathfinding engine.
 * Simplified version without Hypixel-specific features.
 */
public class PathManager {

    // Pathfinding constants
    private static final double NON_PRIMARY_START_PENALTY = 250.0;
    private static final double HEURISTIC_WEIGHT = 1.05;
    private static final double FLY_HEURISTIC_WEIGHT = 1.75;

    // Path flags (for debugging and visualization)
    public static final int FLAG_FLUID_FEET = 1 << 0;
    public static final int FLAG_FLUID_HEAD = 1 << 1;
    public static final int FLAG_LOW_HEADROOM = 1 << 2;
    public static final int FLAG_NEAR_EDGE = 1 << 3;
    public static final int FLAG_NEAR_WALL = 1 << 4;
    public static final int FLAG_STEP_UP_NEXT = 1 << 5;
    public static final int FLAG_DROP_NEXT = 1 << 6;
    public static final int FLAG_TIGHT_CORRIDOR = 1 << 7;

    // Current path state
    private static volatile PathSnapshot currentPath = null;
    private static volatile String lastError = null;
    private static volatile boolean isSearching = false;
    private static volatile int searchVariantSeed = 0;

    // Threading
    private static final ExecutorService searchExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Frosty-Pathfinder");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private static Future<?> currentTask = null;
    private static final AtomicInteger searchId = new AtomicInteger(0);

    /**
     * Path snapshot - immutable result of a pathfinding search
     */
    public static class PathSnapshot {
        public final List<BlockPos> points;
        public final List<BlockPos> keyPoints;
        public final boolean isFly;
        public final long timeMs;
        public final int nodesExplored;
        public final double nanosecondsPerNode;
        public final int selectedStartIndex;

        public PathSnapshot(
                List<BlockPos> points,
                List<BlockPos> keyPoints,
                boolean isFly,
                long timeMs,
                int nodesExplored,
                double nanosecondsPerNode,
                int selectedStartIndex
        ) {
            this.points = points;
            this.keyPoints = keyPoints;
            this.isFly = isFly;
            this.timeMs = timeMs;
            this.nodesExplored = nodesExplored;
            this.nanosecondsPerNode = nanosecondsPerNode;
            this.selectedStartIndex = selectedStartIndex;
        }
    }

    /**
     * Find a path from start to end (ground pathfinding)
     */
    public static boolean findPath(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        return findPath(startX, startY, startZ, endX, endY, endZ, 500_000, false);
    }

    /**
     * Find a path from start to end with custom max iterations
     */
    public static boolean findPath(
            int startX, int startY, int startZ,
            int endX, int endY, int endZ,
            int maxIterations,
            boolean isFly
    ) {
        int[] startPoints = new int[]{startX, startY, startZ};
        int[] endPoints = new int[]{endX, endY, endZ};
        return findPathInternal(startPoints, endPoints, maxIterations, isFly);
    }

    /**
     * Find a fly path from start to end
     */
    public static boolean findFlyPath(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        return findPath(startX, startY, startZ, endX, endY, endZ, 500_000, true);
    }

    /**
     * Internal pathfinding implementation
     */
    private static boolean findPathInternal(
            int[] startPoints,
            int[] endPoints,
            int maxIterations,
            boolean isFly
    ) {
        cancelSearch();
        resetResults();

        // Validation
        if (maxIterations <= 0) {
            return fail("maxIterations must be > 0");
        }

        String nativeValidation = validateNativeAvailability();
        if (nativeValidation != null) {
            return fail(nativeValidation);
        }

        // No avoid zones for now (simplified)
        int[] avoidMeta = new int[0];
        double[] avoidPenalty = new double[0];

        int currentId = searchId.incrementAndGet();
        isSearching = true;

        try {
            currentTask = searchExecutor.submit(() -> {
                try {
                    NativePathResult result = NativePathfinderBridge.findPath(
                            startPoints,
                            endPoints,
                            isFly,
                            maxIterations,
                            isFly ? FLY_HEURISTIC_WEIGHT : HEURISTIC_WEIGHT,
                            isFly ? 0.0 : NON_PRIMARY_START_PENALTY,
                            isFly ? 0 : searchVariantSeed,
                            avoidMeta,
                            avoidPenalty
                    );

                    if (searchId.get() != currentId) {
                        return;
                    }

                    if (result != null && !result.isEmpty()) {
                        List<BlockPos> points = result.getPathAsBlockPos();
                        List<BlockPos> keyNodes = result.getKeyPointsAsBlockPos();

                        if (keyNodes.isEmpty()) {
                            keyNodes = points;
                        }

                        currentPath = new PathSnapshot(
                                points,
                                keyNodes,
                                isFly,
                                result.timeMs,
                                result.nodesExplored,
                                result.nanosecondsPerNode,
                                result.selectedStartIndex
                        );
                        lastError = null;
                    } else {
                        currentPath = null;
                        String error = NativePathfinderBridge.getLastError();
                        lastError = error != null ? error : "No path found to destination";
                    }
                } catch (Exception e) {
                    if (searchId.get() == currentId) {
                        lastError = e.getMessage() != null ? e.getMessage() : "Unknown error during native pathfinding";
                        e.printStackTrace();
                    }
                } finally {
                    if (searchId.get() == currentId) {
                        isSearching = false;
                        currentTask = null;
                    }
                }
            });
        } catch (Exception e) {
            if (searchId.get() == currentId) {
                isSearching = false;
                lastError = e.getMessage() != null ? e.getMessage() : "Failed to submit native pathfinding task";
            }
            return false;
        }

        return true;
    }

    /**
     * Cancel the current pathfinding search
     */
    public static void cancelSearch() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            NativePathfinderBridge.cancelSearch();
        }
        searchId.incrementAndGet();
    }

    /**
     * Get the current path (may be null if search is in progress or failed)
     */
    public static PathSnapshot getCurrentPath() {
        return currentPath;
    }

    /**
     * Get the last error message (null if no error)
     */
    public static String getLastError() {
        return lastError;
    }

    /**
     * Check if a search is currently in progress
     */
    public static boolean isSearching() {
        return isSearching;
    }

    /**
     * Wait for the current search to complete (blocking)
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if search completed, false if timed out
     */
    public static boolean waitForSearch(long timeoutMs) {
        if (!isSearching) return true;

        long startTime = System.currentTimeMillis();
        while (isSearching && (System.currentTimeMillis() - startTime) < timeoutMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return !isSearching;
    }

    // Helper methods

    private static void resetResults() {
        lastError = null;
        currentPath = null;
    }

    private static boolean fail(String message) {
        lastError = message;
        return false;
    }

    private static String validateNativeAvailability() {
        if (!NativePathfinderBridge.isAvailable()) {
            String error = NativePathfinderBridge.getLastError();
            return error != null ? error : "Native pathfinder is not available";
        }
        return null;
    }

    /**
     * Initialize the pathfinding system
     * Should be called once at mod initialization
     */
    public static boolean initialize() {
        return NativePathfinderBridge.isAvailable();
    }

    /**
     * Shutdown the pathfinding system
     * Should be called when mod is disabled or game is closing
     */
    public static void shutdown() {
        cancelSearch();
        searchExecutor.shutdown();
    }
}
