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

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class NativePathResult {
    public final int[] points;
    public final int[] keyPoints;
    public final int[] pathFlags;
    public final int[] keyNodeFlags;
    public final int[] keyNodeMetrics;
    public final String signatureHex;
    public final long timeMs;
    public final int nodesExplored;
    public final double nanosecondsPerNode;
    public final int selectedStartIndex;

    public NativePathResult(
            int[] points,
            int[] keyPoints,
            int[] pathFlags,
            int[] keyNodeFlags,
            int[] keyNodeMetrics,
            String signatureHex,
            long timeMs,
            int nodesExplored,
            double nanosecondsPerNode,
            int selectedStartIndex
    ) {
        this.points = points;
        this.keyPoints = keyPoints;
        this.pathFlags = pathFlags;
        this.keyNodeFlags = keyNodeFlags;
        this.keyNodeMetrics = keyNodeMetrics;
        this.signatureHex = signatureHex;
        this.timeMs = timeMs;
        this.nodesExplored = nodesExplored;
        this.nanosecondsPerNode = nanosecondsPerNode;
        this.selectedStartIndex = selectedStartIndex;
    }

    public List<BlockPos> getPathAsBlockPos() {
        List<BlockPos> path = new ArrayList<>();
        for (int i = 0; i < points.length; i += 3) {
            path.add(new BlockPos(points[i], points[i + 1], points[i + 2]));
        }
        return path;
    }

    public List<BlockPos> getKeyPointsAsBlockPos() {
        List<BlockPos> keyPath = new ArrayList<>();
        for (int i = 0; i < keyPoints.length; i += 3) {
            keyPath.add(new BlockPos(keyPoints[i], keyPoints[i + 1], keyPoints[i + 2]));
        }
        return keyPath;
    }

    public boolean isEmpty() {
        return points == null || points.length == 0;
    }

    public int getPathLength() {
        return points.length / 3;
    }
}
