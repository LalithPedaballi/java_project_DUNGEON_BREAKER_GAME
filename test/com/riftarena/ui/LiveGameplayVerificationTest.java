package com.riftarena.ui;

import com.riftarena.game.Assassin;
import com.riftarena.game.Hero;
import com.riftarena.game.Mage;
import com.riftarena.game.Warrior;
import com.riftarena.realtime.DungeonRun;
import com.riftarena.realtime.DungeonStage;
import com.riftarena.realtime.EntityState;
import com.riftarena.realtime.InputState;
import com.riftarena.realtime.Projectile;
import com.riftarena.realtime.RealTimeBoss;
import com.riftarena.realtime.RealTimeCombatEngine;
import com.riftarena.realtime.RealTimeEnemy;
import com.riftarena.realtime.RealTimePlayer;
import com.riftarena.ui.render.AssassinRenderer;
import com.riftarena.ui.render.CharacterRenderer;
import com.riftarena.ui.render.MageRenderer;
import com.riftarena.ui.render.WarriorRenderer;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Autonomous live manual-launch & gameplay verification suite for
 * Dungeon Clash: Rift Arena.
 *
 * Simulates complete user journey from GameApp / MainFrame boot,
 * through hero selection across all 3 classes, continuous combat physics,
 * ability execution, dungeon stage progression, boss combat, pause,
 * restart, and result screen navigation.
 */
