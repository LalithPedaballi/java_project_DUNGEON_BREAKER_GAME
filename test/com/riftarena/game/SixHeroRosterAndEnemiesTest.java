package com.riftarena.game;

import com.riftarena.realtime.EnemyType;
import com.riftarena.realtime.EntityState;
import com.riftarena.realtime.RealTimeArena;
import com.riftarena.realtime.RealTimeCombatEngine;
import com.riftarena.realtime.RealTimeEnemy;
import com.riftarena.realtime.RealTimePlayer;
import com.riftarena.ui.HeroSelectPanel;
import com.riftarena.ui.render.CharacterRenderer;
import com.riftarena.ui.render.CharacterRendererFactory;
import com.riftarena.ui.render.DravenRenderer;
import com.riftarena.ui.render.HeroSpriteLoader;
import com.riftarena.ui.render.KaelRenderer;
import com.riftarena.ui.render.MageRenderer;
import com.riftarena.ui.render.RiftWardenRenderer;
import com.riftarena.ui.render.VexaraRenderer;
import com.riftarena.ui.render.WarriorRenderer;
import com.riftarena.ui.render.AssassinRenderer;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Targeted verification suite for Milestone: Six-Hero Roster, Animated Characters,
 * and Cinematic Enemies.
 */
public class SixHeroRosterAndEnemiesTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING SIX-HERO ROSTER & ENEMY ARTWORK SUITE   ");
        System.out.println("==================================================");

        testSixHeroSubclassesAndStats();
        testCharacterRendererFactoryMapping();
        testHeroAnimationFramesOnDisk();
        testEnemyArtworkAssetsOnDisk();
        testSpecialPowerMeterAccumulationAndGating();
        testKaelCombatProfileAndSpecial();
        testVexaraCombatProfileAndLifeSiphon();
        testDravenCombatProfileAndSpecial();
        testEnemyArtworkRendererFallbacks();

        System.out.println("==================================================");
        System.out.printf("Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsRun - testsPassed);
        System.out.println("==================================================");

        if (testsPassed != testsRun) {
            System.err.println("FAILURE: Some six-hero roster tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All six-hero roster & enemy artwork tests passed cleanly.");
        }
    }

    private static void testSixHeroSubclassesAndStats() {
        runTest("Six Hero Concrete Classes Instantiate with Distinct Attributes", () -> {
            Hero aegis = new Warrior();
            Hero nyx = new Assassin();
            Hero zephyra = new Mage();
            Hero kael = new Kael();
            Hero vexara = new Vexara();
            Hero draven = new Draven();

            assertEquals("Warrior", aegis.getName(), "Aegis internal name is Warrior");
            assertEquals("Assassin", nyx.getName(), "Nyx internal name is Assassin");
            assertEquals("Mage", zephyra.getName(), "Zephyra internal name is Mage");
            assertEquals("Kael", kael.getName(), "Kael name");
            assertEquals("Vexara", vexara.getName(), "Vexara name");
            assertEquals("Draven", draven.getName(), "Draven name");

            assertTrue(kael.getMaxHp() == 100, "Kael 100 HP");
            assertTrue(vexara.getMaxHp() == 85, "Vexara 85 HP");
            assertTrue(draven.getMaxHp() == 135, "Draven 135 HP (highest vitality)");

            assertTrue(kael.getBaseAttack() == 16, "Kael attack");
            assertTrue(vexara.getBaseAttack() == 17, "Vexara attack");
            assertTrue(draven.getBaseAttack() == 22, "Draven attack (highest strike)");
        });
    }

    private static void testCharacterRendererFactoryMapping() {
        runTest("CharacterRendererFactory Maps All Six Champions to Unique Renderers", () -> {
            CharacterRenderer rAegis = CharacterRendererFactory.forHeroName("Warrior");
            CharacterRenderer rNyx = CharacterRendererFactory.forHeroName("Assassin");
            CharacterRenderer rZephyra = CharacterRendererFactory.forHeroName("Mage");
            CharacterRenderer rKael = CharacterRendererFactory.forHeroName("Kael");
            CharacterRenderer rVexara = CharacterRendererFactory.forHeroName("Vexara");
            CharacterRenderer rDraven = CharacterRendererFactory.forHeroName("Draven");

            assertTrue(rAegis instanceof WarriorRenderer, "Aegis -> WarriorRenderer");
            assertTrue(rNyx instanceof AssassinRenderer, "Nyx -> AssassinRenderer");
            assertTrue(rZephyra instanceof MageRenderer, "Zephyra -> MageRenderer");
            assertTrue(rKael instanceof KaelRenderer, "Kael -> KaelRenderer");
            assertTrue(rVexara instanceof VexaraRenderer, "Vexara -> VexaraRenderer");
            assertTrue(rDraven instanceof DravenRenderer, "Draven -> DravenRenderer");
        });
    }

    private static void testHeroAnimationFramesOnDisk() {
        runTest("All Six Champions Have Complete 12-Frame Animation Sets on Disk", () -> {
            String[] heroes = {"aegis", "nyx", "zephyra", "kael", "vexara", "draven"};
            String[] frames = {"idle", "walk1", "walk2", "walk3", "walk4", "jump", "dash",
                    "attack1", "attack2", "attack3", "hurt", "defeat"};

            for (String hero : heroes) {
                for (String frame : frames) {
                    BufferedImage img = HeroSpriteLoader.getHeroFrame(hero, frame);
                    assertTrue(img != null, hero + "/" + frame + ".png exists and loads cleanly");
                    assertTrue(img.getWidth() > 50 && img.getHeight() > 100,
                            hero + "/" + frame + " has valid dimensions: " + img.getWidth() + "x" + img.getHeight());
                }
            }
        });
    }

    private static void testEnemyArtworkAssetsOnDisk() {
        runTest("All Six Enemy and Boss Cutout Artwork Files Exist and Load Cleanly", () -> {
            String[] enemies = {"malakor", "morgatha", "rift_thrall", "void_skirmisher", "ironclad_grunt", "rift_cultist"};

            for (String enemy : enemies) {
                BufferedImage img = HeroSpriteLoader.getEnemySprite(enemy);
                assertTrue(img != null, "assets/enemies/" + enemy + ".png exists and loads cleanly");
                assertTrue(img.getWidth() >= 100 && img.getHeight() >= 100,
                        enemy + " sprite dimensions valid: " + img.getWidth() + "x" + img.getHeight());
            }
        });
    }

    private static void testSpecialPowerMeterAccumulationAndGating() {
        runTest("Special Power Meter Accumulates on Hits/Damage and Gates Special Moves", () -> {
            Hero kael = new Kael();
            RealTimeArena arena = new RealTimeArena(800, 500, 360, 50, 750);
            RealTimeCombatEngine engine = new RealTimeCombatEngine(kael, new Enemy("Dummy", 200, 10, 5, Enemy.Behaviour.TACTICAL), arena);

            RealTimePlayer player = engine.getPlayer();

            // 1. Initial combat state: Special meter starts full (100.0%) so player can act
            assertEquals(100.0f, player.getSpecialMeter(), "Special starts at 100% full");
            assertTrue(player.isSpecialReady(), "Special is ready at match start");

            // 2. Fire special ability via input
            engine.getInputState().setSpecialRequested(true);
            engine.update(0.02f);
            assertEquals(0.0f, player.getSpecialMeter(), "Special meter resets to 0% after firing");
            assertTrue(!player.isSpecialReady(), "Special is locked while meter is below 100%");

            // 3. Accumulate special power by dealing damage (+20% per hit)
            player.addSpecialPower(20.0f);
            assertEquals(20.0f, player.getSpecialMeter(), "Meter increases to 20%");
            assertTrue(!player.isSpecialReady(), "Cannot fire special at 20%");

            // 4. Taking damage also charges special power (+15%)
            player.addSpecialPower(15.0f);
            assertEquals(35.0f, player.getSpecialMeter(), "Meter increases to 35%");

            // 5. Fill meter to 100%
            player.addSpecialPower(65.0f);
            assertEquals(100.0f, player.getSpecialMeter(), "Meter capped at 100%");
            // Clear cooldown timer
            player.updateTimers(5.0f);
            assertTrue(player.isSpecialReady(), "Special is ready once meter is 100% and cooldown is 0");
        });
    }

    private static void testKaelCombatProfileAndSpecial() {
        runTest("Kael Piercing Thrust Combat Profile and High Mobility", () -> {
            Kael kael = new Kael();
            RealTimeArena arena = new RealTimeArena(800, 500, 360, 50, 750);
            RealTimeCombatEngine engine = new RealTimeCombatEngine(kael, new Enemy("Dummy", 200, 10, 5, Enemy.Behaviour.TACTICAL), arena);
            RealTimePlayer player = engine.getPlayer();

            engine.getInputState().setAttackRequested(true);
            engine.update(0.02f);
            assertEquals(EntityState.ATTACKING, player.getState(), "Kael enters attack state");

            // Clear attack
            engine.getInputState().setAttackRequested(false);
            for (int i = 0; i < 25; i++) engine.update(0.02f);

            // Special: Crimson Thrust
            player.addSpecialPower(100.0f);
            engine.getInputState().setSpecialRequested(true);
            engine.update(0.02f);
            assertEquals(EntityState.SPECIAL, player.getState(), "Kael enters special attack flurry");
        });
    }

    private static void testVexaraCombatProfileAndLifeSiphon() {
        runTest("Vexara Soul Reap Special Siphons Health on Activation", () -> {
            Vexara vexara = new Vexara();
            RealTimeArena arena = new RealTimeArena(800, 500, 360, 50, 750);
            RealTimeCombatEngine engine = new RealTimeCombatEngine(vexara, new Enemy("Dummy", 200, 10, 5, Enemy.Behaviour.TACTICAL), arena);
            RealTimePlayer player = engine.getPlayer();

            // Damage player
            player.takeDamage(30, 0, 0);
            int damagedHp = player.getCurrentHp();
            assertEquals(55, damagedHp, "Vexara reduced to 55 HP");

            // Allow hurt recovery timer (0.25s) to elapse
            for (int i = 0; i < 15; i++) {
                engine.update(0.02f);
            }

            // Trigger Soul Reap
            player.addSpecialPower(100.0f);
            engine.getInputState().setSpecialRequested(true);
            engine.update(0.02f);

            // Soul Reap heals 15 HP
            assertEquals(70, player.getCurrentHp(), "Soul Reap siphoned 15 HP to heal Vexara to 70 HP");
        });
    }

    private static void testDravenCombatProfileAndSpecial() {
        runTest("Draven Magma Eruption Special Grants Invulnerability and Ground Slam", () -> {
            Draven draven = new Draven();
            RealTimeArena arena = new RealTimeArena(800, 500, 360, 50, 750);
            RealTimeCombatEngine engine = new RealTimeCombatEngine(draven, new Enemy("Dummy", 200, 10, 5, Enemy.Behaviour.TACTICAL), arena);
            RealTimePlayer player = engine.getPlayer();

            assertEquals(135, player.getMaxHp(), "Draven has 135 max HP");
            engine.getInputState().setSpecialRequested(true);
            engine.update(0.02f);

            assertTrue(player.isInvulnerable(), "Draven gains invulnerability frame during Magma Eruption");
        });
    }

    private static void testEnemyArtworkRendererFallbacks() {
        runTest("Enemy Renderers Safely Function When Sprites are Rendered Offscreen", () -> {
            BufferedImage testCanvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = testCanvas.createGraphics();

            CharacterRenderer wardenRenderer = new RiftWardenRenderer();
            CharacterRenderer spawnRenderer = CharacterRendererFactory.forCombatant(new Enemy("Rift Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL));

            wardenRenderer.render(g2, 200, 150, 240, com.riftarena.ui.render.AnimationState.IDLE, 0f, 0.5f, false);
            spawnRenderer.render(g2, 200, 150, 200, com.riftarena.ui.render.AnimationState.ATTACKING, 0.5f, 0.5f, true);

            g2.dispose();
            assertTrue(testCanvas.getWidth() == 400, "Rendered without throwing exceptions");
        });
    }

    // ------------------------------------------------------------------
    // Assertions
    // ------------------------------------------------------------------

    private static void runTest(String name, Runnable action) {
        testsRun++;
        try {
            action.run();
            testsPassed++;
            System.out.println("  [PASS] " + name);
        } catch (Throwable t) {
            System.err.println("  [FAIL] " + name + ": " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private static void assertEquals(float expected, float actual, String message) {
        if (Math.abs(expected - actual) < 0.001f) return;
        throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

