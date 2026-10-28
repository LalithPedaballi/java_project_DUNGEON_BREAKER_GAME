package com.riftarena.ui.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache and loader for hero character artwork and animated sprite frames.
 *
 * Provides fast access to transparent PNG cutouts and animation frames
 * with zero external dependencies and graceful fallback handling.
 */
public final class HeroSpriteLoader {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private HeroSpriteLoader() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Loads an image from the specified path, caching the result in memory.
     * Returns null if the file does not exist or fails to load.
     */
    public static BufferedImage load(String path) {
        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }
        BufferedImage img = null;
        try {
            File file = new File(path);
            if (file.exists() && file.canRead()) {
                img = ImageIO.read(file);
            }
        } catch (Exception ignored) {
        }
        CACHE.put(path, img);
        return img;
    }

    /**
     * Loads the single clean full-body cut-out that represents a champion. Poses are
     * animated at draw time, so one high quality illustration is all a fighter needs.
     */
    public static BufferedImage getHeroBody(String heroName) {
        String key = heroName.toLowerCase();
        BufferedImage img = load("assets/heroes/" + key + ".png");
        if (img == null) {
            img = load("assets/heroes/" + key + "/idle.png");
        }
        return img;
    }

    /**
     * Loads a specific animation frame for a hero from disk, or falls back to the full body.
     */
    public static BufferedImage getHeroFrame(String heroName, String frameName) {
        String path = "assets/heroes/" + heroName.toLowerCase() + "/" + frameName.toLowerCase() + ".png";
        BufferedImage img = load(path);
        if (img == null) {
            img = getHeroBody(heroName);
        }
        return img;
    }

    /**
     * Helper to load the full-body showcase portrait for hero selection.
     */
    public static BufferedImage getHeroPortrait(String heroName) {
        return getHeroBody(heroName);
    }

    /**
     * Helper to load enemy artwork cutout.
     */
    public static BufferedImage getEnemySprite(String enemyName) {
        String key = enemyName.toLowerCase().replace(" ", "_");
        if (key.contains("warden") || key.contains("boss") || key.contains("malakor")) {
            key = "malakor";
        } else if (key.contains("soulweaver") || key.contains("morgatha")) {
            key = "morgatha";
        } else if (key.contains("skirmish")) {
            key = "void_skirmisher";
        } else if (key.contains("cultist")) {
            key = "rift_cultist";
        } else if (key.contains("grunt") || key.contains("iron")) {
            key = "ironclad_grunt";
        } else if (key.contains("thrall") || key.contains("spawn") || key.contains("skulker")) {
            key = "rift_thrall";
        }

        BufferedImage img = load("assets/enemies/" + key + ".png");
        if (img == null) {
            img = load("assets/enemies/rift_thrall.png");
        }
        return img;
    }
}