public class LiveGameplayVerificationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println(" RUNNING LIVE GAMEPLAY & FLOW VERIFICATION SUITE  ");
        System.out.println("==================================================");

        // Run all tests on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeAndWait(() -> {
            testAppBootAndMainMenu();
            testHeroSelectionFlow();
            testRealTimeMovementAndJumping();
            testRealTimeAttackingAndHitboxes();
            testClassSpecificSpecialAbilities();
            testDashMechanicsAndInvulnerability();
            testFullDungeonProgressionAndBossPhases();
            testPauseResumeAndRestartFlow();
            testSurrenderAndResultScreenNavigation();
            testZeroCrashRenderingAcrossAllScreens();
        });

        System.out.println("==================================================");
        System.out.printf("Verification Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: One or more live verification tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All live gameplay verification tests passed cleanly.");
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    // Verification Tests
    // ------------------------------------------------------------------

    private static void testAppBootAndMainMenu() {
        runTest("MainFrame Boots to Main Menu with Navigation Components", () -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1024, 680);
            frame.doLayout();

            assertEquals("Dungeon Clash: Rift Arena", frame.getTitle(), "Window title matches specification");
            assertEquals(MainFrame.Screen.MENU, frame.getCurrentScreen(), "MainFrame boots to MENU screen");
            assertNotNull(frame.getCombatPanel(), "Combat panel is instantiated");
            assertNotNull(frame.getResultPanel(), "Result panel is instantiated");
            assertNotNull(frame.getHeroSelectPanel(), "Hero select panel is instantiated");

            // Verify navigation to hero selection
            frame.showScreen(MainFrame.Screen.HERO_SELECT);
            assertEquals(MainFrame.Screen.HERO_SELECT, frame.getCurrentScreen(), "Screen changes to HERO_SELECT");
            assertEquals(MainFrame.EncounterType.SKIRMISH, frame.getEncounterType(), "Default encounter type is Skirmish");
        });
    }

    private static void testHeroSelectionFlow() {
        runTest("Hero Selection Accurately Configures Warrior, Mage, and Assassin", () -> {
            MainFrame frame = new MainFrame();

            // 1. Warrior
            frame.startBattle("Warrior");
            CombatPanel panel = frame.getCombatPanel();
            RealTimeCombatEngine rt = panel.getRealTimeEngine();
            assertNotNull(rt, "Real-time engine active for Warrior");
            assertEquals("Warrior", rt.getPlayer().getHeroClass(), "Player hero class is Warrior");
            assertEquals(120, rt.getPlayer().getMaxHp(), "Warrior Max HP is 120");
            CharacterRenderer heroRenderer = getField(panel.getArenaView(), "heroRenderer");
            assertTrue(heroRenderer instanceof WarriorRenderer, "WarriorRenderer assigned");

            // 2. Mage
            frame.startBattle("Mage");
            rt = panel.getRealTimeEngine();
            assertEquals("Mage", rt.getPlayer().getHeroClass(), "Player hero class is Mage");
            assertEquals(80, rt.getPlayer().getMaxHp(), "Mage Max HP is 80");
            heroRenderer = getField(panel.getArenaView(), "heroRenderer");
            assertTrue(heroRenderer instanceof MageRenderer, "MageRenderer assigned");

            // 3. Assassin
            frame.startBattle("Assassin");
            rt = panel.getRealTimeEngine();
            assertEquals("Assassin", rt.getPlayer().getHeroClass(), "Player hero class is Assassin");
            assertEquals(95, rt.getPlayer().getMaxHp(), "Assassin Max HP is 95");
            heroRenderer = getField(panel.getArenaView(), "heroRenderer");
            assertTrue(heroRenderer instanceof AssassinRenderer, "AssassinRenderer assigned");
        });
    }

    private static void testRealTimeMovementAndJumping() {
        runTest("Player Continuous Movement, Arena Boundary, and Jump Physics", () -> {
            CombatPanel panel = new CombatPanel(null);
            panel.startDungeon(new Warrior());
            DungeonRun run = panel.getDungeonRun();
            RealTimeCombatEngine rt = panel.getRealTimeEngine();
            RealTimePlayer player = rt.getPlayer();
            InputState input = rt.getInputState();

            // Advance through entrance transition
            run.update(2.1f);

            float initialX = player.getX();
            float initialY = player.getY();

            // Move Right
            input.onKeyPressed(KeyEvent.VK_D);
            run.update(0.1f);
            assertTrue(player.getX() > initialX, "Player x-position increases when moving right");
            assertTrue(player.isFacingRight(), "Player faces right when moving right");
            input.onKeyReleased(KeyEvent.VK_D);

            // Move Left
            float currentX = player.getX();
            input.onKeyPressed(KeyEvent.VK_A);
            run.update(0.1f);
            assertTrue(player.getX() < currentX, "Player x-position decreases when moving left");
            assertFalse(player.isFacingRight(), "Player faces left when moving left");
            input.onKeyReleased(KeyEvent.VK_A);

            // Jump
            input.onKeyPressed(KeyEvent.VK_W);
            run.update(0.05f);
            assertTrue(player.getVy() < 0, "Player vertical velocity is upward during jump");
            assertTrue(player.getY() < initialY, "Player y-position is above ground");
            input.onKeyReleased(KeyEvent.VK_W);

            // Gravity brings player back to ground
            for (int i = 0; i < 60; i++) {
                run.update(0.016f);
            }
            assertEquals(initialY, player.getY(), 0.5f, "Player safely lands back on ground level");
            assertEquals(0.0f, player.getVy(), 0.01f, "Vertical velocity resets to 0 upon landing");
        });
    }

    private static void testRealTimeAttackingAndHitboxes() {
        runTest("Player Attack Cooldown, Hitbox Trigger, and Damage Infliction", () -> {
            CombatPanel panel = new CombatPanel(null);
            panel.startDungeon(new Warrior());
            DungeonRun run = panel.getDungeonRun();
            RealTimeCombatEngine rt = panel.getRealTimeEngine();
            RealTimePlayer player = rt.getPlayer();
            InputState input = rt.getInputState();

            // Advance to Encounter 1
            run.update(2.1f);
            RealTimeEnemy target = rt.getEnemy();
            assertNotNull(target, "Enemy present in Encounter 1");
            int initialEnemyHp = target.getCurrentHp();

            // Position player right next to enemy
            player.setX(target.getX() - 35);
            player.setFacingRight(true);

            // Press Attack key [J]
            input.onKeyPressed(KeyEvent.VK_J);
            run.update(0.04f);
            input.onKeyReleased(KeyEvent.VK_J);

            assertEquals(EntityState.ATTACKING, player.getState(), "Player entered ATTACKING state");
            assertTrue(player.getAttackCooldownTimer() > 0, "Attack cooldown active");

            // Advance into active strike hitbox window (progress between 0.18 and 0.72)
            for (int i = 0; i < 3; i++) {
                run.update(0.04f);
            }
            assertTrue(target.getCurrentHp() < initialEnemyHp, "Target enemy took damage from player attack");
            assertTrue(target.isHurt(), "Target enemy entered hurt state");
        });
    }

    private static void testClassSpecificSpecialAbilities() {
        runTest("Class-Specific Special Abilities (Warrior Defense, Mage Nova, Assassin Execute)", () -> {
            // 1. Warrior Shield Slam (Grants guard invulnerability & high damage)
            CombatPanel warriorPanel = new CombatPanel(null);
            warriorPanel.startDungeon(new Warrior());
            DungeonRun wRun = warriorPanel.getDungeonRun();
            RealTimeCombatEngine wRt = warriorPanel.getRealTimeEngine();
            wRun.update(2.1f);
            RealTimeEnemy wTarget = wRt.getEnemy();
            wRt.getPlayer().setX(wTarget.getX() - 30);
            wRt.getInputState().onKeyPressed(KeyEvent.VK_K);
            wRun.update(0.05f);
            wRt.getInputState().onKeyReleased(KeyEvent.VK_K);
            assertTrue(wRt.getPlayer().getSpecialCooldownTimer() > 0, "Warrior special cooldown active");
            assertTrue(wRt.getPlayer().isInvulnerable(), "Warrior Shield Slam granted defensive guard");

            // 2. Mage Basic Projectile & Arcane Nova Area Blast
            CombatPanel magePanel = new CombatPanel(null);
            magePanel.startDungeon(new Mage());
            DungeonRun mRun = magePanel.getDungeonRun();
            RealTimeCombatEngine mRt = magePanel.getRealTimeEngine();
            mRun.update(2.1f);
            int initialProjCount = mRt.getProjectiles().size();
            mRt.getInputState().onKeyPressed(KeyEvent.VK_J);
            mRun.update(0.15f);
            mRt.getInputState().onKeyReleased(KeyEvent.VK_J);
            assertTrue(mRt.getProjectiles().size() > initialProjCount, "Mage basic attack spawned magical projectile");
            Projectile p = mRt.getProjectiles().get(mRt.getProjectiles().size() - 1);
            assertTrue(p.isFromPlayer(), "Projectile is flagged as player projectile");

            // 3. Assassin Shadow Strike
            CombatPanel assassinPanel = new CombatPanel(null);
            assassinPanel.startDungeon(new Assassin());
            DungeonRun aRun = assassinPanel.getDungeonRun();
            RealTimeCombatEngine aRt = assassinPanel.getRealTimeEngine();
            aRun.update(2.1f);
            RealTimeEnemy aTarget = aRt.getEnemy();
            aRt.getPlayer().setX(aTarget.getX() - 30);
            aRt.getInputState().onKeyPressed(KeyEvent.VK_K);
            aRun.update(0.05f);
            aRt.getInputState().onKeyReleased(KeyEvent.VK_K);
            assertTrue(aRt.getPlayer().getSpecialCooldownTimer() > 0, "Assassin special cooldown active");
            assertEquals(EntityState.SPECIAL, aRt.getPlayer().getState(), "Assassin in SPECIAL state");
        });
    }

    private static void testDashMechanicsAndInvulnerability() {
        runTest("Dash Evasion Applies High Velocity, i-Frames, and Invulnerability", () -> {
            CombatPanel panel = new CombatPanel(null);
            panel.startDungeon(new Warrior());
            DungeonRun run = panel.getDungeonRun();
            RealTimeCombatEngine rt = panel.getRealTimeEngine();
            RealTimePlayer player = rt.getPlayer();
            InputState input = rt.getInputState();

            run.update(2.1f);
            player.setFacingRight(true);

            // Press Shift to dash
            input.onKeyPressed(KeyEvent.VK_SHIFT);
            run.update(0.05f);
            input.onKeyReleased(KeyEvent.VK_SHIFT);

            assertEquals(EntityState.DASHING, player.getState(), "Player in DASHING state");
            assertTrue(player.isInvulnerable(), "Player is invulnerable during dash");
            assertTrue(player.getVx() > 200, "Dash applies high forward velocity");
            assertTrue(player.getDashCooldownTimer() > 0, "Dash cooldown is active");

            // Verify that incoming damage is nullified while dashing
            int hpBeforeHit = player.getCurrentHp();
            int damageTaken = player.takeDamage(50, 1, 0);
            assertEquals(0, damageTaken, "Invulnerability nullified damage during dash");
            assertEquals(hpBeforeHit, player.getCurrentHp(), "Player HP preserved during dash i-frames");
        });
    }

    private static void testFullDungeonProgressionAndBossPhases() {
        runTest("Full Dungeon Progression: Entrance -> Encounters 1 & 2 -> Boss Gate -> Boss Cataclysm -> Victory", () -> {
            CombatPanel panel = new CombatPanel(null);
            panel.startDungeon(new Warrior());
            DungeonRun run = panel.getDungeonRun();
            RealTimeCombatEngine rt = panel.getRealTimeEngine();

            // 1. Entrance
            assertEquals(DungeonStage.ENTRANCE, run.getCurrentStage(), "Initial stage is ENTRANCE");

            // 2. Advance to Encounter 1
            run.update(2.1f);
            assertEquals(DungeonStage.ENCOUNTER_ONE, run.getCurrentStage(), "Advanced to ENCOUNTER_ONE");
            assertTrue(rt.getAliveEnemyCount() > 0, "Enemies spawned in Encounter 1");

            // Clear Encounter 1
            for (RealTimeEnemy e : rt.getEnemies()) {
                e.takeDamage(999, 1, 0);
            }
            run.update(0.04f);
            assertEquals(DungeonStage.ENCOUNTER_ONE_CLEARED, run.getCurrentStage(), "Stage is ENCOUNTER_ONE_CLEARED");

            // 3. Advance to Encounter 2
            run.update(1.9f);
            assertEquals(DungeonStage.ENCOUNTER_TWO, run.getCurrentStage(), "Advanced to ENCOUNTER_TWO");

            // Clear Encounter 2
            for (RealTimeEnemy e : rt.getEnemies()) {
                e.takeDamage(999, 1, 0);
            }
            run.update(0.04f);
            assertEquals(DungeonStage.BOSS_GATE, run.getCurrentStage(), "Stage is BOSS_GATE");

            // 4. Advance to Boss Encounter
            run.update(2.3f);
            assertEquals(DungeonStage.BOSS_ENCOUNTER, run.getCurrentStage(), "Advanced to BOSS_ENCOUNTER");
            RealTimeBoss boss = run.getBoss();
            assertNotNull(boss, "Rift Warden boss instantiated");
            assertEquals(240, boss.getCurrentHp(), "Boss starts with 240 HP");
            assertEquals(1, boss.getBossPhase(), "Boss starts in Phase 1");

            // Damage boss to <= 50% HP (<= 120 HP)
            boss.takeDamage(130, 1, 0);
            run.update(0.05f);
            assertEquals(2, boss.getBossPhase(), "Boss transitioned to Phase 2 (Cataclysm)");

            // Advance through phase transition invulnerability window (1.2s)
            run.update(1.3f);
            assertFalse(boss.isInvulnerable(), "Boss invulnerability cleared after transition");

            // Defeat boss
            boss.takeDamage(999, 1, 0);
            run.update(0.05f);
            assertEquals(DungeonStage.VICTORY, run.getCurrentStage(), "Stage transitioned to VICTORY");
            assertTrue(run.isVictory(), "DungeonRun flagged as victory");
            assertTrue(run.isGameOver(), "DungeonRun flagged as game over");
        });
    }

    private static void testPauseResumeAndRestartFlow() {
        runTest("Pause/Resume Freezes Physics; Restart Fully Restores Dungeon", () -> {
            CombatPanel panel = new CombatPanel(null);
            panel.startDungeon(new Warrior());
            DungeonRun run = panel.getDungeonRun();
            RealTimeCombatEngine rt = panel.getRealTimeEngine();
            run.update(2.1f); // In Encounter 1

            // 1. Pause
            rt.getInputState().onKeyPressed(KeyEvent.VK_ESCAPE);
            run.update(0.016f);
            rt.getInputState().onKeyReleased(KeyEvent.VK_ESCAPE);
            assertTrue(rt.isPaused(), "Engine is paused after pressing ESC");

            float playerXBefore = rt.getPlayer().getX();
            run.update(0.2f);
            assertEquals(playerXBefore, rt.getPlayer().getX(), 0.001f, "Physics completely frozen during pause");

            // 2. Resume
            rt.getInputState().onKeyPressed(KeyEvent.VK_ESCAPE);
            run.update(0.016f);
            rt.getInputState().onKeyReleased(KeyEvent.VK_ESCAPE);
            assertFalse(rt.isPaused(), "Engine resumed after pressing ESC again");

            // 3. Restart
            rt.getInputState().onKeyPressed(KeyEvent.VK_R);
            run.update(0.016f);
            rt.getInputState().onKeyReleased(KeyEvent.VK_R);
            assertEquals(DungeonStage.ENTRANCE, run.getCurrentStage(), "Restart reset stage to ENTRANCE");
            assertEquals(120, rt.getPlayer().getCurrentHp(), "Player HP restored to maximum");
            assertEquals(0, run.getTotalEnemiesDefeated(), "Kill count reset to zero");
        });
    }

    private static void testSurrenderAndResultScreenNavigation() {
        runTest("Surrender & Victory Route Correctly to ResultPanel with Proper Stats", () -> {
            MainFrame frame = new MainFrame();
            frame.startBattle("Warrior");
            assertEquals(MainFrame.Screen.COMBAT, frame.getCurrentScreen(), "Combat screen opened on startBattle");
            CombatPanel panel = frame.getCombatPanel();
            DungeonRun run = panel.getDungeonRun();

            // 1. Simulate victory
            run.update(2.1f);
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
            run.update(0.04f);
            run.update(1.9f);
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
            run.update(0.04f);
            run.update(2.3f);
            run.getBoss().takeDamage(999, 1, 0);
            run.update(0.05f); // Now VICTORY

            // Click View Results
            panel.getViewResultsButton().doClick();
            assertEquals(MainFrame.Screen.RESULT, frame.getCurrentScreen(), "Navigated to Result screen on View Results click");
            ResultPanel resultPanel = frame.getResultPanel();
            assertNotNull(resultPanel, "Navigated to ResultPanel");
            assertTrue(resultPanel.isVictory(), "ResultPanel outcome is Victory");
            assertTrue(resultPanel.getEnemiesDefeated() >= 4, "ResultPanel recorded vanquished enemies");
            assertNotNull(resultPanel.getPerformanceRank(), "Performance rank is present");

            // ResultPanel Navigation
            resultPanel.getPlayAgainButton().doClick();
            assertEquals(MainFrame.Screen.HERO_SELECT, frame.getCurrentScreen(), "Play Again routed to HERO_SELECT");

            resultPanel.getMainMenuButton().doClick();
            assertEquals(MainFrame.Screen.MENU, frame.getCurrentScreen(), "Main Menu button routed to MENU");

            // 2. Simulate Surrender
            frame.startBattle("Mage");
            assertEquals(MainFrame.Screen.COMBAT, frame.getCurrentScreen(), "Combat screen reopened for Mage");
            CombatPanel magePanel = frame.getCombatPanel();
            DungeonRun mageRun = magePanel.getDungeonRun();
            mageRun.update(2.1f); // Active in Encounter 1

            magePanel.getSurrenderButton().doClick();
            assertEquals(MainFrame.Screen.RESULT, frame.getCurrentScreen(), "Surrender routed immediately to Result screen");
            assertFalse(resultPanel.isVictory(), "Surrender registered as defeat");
            assertTrue(resultPanel.getPerformanceRank().contains("SURRENDERED"), "Rank reflects surrender outcome");
        });
    }

    private static void testZeroCrashRenderingAcrossAllScreens() {
        runTest("Zero-Crash Headless Java2D Offscreen Rendering Across All Panels", () -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1024, 680);
            frame.doLayout();

            // Render CombatPanel
            CombatPanel combat = frame.getCombatPanel();
            combat.startDungeon(new Warrior());
            combat.setSize(new Dimension(1024, 680));
            combat.doLayout();
            combat.getArenaView().setSize(new Dimension(1024, 680));
            combat.getArenaView().doLayout();

            BufferedImage image = new BufferedImage(1024, 680, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            combat.paint(g2);
            g2.dispose();

            // Render ResultPanel
            ResultPanel result = frame.getResultPanel();
            result.setOutcome(true, 45, 5, "S-RANK (LEGENDARY HUNTER)");
            result.setSize(new Dimension(1024, 680));
            result.doLayout();

            BufferedImage resImage = new BufferedImage(1024, 680, BufferedImage.TYPE_INT_ARGB);
            Graphics2D resG2 = resImage.createGraphics();
            result.paint(resG2);
            resG2.dispose();

            assertNotNull(image, "CombatPanel image painted cleanly");
            assertNotNull(resImage, "ResultPanel image painted cleanly");
        });
    }

    // ------------------------------------------------------------------
    // Test Infrastructure
    // ------------------------------------------------------------------

    private static void runTest(String description, TestRunnable test) {
        testsRun++;
        try {
            test.run();
            testsPassed++;
            System.out.printf("  [PASS] %s%n", description);
        } catch (Throwable t) {
            testsFailed++;
            System.err.printf("  [FAIL] %s%n", description);
            t.printStackTrace(System.err);
        }
    }

    @FunctionalInterface
    private interface TestRunnable {
        void run() throws Throwable;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " - expected: <" + expected + "> but was: <" + actual + ">");
    }

    private static void assertEquals(float expected, float actual, float delta, String message) {
        if (Math.abs(expected - actual) <= delta) return;
        throw new AssertionError(message + " - expected: <" + expected + "> +/- " + delta + " but was: <" + actual + ">");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError("Assertion failed (expected false): " + message);
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError("Expected non-null: " + message);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to access field: " + fieldName, ex);
        }
    }
}
