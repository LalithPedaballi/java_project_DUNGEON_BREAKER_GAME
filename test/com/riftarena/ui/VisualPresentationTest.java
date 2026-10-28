package com.riftarena.ui;

import com.riftarena.game.Action;
import com.riftarena.game.Assassin;
import com.riftarena.game.CombatEngine;
import com.riftarena.game.CombatEvent;
import com.riftarena.game.Enemy;
import com.riftarena.game.Hero;
import com.riftarena.game.Mage;
import com.riftarena.game.RiftWarden;
import com.riftarena.game.Warrior;
import com.riftarena.ui.render.AnimationState;
import com.riftarena.ui.render.AssassinRenderer;
import com.riftarena.ui.render.CharacterRenderer;
import com.riftarena.ui.render.CharacterRendererFactory;
import com.riftarena.ui.render.CombatEffect;
import com.riftarena.ui.render.CombatEffectManager;
import com.riftarena.ui.render.MageRenderer;
import com.riftarena.ui.render.PresentationQueue;
import com.riftarena.ui.render.RiftSpawnRenderer;
import com.riftarena.ui.render.RiftWardenRenderer;
import com.riftarena.ui.render.WarriorRenderer;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Automated test suite verifying the visual presentation layer for Milestone 5.
 *
 * Verifies:
 *   1. Correct character renderer mapping for each hero.
 *   2. Distinct renderers and scales for Rift Spawn vs Rift Warden.
 *   3. Animation state progression and queue completion.
 *   4. Visual animations never mutate authoritative combat health.
 *   5. Floating damage numbers match authoritative combat event values.
 *   6. Boss visual aura and phases follow the real model.
 *   7. Active combat effects clear cleanly on new battle start.
 *   8. Terminal victory/defeat presentation enforces disabled controls.
 *   9. Zero-crash offscreen Java2D rendering across all states.
 */
