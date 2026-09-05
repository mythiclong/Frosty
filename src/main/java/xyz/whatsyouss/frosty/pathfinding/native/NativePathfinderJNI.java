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
 * Original code has been adapted for integration with Frosty.
 */

package xyz.whatsyouss.frosty.pathfinding.native_;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NativePathfinderJNI {

    private static final String LIB_BASE = "V5PathJNI";

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static volatile String loadError = null;

    public static synchronized boolean initialize() {
        if (initialized) return available;

        List<String> candidates = nativeResourceCandidates();
        List<String> errors = new ArrayList<>();

        for (String resourcePath : candidates) {
            try {
                if (loadNativeFromResource(resourcePath)) {
                    available = true;
                    loadError = null;
                    initialized = true;
                    return true;
                }
                errors.add(resourcePath + ": not found in jar");
            } catch (Throwable t) {
                errors.add(resourcePath + ": " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
            }
        }

        available = false;
        StringBuilder sb = new StringBuilder("Failed to load native pathfinder");
        sb.append(" (OS: ").append(System.getProperty("os.name"));
        sb.append(", arch: ").append(System.getProperty("os.arch")).append(")");
        sb.append(". Tried:\n");
        for (String error : errors) {
            sb.append("  - ").append(error).append("\n");
        }
        loadError = sb.toString().trim();
        initialized = true;
        return false;
    }

    private static boolean loadNativeFromResource(String resourcePath) throws Exception {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String ext = extensionForOs(os);

        InputStream input = NativePathfinderJNI.class.getResourceAsStream(resourcePath);
        if (input == null) return false;

        File tempFile = Files.createTempFile(LIB_BASE, ext).toFile();
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            input.close();
        }

        System.load(tempFile.getAbsolutePath());

        if (!initNative()) {
            throw new IllegalStateException("initNative() returned false");
        }

        return true;
    }

    private static List<String> nativeResourceCandidates() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = normalizeArch(System.getProperty("os.arch"));
        String ext = extensionForOs(os);
        String lib = LIB_BASE + ext;

        List<String> platformPaths = new ArrayList<>();

        if (os.contains("win")) {
            platformPaths.add("windows/" + arch);
        } else if (os.contains("linux")) {
            if ("x86_64".equals(arch)) {
                platformPaths.add("linux/x86_64");
            } else {
                platformPaths.add("linux/" + arch);
                platformPaths.add("linux/x86_64");
            }
        } else if (os.contains("mac")) {
            if ("arm64".equals(arch)) {
                platformPaths.add("macos/arm64");
                platformPaths.add("macos/universal");
            } else if ("x86_64".equals(arch)) {
                platformPaths.add("macos/x86_64");
                platformPaths.add("macos/universal");
            } else {
                platformPaths.add("macos/" + arch);
            }
        }

        List<String> candidates = new ArrayList<>();
        for (String path : platformPaths) {
            candidates.add("/natives/" + path + "/" + lib);
        }

        return candidates;
    }

    private static String extensionForOs(String os) {
        if (os.contains("win")) return ".dll";
        if (os.contains("mac")) return ".dylib";
        if (os.contains("linux")) return ".so";
        throw new IllegalStateException("Unsupported OS for native pathfinder: " + os);
    }

    private static String normalizeArch(String arch) {
        String lower = arch.toLowerCase(Locale.ROOT);
        if (lower.equals("amd64") || lower.equals("x86_64")) return "x86_64";
        if (lower.equals("aarch64") || lower.equals("arm64")) return "arm64";
        return lower;
    }

    public static boolean isAvailable() {
        initialize();
        return available;
    }

    public static String getLoadError() {
        return loadError;
    }

    // Native methods
    public static native boolean initNative();

    public static native void setWorld(String worldKey, int minY, int maxY);

    public static native void clearWorld();

    public static native void upsertChunk(
            int chunkX,
            int chunkZ,
            int minY,
            int maxY,
            long sectionMask,
            short[] sectionFlags
    );

    public static native void applyBlockUpdates(int[] updates);

    public static native NativePathResult findPath(
            int[] startPoints,
            int[] endPoints,
            boolean isFly,
            int maxIterations,
            double heuristicWeight,
            double nonPrimaryStartPenalty,
            int moveOrderOffset,
            int[] avoidMeta,
            double[] avoidPenalty
    );

    public static native NativeEtherwarpResult findEtherwarpPath(
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
    );

    public static native void cancelSearch();
}
