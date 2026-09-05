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

public class NativeVoxelFlags {
    public static final int PASSABLE = 1 << 0;
    public static final int SOLID = 1 << 1;
    public static final int PASSABLE_FLY = 1 << 2;
    public static final int BLOCKING_WALL = 1 << 3;
    public static final int FLUID = 1 << 4;
    public static final int SLAB_BOTTOM = 1 << 5;
    public static final int SLAB_TOP = 1 << 6;
    public static final int FENCE_LIKE = 1 << 7;
    public static final int STAIRS_BOTTOM = 1 << 8;
    public static final int CARPET_LIKE = 1 << 9;
    public static final int ETHER_PASSABLE = 1 << 10;
    public static final int ETHER_TELEPORT_CLEAR = 1 << 11;
    public static final int ETHER_FEET_BLOCKER = 1 << 12;
    public static final int ETHER_FAKE_FULL_BLOCKER = 1 << 13;

    private NativeVoxelFlags() {}
}