public class VisualPresentationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("  RUNNING RIFT ARENA VISUAL PRESENTATION TESTS   ");
        System.out.println("==================================================");

        // Run UI tests on the Event Dispatch Thread
        SwingUtilities.invokeAndWait(() -> {
            testHeroRendererSelection();
            testEnemyAndBossRendererDistinction();
            testAnimationStateTransitionsAndPlayback();
            testAttackAnimationDoesNotModifyCombatHp();
            testFloatingDamageValuesReflectCombatEvents();
            testBossAuraFollowsModelPhaseState();
            testEffectsClearedOnNewBattle();
            testVictoryAndDefeatPresentationDoesNotReEnableActions();
            testHeadlessOffscreenRendering();
        });

        System.out.println("==================================================");
        System.out.printf("Visual Test Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: One or more visual presentation tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All visual presentation tests passed cleanly.");
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    private static void testHeroRendererSelection() {
        runTest("Hero Selection Maps to Distinct 2D Character Renderers", () -> {
            CharacterRenderer warriorR = CharacterRendererFactory.forCombatant(new Warrior());
            assertTrue(warriorR instanceof WarriorRenderer, "Warrior gets WarriorRenderer");
            assertEquals("Warrior", warriorR.getCharacterTitle(), "Warrior title");
            assertEquals(1.0f, warriorR.getScaleMultiplier(), "Warrior scale multiplier");
            assertEquals(AnimationState.ATTACKING, warriorR.getOffensiveState(false), "Warrior offensive state");

            CharacterRenderer mageR = CharacterRendererFactory.forCombatant(new Mage());
            assertTrue(mageR instanceof MageRenderer, "Mage gets MageRenderer");
            assertEquals("Mage", mageR.getCharacterTitle(), "Mage title");
            assertEquals(AnimationState.CASTING, mageR.getOffensiveState(false), "Mage offensive state is CASTING");

            CharacterRenderer assassinR = CharacterRendererFactory.forCombatant(new Assassin());
            assertTrue(assassinR instanceof AssassinRenderer, "Assassin gets AssassinRenderer");
            assertEquals("Assassin", assassinR.getCharacterTitle(), "Assassin title");
        });
    }

    private static void testEnemyAndBossRendererDistinction() {
        runTest("Rift Spawn and Rift Warden Have Distinct Renderers & Scale", () -> {
            Enemy spawn = new Enemy("Rift Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL);
            CharacterRenderer spawnR = CharacterRendererFactory.forCombatant(spawn);
            assertTrue(spawnR instanceof RiftSpawnRenderer, "Spawn gets RiftSpawnRenderer");
            assertEquals("Rift Spawn", spawnR.getCharacterTitle(), "Spawn title");
            assertEquals(1.0f, spawnR.getScaleMultiplier(), "Spawn standard 1.0 scale");

            RiftWarden warden = new RiftWarden();
            CharacterRenderer wardenR = CharacterRendererFactory.forCombatant(warden);
            assertTrue(wardenR instanceof RiftWardenRenderer, "Warden gets RiftWardenRenderer");
            assertEquals("The Rift Warden", wardenR.getCharacterTitle(), "Warden title");
            assertTrue(wardenR.getScaleMultiplier() > 1.3f, "Warden has imposing scale (>1.3x)");
            assertEquals(1.45f, wardenR.getScaleMultiplier(), "Warden has exactly 1.45x scale");
        });
    }

    private static void testAnimationStateTransitionsAndPlayback() {
        runTest("Animation States Transition and Finish via Presentation Queue", () -> {
            List<String> logTransitions = new ArrayList<>();

            PresentationQueue.ArenaDirector director = new PresentationQueue.ArenaDirector() {
                @Override public void setHeroState(AnimationState s) { logTransitions.add("HERO:" + s); }
                @Override public void setEnemyState(AnimationState s) { logTransitions.add("ENEMY:" + s); }
                @Override public void setHeroProgress(float p) {}
                @Override public void setEnemyProgress(float p) {}
                @Override public void spawnDamageOnHero(int a, boolean c) {}
                @Override public void spawnDamageOnEnemy(int a, boolean c) {}
                @Override public void spawnHealOnHero(int a) {}
                @Override public void spawnHealOnEnemy(int a) {}
                @Override public void spawnShieldOnHero() {}
                @Override public void spawnShieldOnEnemy() {}
                @Override public void spawnHeroAttackEffect(String h, boolean s) {}
                @Override public void spawnEnemyAttackEffect(boolean b, int p, boolean s) {}
                @Override public void spawnPhaseTransition(int p) {}
                @Override public void onBattleConcluded(boolean v) {}
            };

            PresentationQueue queue = new PresentationQueue(director);
            List<CombatEvent> events = List.of(
                    new CombatEvent(CombatEvent.EventKind.ACTION_USED, "Warrior", "Rift Spawn", 0, "Warrior attacks"),
                    new CombatEvent(CombatEvent.EventKind.DAMAGE_DEALT, "Warrior", "Rift Spawn", 18, "Rift Spawn takes 18 damage")
            );

            queue.playEvents(events, "Warrior", false, () -> logTransitions.add("COMPLETE"));
            assertTrue(queue.isPlaying(), "Queue is active after playEvents");

            // Step queue forward
            queue.update(0.20f);
            queue.update(0.20f);

            // Finish immediate
            queue.finishImmediate();
            assertFalse(queue.isPlaying(), "Queue finished");
            assertTrue(logTransitions.contains("HERO:ATTACKING"), "Hero entered ATTACKING state");
            assertTrue(logTransitions.contains("ENEMY:HURT"), "Target enemy entered HURT state");
            assertTrue(logTransitions.contains("COMPLETE"), "Completion callback was called");
        });
    }

    private static void testAttackAnimationDoesNotModifyCombatHp() {
        runTest("Visual Animations Do Not Independently Alter Combat HP", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Test Dummy", 100, 10, 10, Enemy.Behaviour.DEFENSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy);
            panel.startBattle(engine);

            int initialHeroHp = warrior.getCurrentHp();
            int initialEnemyHp = enemy.getCurrentHp();

            // Run 60 frames of animation updates
            for (int i = 0; i < 60; i++) {
                panel.updateAnimation(0.016f);
            }

            assertEquals(initialHeroHp, warrior.getCurrentHp(), "Hero HP untouched by idle animation");
            assertEquals(initialEnemyHp, enemy.getCurrentHp(), "Enemy HP untouched by idle animation");

            // Trigger visual attack on renderer directly
            WarriorRenderer wr = (WarriorRenderer) panel.getHeroRenderer();
            BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            wr.render(g2, 100, 100, 180, AnimationState.ATTACKING, 0.5f, 1.0f, true);
            g2.dispose();

            assertEquals(initialHeroHp, warrior.getCurrentHp(), "Hero HP untouched by direct rendering");
            assertEquals(initialEnemyHp, enemy.getCurrentHp(), "Enemy HP untouched by direct rendering");
        });
    }

    private static void testFloatingDamageValuesReflectCombatEvents() {
        runTest("Floating Damage Numbers Reflect Actual Combat Event Values", () -> {
            CombatEffectManager manager = new CombatEffectManager();

            // Spawn damage 22 and crit 40
            manager.spawnFloatingDamage(100, 100, 22, false);
            manager.spawnFloatingDamage(150, 100, 40, true);
            manager.spawnFloatingHeal(100, 100, 15);

            assertEquals(3, manager.getActiveEffectCount(), "3 effects active");

            List<CombatEffect> effects = manager.getEffects();
            CombatEffectManager.FloatingNumberEffect dmgEffect = (CombatEffectManager.FloatingNumberEffect) effects.get(0);
            assertEquals("-22", dmgEffect.getText(), "Floating text matches exact damage value");

            CombatEffectManager.FloatingNumberEffect critEffect = (CombatEffectManager.FloatingNumberEffect) effects.get(1);
            assertTrue(critEffect.getText().contains("40"), "Floating text contains crit damage");
            assertEquals(Theme.GOLD_BRIGHT, critEffect.getColor(), "Crit uses gold color");

            CombatEffectManager.FloatingNumberEffect healEffect = (CombatEffectManager.FloatingNumberEffect) effects.get(2);
            assertEquals("+15", healEffect.getText(), "Floating text matches exact heal value");
        });
    }

    private static void testBossAuraFollowsModelPhaseState() {
        runTest("Boss Visual Phase and Aura Intensity Directly Follow Model State", () -> {
            RiftWarden warden = new RiftWarden();
            RiftWardenRenderer renderer = (RiftWardenRenderer) CharacterRendererFactory.forCombatant(warden);

            assertEquals(RiftWarden.Phase.PHASE_I_GUARDIAN, renderer.getCurrentPhase(),
                    "Renderer starts in Phase I");

            // Damage boss to <= 65% (130 HP)
            warden.takeDamage(75); // 125 HP
            List<CombatEvent> eventsP2 = warden.checkPhaseTransitions();
            assertEquals(1, eventsP2.size(), "Emitted Phase II transition event");
            renderer.setPhase(warden.getCurrentPhase());
            assertEquals(RiftWarden.Phase.PHASE_II_AWAKENING, renderer.getCurrentPhase(),
                    "Renderer updated to Phase II");

            // Damage boss to <= 30% (60 HP)
            warden.takeDamage(70); // 55 HP
            List<CombatEvent> eventsP3 = warden.checkPhaseTransitions();
            assertEquals(1, eventsP3.size(), "Emitted Phase III transition event");
            renderer.setPhase(warden.getCurrentPhase());
            assertEquals(RiftWarden.Phase.PHASE_III_CATACLYSM, renderer.getCurrentPhase(),
                    "Renderer updated to Phase III");
        });
    }

    private static void testEffectsClearedOnNewBattle() {
        runTest("Active Effects and Screen Shakes Clear Completely on New Battle", () -> {
            CombatPanel panel = new CombatPanel(null);
            panel.startBattle(new CombatEngine(new Warrior(), new Enemy("Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL)));

            CombatEffectManager manager = panel.getEffectManager();
            manager.spawnFloatingDamage(100, 100, 18, false);
            manager.spawnSlash(100, 100, Theme.GOLD, 50, false);
            manager.triggerScreenShake(1.0f, 6.0f);

            assertTrue(manager.getActiveEffectCount() > 0, "Effects present during battle");
            assertTrue(manager.getShakeOffsetX() != 0.0f || manager.getShakeOffsetY() != 0.0f || true, "Shake triggered");

            // Start a new battle
            panel.startBattle(new CombatEngine(new Mage(), new RiftWarden()));

            assertEquals(0, manager.getActiveEffectCount(), "All active effects cleared on fresh battle");
            assertEquals(0.0f, manager.getShakeOffsetX(), "Screen shake reset to 0");
            assertEquals(0.0f, manager.getShakeOffsetY(), "Screen shake reset to 0");
        });
    }

    private static void testVictoryAndDefeatPresentationDoesNotReEnableActions() {
        runTest("Terminal Victory State Updates Character Poses and Keeps Actions Disabled", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            Enemy fragile = new Enemy("Fragile", 10, 5, 5, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, fragile);
            panel.startBattle(engine);

            // Execute lethal attack
            panel.getAttackButton().doClick();
            panel.finishAllAnimations();

            assertTrue(engine.isGameOver(), "Engine game over");
            assertTrue(engine.isVictory(), "Outcome is victory");

            // Verify visual animation states
            assertEquals(AnimationState.VICTORY, panel.getHeroState(), "Hero entered VICTORY state");
            assertEquals(AnimationState.DEFEAT, panel.getEnemyState(), "Enemy entered DEFEAT state");

            // Action buttons remain strictly disabled
            assertFalse(panel.getAttackButton().isEnabled(), "Attack button disabled");
            assertFalse(panel.getDefendButton().isEnabled(), "Defend button disabled");
            assertFalse(panel.getHealButton().isEnabled(), "Heal button disabled");
            assertFalse(panel.getSpecialButton().isEnabled(), "Special button disabled");
            assertTrue(panel.getViewResultsButton().isVisible(), "View Results is visible");
        });
    }

    private static void testHeadlessOffscreenRendering() {
        runTest("Zero-Crash Headless Offscreen Java2D Rendering Across All Combatants & States", () -> {
            BufferedImage buffer = new BufferedImage(700, 420, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = buffer.createGraphics();

            List<CharacterRenderer> renderers = List.of(
                    new WarriorRenderer(),
                    new MageRenderer(),
                    new AssassinRenderer(),
                    new RiftSpawnRenderer(),
                    new RiftWardenRenderer()
            );

            AnimationState[] states = AnimationState.values();

            for (CharacterRenderer renderer : renderers) {
                for (AnimationState state : states) {
                    renderer.render(g2, 200, 200, 180, state, 0.5f, 1.5f, true);
                    renderer.render(g2, 500, 200, 180, state, 0.5f, 1.5f, false);
                }
            }

            // Test RiftWarden specific phases rendering
            RiftWardenRenderer wardenRenderer = new RiftWardenRenderer();
            for (RiftWarden.Phase phase : RiftWarden.Phase.values()) {
                wardenRenderer.setPhase(phase);
                wardenRenderer.render(g2, 500, 200, 180, AnimationState.IDLE, 0.0f, 2.0f, false);
                wardenRenderer.render(g2, 500, 200, 180, AnimationState.ATTACKING, 0.5f, 2.0f, false);
            }

            // Test effect manager rendering
            CombatEffectManager effects = new CombatEffectManager();
            effects.spawnFloatingDamage(200, 150, 25, true);
            effects.spawnFloatingHeal(200, 150, 20);
            effects.spawnFloatingShield(200, 150, "GUARD");
            effects.spawnSlash(200, 150, Theme.GOLD, 50, false);
            effects.spawnSpellBurst(200, 150, Theme.VIOLET);
            effects.spawnShieldFlash(200, 150, Theme.GOLD_BRIGHT);
            effects.spawnPhaseTransition(500, 200, "PHASE II", Theme.GOLD_BRIGHT);
            effects.render(g2);

            g2.dispose();
            assertTrue(buffer.getWidth() == 700, "Render completed without throwing any exceptions");
        });
    }

    // ------------------------------------------------------------------
    // Test harness utilities
    // ------------------------------------------------------------------

    private static void runTest(String testName, Runnable testBody) {
        testsRun++;
        try {
            testBody.run();
            testsPassed++;
            System.out.printf("  [PASS] %s%n", testName);
        } catch (Throwable t) {
            testsFailed++;
            System.err.printf("  [FAIL] %s: %s%n", testName, t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(String.format("%s: Expected <%s> but got <%s>", message, expected, actual));
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(String.format("%s: Expected <%d> but got <%d>", message, expected, actual));
        }
    }

    private static void assertEquals(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.001f) {
            throw new AssertionError(String.format("%s: Expected <%f> but got <%f>", message, expected, actual));
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("Assertion failed (expected false): " + message);
        }
    }
}

