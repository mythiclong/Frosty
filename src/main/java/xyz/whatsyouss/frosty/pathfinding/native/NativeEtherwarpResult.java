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

public class NativeEtherwarpResult {
    public final int[] points;
    public final float[] angles;
    public final long timeMs;
    public final int nodesExplored;
    public final double nanosecondsPerNode;

    public NativeEtherwarpResult(
            int[] points,
            float[] angles,
            long timeMs,
            int nodesExplored,
            double nanosecondsPerNode
    ) {
        this.points = points;
        this.angles = angles;
        this.timeMs = timeMs;
        this.nodesExplored = nodesExplored;
        this.nanosecondsPerNode = nanosecondsPerNode;
    }

    public List<BlockPos> getPathAsBlockPos() {
        List<BlockPos> path = new ArrayList<>();
        for (int i = 0; i < points.length; i += 3) {
            path.add(new BlockPos(points[i], points[i + 1], points[i + 2]));
        }
        return path;
    }

    public static class AnglePair {
        public final float yaw;
        public final float pitch;

        public AnglePair(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public List<AnglePair> getAnglesAsList() {
        List<AnglePair> angleList = new ArrayList<>();
        for (int i = 0; i < angles.length; i += 2) {
            angleList.add(new AnglePair(angles[i], angles[i + 1]));
        }
        return angleList;
    }

    public boolean isEmpty() {
        return points == null || points.length == 0;
    }

    public int getPathLength() {
        return points.length / 3;
    }
}
