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
 */

package xyz.whatsyouss.frosty.pathfinding.native_;

public class NativePathfinderBridge {

    private static volatile String lastError = null;

    public static boolean isAvailable() {
        return NativePathfinderJNI.isAvailable();
    }

    public static String getLastError() {
        if (lastError != null) return lastError;
        return NativePathfinderJNI.getLoadError();
    }

    private static void setUnavailableError() {
        String loadError = NativePathfinderJNI.getLoadError();
        lastError = loadError != null ? loadError : "Native pathfinder unavailable";
    }

    private static void setError(Throwable t) {
        lastError = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    public static void setWorld(String worldKey, int minY, int maxY) {
        if (!isAvailable()) {
            setUnavailableError();
            return;
        }

        try {
            NativePathfinderJNI.setWorld(worldKey, minY, maxY);
            lastError = null;
        } catch (Throwable t) {
            setError(t);
        }
    }

    public static void clearWorld() {
        if (!isAvailable()) {
            setUnavailableError();
            return;
        }

        try {
            NativePathfinderJNI.clearWorld();
            lastError = null;
        } catch (Throwable t) {
            setError(t);
        }
    }

    public static void upsertChunk(
            int chunkX,
            int chunkZ,
            int minY,
            int maxY,
            long sectionMask,
            short[] sectionFlags
    ) {
        if (!isAvailable()) {
            setUnavailableError();
            return;
        }

        try {
            NativePathfinderJNI.upsertChunk(chunkX, chunkZ, minY, maxY, sectionMask, sectionFlags);
            lastError = null;
        } catch (Throwable t) {
            setError(t);
        }
    }

    public static void applyBlockUpdates(int[] updates) {
        if (updates == null || updates.length == 0) return;

        if (!isAvailable()) {
            setUnavailableError();
            return;
        }

        try {
            NativePathfinderJNI.applyBlockUpdates(updates);
            lastError = null;
        } catch (Throwable t) {
            setError(t);
        }
    }

    public static NativePathResult findPath(
            int[] startPoints,
            int[] endPoints,
            boolean isFly,
            int maxIterations,
            double heuristicWeight,
            double nonPrimaryStartPenalty,
            int moveOrderOffset,
            int[] avoidMeta,
            double[] avoidPenalty
    ) {
        if (!isAvailable()) {
            setUnavailableError();
            return null;
        }

        try {
            NativePathResult result = NativePathfinderJNI.findPath(
                    startPoints,
                    endPoints,
                    isFly,
                    maxIterations,
                    heuristicWeight,
                    nonPrimaryStartPenalty,
                    moveOrderOffset,
                    avoidMeta,
                    avoidPenalty
            );

            if (result == null) {
                lastError = "Native pathfinder returned no path";
            } else {
                lastError = null;
            }

            return result;
        } catch (Throwable t) {
            setError(t);
            return null;
        }
    }

    public static NativeEtherwarpResult findEtherwarpPath(
            int goalX,
            int goalY,
            int goalZ,
            double startEyeX,
            double startEyeY,
            double startEyeZ,
            int maxIterations,
            int threadCount,
            double yawStep,
            double pitchStep,
            double newNodeCost,
            double heuristicWeight,
            double rayLength,
            double rewireEpsilon,
            double eyeHeight
    ) {
        if (!isAvailable()) {
            setUnavailableError();
            return null;
        }

        try {
            NativeEtherwarpResult result = NativePathfinderJNI.findEtherwarpPath(
                    goalX, goalY, goalZ,
                    startEyeX, startEyeY, startEyeZ,
                    maxIterations,
                    threadCount,
                    yawStep,
                    pitchStep,
                    newNodeCost,
                    heuristicWeight,
                    rayLength,
                    rewireEpsilon,
                    eyeHeight
            );

            if (result == null) {
                lastError = "Native etherwarp pathfinder returned no path";
            } else {
                lastError = null;
            }

            return result;
        } catch (Throwable t) {
            setError(t);
            return null;
        }
    }

    public static void cancelSearch() {
        if (!isAvailable()) return;

        try {
            NativePathfinderJNI.cancelSearch();
        } catch (Throwable ignored) {
        }
    }
}
