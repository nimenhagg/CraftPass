package com.craftpass.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.UUID;

/**
 * Automatically locates player data files across all Spigot/Paper directory layouts:
 * - Standard legacy/Spigot layout: world/playerdata/<uuid>.dat
 * - Modern Paper 26.x layout: world/players/data/<uuid>.dat
 */
public class PlayerDataLocator {

    public static File findPlayerDataFile(UUID uuid) {
        World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        File worldContainer = Bukkit.getWorldContainer();

        // 1. Try default world folder
        if (defaultWorld != null) {
            File worldDir = defaultWorld.getWorldFolder();
            File f1 = new File(worldDir, "players/data/" + uuid + ".dat");
            if (f1.exists()) return f1;

            File f2 = new File(worldDir, "playerdata/" + uuid + ".dat");
            if (f2.exists()) return f2;
        }

        // 2. Try world container / "world" folder
        File rootWorld = new File(worldContainer, "world");
        if (rootWorld.exists()) {
            File f1 = new File(rootWorld, "players/data/" + uuid + ".dat");
            if (f1.exists()) return f1;

            File f2 = new File(rootWorld, "playerdata/" + uuid + ".dat");
            if (f2.exists()) return f2;
        }

        // Fallback to first existing directory or standard path
        File standardDir = new File(rootWorld, "playerdata");
        File modernDir = new File(rootWorld, "players/data");
        if (modernDir.exists()) {
            return new File(modernDir, uuid + ".dat");
        }
        return new File(standardDir, uuid + ".dat");
    }

    public static File findStatsFile(UUID uuid) {
        World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        File worldDir = defaultWorld != null ? defaultWorld.getWorldFolder() : new File(Bukkit.getWorldContainer(), "world");

        File f1 = new File(worldDir, "players/stats/" + uuid + ".json");
        if (f1.exists()) return f1;

        File f2 = new File(worldDir, "stats/" + uuid + ".json");
        if (f2.exists()) return f2;

        return f1.getParentFile().exists() ? f1 : f2;
    }

    public static File findAdvancementsFile(UUID uuid) {
        World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        File worldDir = defaultWorld != null ? defaultWorld.getWorldFolder() : new File(Bukkit.getWorldContainer(), "world");

        File f1 = new File(worldDir, "players/advancements/" + uuid + ".json");
        if (f1.exists()) return f1;

        File f2 = new File(worldDir, "advancements/" + uuid + ".json");
        if (f2.exists()) return f2;

        return f1.getParentFile().exists() ? f1 : f2;
    }
}
