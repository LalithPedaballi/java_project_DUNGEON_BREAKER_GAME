package com.riftarena.ui;

import com.riftarena.game.Action;
import com.riftarena.game.Assassin;
import com.riftarena.game.CombatEngine;
import com.riftarena.game.Enemy;
import com.riftarena.game.Hero;
import com.riftarena.game.Mage;
import com.riftarena.game.RiftWarden;
import com.riftarena.game.Warrior;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Random;

/**
 * Headless-compatible, zero-dependency integration test suite for Milestone 3 UI-to-Engine binding.
 *
 * Verifies that the Swing combat screen interacts faithfully with {@link CombatEngine},
 * that user clicks invoke engine rules, that live model values drive the display,
 * and that terminal states disable controls cleanly.
 */
public class UIIntegrationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   RUNNING RIFT ARENA UI INTEGRATION TEST SUITE   ");
        System.out.println("==================================================");

        // Run UI tests on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeAndWait(() -> {
            testHeroFactoryMapping();
            testBattleInitializationAndLiveState();
            testButtonActionReachesEngineAndUpdatesLog();
            testSpecialButtonCooldownLabelAndDisabling();
            testCombatControlsDisabledOnVictory();
            testCombatControlsDisabledOnDefeat();
            testResultPanelReceivesRealOutcomeAndTurns();
            testReplayCreatesCompletelyFreshState();
            testBossBattleInitializationAndLivePhaseState();
            testBossPhaseTransitionsUpdateCombatUI();
            testBossBattleReplayResetsToPhaseI();

            if (!GraphicsEnvironment.isHeadless()) {
                testFullWindowNavigation();
                testFullWindowBossNavigation();
            }
        });

        System.out.println("==================================================");
        System.out.printf("UI Test Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: One or more UI integration tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All UI integration tests passed cleanly.");
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    private static void testHeroFactoryMapping() {
        runTest("Hero Selection Creates Correct Concrete Hero Subclasses", () -> {
            Hero warrior = MainFrame.createHero("Warrior");
            assertTrue(warrior instanceof Warrior, "Expected Warrior instance");
            assertEquals(120, warrior.getMaxHp(), "Warrior Max HP");

            Hero mage = MainFrame.createHero("Mage");
            assertTrue(mage instanceof Mage, "Expected Mage instance");
            assertEquals(80, mage.getMaxHp(), "Mage Max HP");

            Hero assassin = MainFrame.createHero("Assassin");
            assertTrue(assassin instanceof Assassin, "Expected Assassin instance");
            assertEquals(95, assassin.getMaxHp(), "Assassin Max HP");

            Hero fallback = MainFrame.createHero("Unknown");
            assertTrue(fallback instanceof Warrior, "Fallback defaults to Warrior");
        });
    }

    private static void testBattleInitializationAndLiveState() {
        runTest("CombatPanel Initializes Live State From Engine", () -> {
            CombatPanel combatPanel = new CombatPanel(null);
            Mage mage = new Mage();
            Enemy enemy = new Enemy("Rift Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL);
            CombatEngine engine = new CombatEngine(mage, enemy);

            combatPanel.startBattle(engine);

            assertTrue(combatPanel.getEngine() != null, "Engine bound to combat screen");
            assertTrue(combatPanel.getEngine().getHero() instanceof Mage, "Engine hero is Mage");
            assertEquals(80, combatPanel.getEngine().getHero().getCurrentHp(), "Hero starts at full HP");
            assertEquals(110, combatPanel.getEngine().getEnemy().getCurrentHp(), "Enemy starts at full HP");
            assertEquals(1, combatPanel.getEngine().getTurnCount(), "Starts on turn 1");

            // Buttons state
            assertTrue(combatPanel.getAttackButton().isEnabled(), "Attack enabled at start");
            assertTrue(combatPanel.getDefendButton().isEnabled(), "Defend enabled at start");
            assertTrue(combatPanel.getHealButton().isEnabled(), "Heal enabled at start");
            assertTrue(combatPanel.getSpecialButton().isEnabled(), "Special enabled at start");
            assertEquals("ARCANE NOVA", combatPanel.getSpecialButton().getText(),
                    "Special button displays signature name");
            assertFalse(combatPanel.getViewResultsButton().isVisible(),
                    "View results button initially hidden");

            // Log initialized
            assertTrue(combatPanel.getLogText().contains("Mage steps into the arena"),
                    "Combat log contains opening announcement");
        });
    }

    private static void testButtonActionReachesEngineAndUpdatesLog() {
        runTest("Button Clicks Route to Engine and Update Combat Log", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Target Spawn", 100, 10, 10, Enemy.Behaviour.DEFENSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy, new Random() {
                @Override
                public int nextInt(int bound) { return 90; } // Defend
            });

            panel.startBattle(engine);
            int initialEnemyHp = enemy.getCurrentHp();

            // Simulate user clicking ATTACK button
            panel.getAttackButton().doClick();

            // Verify engine state changed
            assertTrue(enemy.getCurrentHp() < initialEnemyHp, "Enemy HP decreased via engine");
            assertEquals(82, enemy.getCurrentHp(), "Enemy took 18 damage (100 - 18)");

            // Verify log received events
            String log = panel.getLogText();
            assertTrue(log.contains("attacks with their weapon"), "Log records attack action");
            assertTrue(log.contains("Target Spawn takes 18 damage"), "Log records damage dealt");
        });
    }

    private static void testSpecialButtonCooldownLabelAndDisabling() {
        runTest("Special Button Displays Cooldown Counter and Disables", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Test Enemy", 150, 10, 10, Enemy.Behaviour.DEFENSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy, new Random() {
                @Override
                public int nextInt(int bound) { return 90; } // Defend
            });

            panel.startBattle(engine);

            assertEquals("SHIELD SLAM", panel.getSpecialButton().getText(),
                    "Special button text is SHIELD SLAM when ready");
            assertTrue(panel.getSpecialButton().isEnabled(), "Special is enabled when ready");

            // Use Special
            panel.getSpecialButton().doClick();

            // Warrior max cooldown is 3, decrements to 2 at round end
            assertEquals(2, panel.getEngine().getHero().getSpecialCooldown(), "Cooldown is 2");
            assertFalse(panel.getSpecialButton().isEnabled(), "Special button disabled while on cooldown");
            assertEquals("SHIELD SLAM (2)", panel.getSpecialButton().getText(),
                    "Special button displays cooldown countdown: (2)");
        });
    }

    private static void testCombatControlsDisabledOnVictory() {
        runTest("All Combat Controls Disabled on Victory & View Results Appears", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            Enemy fragileEnemy = new Enemy("Fragile", 10, 5, 5, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, fragileEnemy);

            panel.startBattle(engine);

            // Strike lethal blow
            panel.getAttackButton().doClick();

            assertTrue(engine.isGameOver(), "Battle concluded");
            assertTrue(engine.isVictory(), "Outcome is victory");

            // Tactical controls must be disabled
            assertFalse(panel.getAttackButton().isEnabled(), "Attack disabled after victory");
            assertFalse(panel.getDefendButton().isEnabled(), "Defend disabled after victory");
            assertFalse(panel.getHealButton().isEnabled(), "Heal disabled after victory");
            assertFalse(panel.getSpecialButton().isEnabled(), "Special disabled after victory");

            // View Results button must be active
            assertTrue(panel.getViewResultsButton().isVisible(), "View Results visible on game over");
            assertTrue(panel.getViewResultsButton().isEnabled(), "View Results enabled on game over");

            // Verify log preserved
            assertTrue(panel.getLogText().contains("Victory is yours!"), "Log contains victory celebration");
        });
    }

    private static void testCombatControlsDisabledOnDefeat() {
        runTest("Combat Controls Disabled on Defeat", () -> {
            CombatPanel panel = new CombatPanel(null);
            Mage mage = new Mage();
            mage.takeDamage(75); // 5 HP left
            Enemy lethalEnemy = new Enemy("Boss", 100, 30, 5, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(mage, lethalEnemy, new Random() {
                @Override
                public int nextInt(int bound) { return 10; } // Force attack for 30 dmg
            });

            panel.startBattle(engine);

            // Player defends, enemy attacks for 15 dmg, slaying mage
            panel.getDefendButton().doClick();

            assertTrue(engine.isGameOver(), "Battle ended");
            assertFalse(engine.isVictory(), "Player defeated");

            assertFalse(panel.getAttackButton().isEnabled(), "Attack disabled on defeat");
            assertFalse(panel.getDefendButton().isEnabled(), "Defend disabled on defeat");
            assertTrue(panel.getViewResultsButton().isVisible(), "View Results button available");
        });
    }

    private static void testResultPanelReceivesRealOutcomeAndTurns() {
        runTest("ResultPanel Receives Authoritative Outcome and Turn Count", () -> {
            ResultPanel resultPanel = new ResultPanel(null);

            resultPanel.setOutcome(true, 4);
            assertTrue(resultPanel.isVictory(), "ResultPanel outcome set to victory");
            assertEquals(4, resultPanel.getTurns(), "ResultPanel received turn count of 4");

            resultPanel.setOutcome(false, 7);
            assertFalse(resultPanel.isVictory(), "ResultPanel outcome set to defeat");
            assertEquals(7, resultPanel.getTurns(), "ResultPanel received turn count of 7");
        });
    }

    private static void testReplayCreatesCompletelyFreshState() {
        runTest("Replaying Battle Generates Fresh State with No Carryover", () -> {
            CombatPanel panel = new CombatPanel(null);

            // First battle
            Hero h1 = MainFrame.createHero("Warrior");
            Enemy e1 = new Enemy("Spawn 1", 110, 16, 16, Enemy.Behaviour.TACTICAL);
            panel.startBattle(new CombatEngine(h1, e1));
            panel.getEngine().getHero().takeDamage(50);
            panel.getEngine().getHero().startSpecialCooldown();

            // Second battle (simulating clicking champion again)
            Hero h2 = MainFrame.createHero("Warrior");
            Enemy e2 = new Enemy("Spawn 2", 110, 16, 16, Enemy.Behaviour.TACTICAL);
            panel.startBattle(new CombatEngine(h2, e2));
            CombatEngine freshEngine = panel.getEngine();

            assertEquals(120, freshEngine.getHero().getCurrentHp(), "New battle hero starts at full HP");
            assertEquals(0, freshEngine.getHero().getSpecialCooldown(), "New battle hero starts with 0 cooldown");
            assertEquals(110, freshEngine.getEnemy().getCurrentHp(), "New battle enemy starts at full HP");
            assertEquals(1, freshEngine.getTurnCount(), "New battle starts at turn 1");
        });
    }

    private static void testBossBattleInitializationAndLivePhaseState() {
        runTest("Boss Encounter Initializes With 200 HP, Phase I, and Dedicated UI Presentation", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            RiftWarden warden = new RiftWarden();
            CombatEngine engine = new CombatEngine(warrior, warden);

            panel.startBattle(engine);

            assertTrue(panel.getEngine().getEnemy() instanceof RiftWarden, "Engine enemy is RiftWarden");
            assertEquals(200, warden.getMaxHp(), "Warden max HP is 200");
            assertEquals(200, warden.getCurrentHp(), "Warden starts at full 200 HP");
            assertEquals(RiftWarden.Phase.PHASE_I_GUARDIAN, warden.getCurrentPhase(), "Starts in Phase I");

            // Header status displays active Phase I
            String status = panel.getStatusText();
            assertTrue(status.contains("PHASE I"), "Status header displays Phase I");
            assertTrue(status.contains("GUARDIAN"), "Status header displays Phase I name");

            // Opening log announces boss encounter
            String log = panel.getLogText();
            assertTrue(log.contains("BOSS ENCOUNTER: THE RIFT WARDEN"), "Log records warden opening announcement");
        });
    }

    private static void testBossPhaseTransitionsUpdateCombatUI() {
        runTest("Boss Combat Screen Dynamically Updates Header and Log on Phase Transitions", () -> {
            CombatPanel panel = new CombatPanel(null);
            Warrior warrior = new Warrior();
            RiftWarden warden = new RiftWarden();
            // Pre-condition boss to 140 HP (close to 130 threshold)
            warden.takeDamage(60);

            CombatEngine engine = new CombatEngine(warrior, warden, new Random() {
                @Override
                public int nextInt(int bound) { return 0; } // Deterministic AI
            });
            panel.startBattle(engine);

            // Warrior attacks for 18 damage -> warden drops from 140 to 122 (<= 130 -> triggers Phase II)
            panel.getAttackButton().doClick();

            assertEquals(RiftWarden.Phase.PHASE_II_AWAKENING, warden.getCurrentPhase(),
                    "Boss transitioned to Phase II");
            assertTrue(panel.getStatusText().contains("PHASE II: RIFT AWAKENING"),
                    "Status header updated to Phase II");
            assertTrue(panel.getLogText().contains("BOSS PHASE II: RIFT AWAKENING"),
                    "Combat log announced Phase II transition");

            // Now lower warden to 65 HP (close to 60 threshold)
            warden.takeDamage(57); // 122 - 57 = 65 HP
            // Next attack for 18 damage -> warden drops to 47 HP (<= 60 -> triggers Phase III)
            panel.getAttackButton().doClick();

            assertEquals(RiftWarden.Phase.PHASE_III_CATACLYSM, warden.getCurrentPhase(),
                    "Boss transitioned to Phase III");
            assertTrue(panel.getStatusText().contains("PHASE III: CATACLYSM"),
                    "Status header updated to Phase III");
            assertTrue(panel.getLogText().contains("BOSS PHASE III: CATACLYSM"),
                    "Combat log announced Phase III transition");
        });
    }

    private static void testBossBattleReplayResetsToPhaseI() {
        runTest("Replaying Boss Encounter Reinitializes Fresh Phase I and 200 HP", () -> {
            CombatPanel panel = new CombatPanel(null);

            // Encounter 1: Damaged into Phase III
            RiftWarden warden1 = new RiftWarden();
            warden1.takeDamage(160); // 40 HP left -> Phase III
            warden1.checkPhaseTransitions();
            panel.startBattle(new CombatEngine(new Warrior(), warden1));
            assertEquals(RiftWarden.Phase.PHASE_III_CATACLYSM, warden1.getCurrentPhase(), "Damaged into Phase III");

            // Encounter 2: Fresh replay
            RiftWarden warden2 = new RiftWarden();
            panel.startBattle(new CombatEngine(new Warrior(), warden2));

            assertEquals(200, warden2.getCurrentHp(), "New boss starts at 200 HP");
            assertEquals(RiftWarden.Phase.PHASE_I_GUARDIAN, warden2.getCurrentPhase(),
                    "New boss starts at Phase I");
            assertTrue(panel.getStatusText().contains("PHASE I"), "UI displays Phase I for fresh encounter");
        });
    }

    private static void testFullWindowBossNavigation() {
        runTest("MainFrame Encounter Switching and Boss Mode Navigation", () -> {
            MainFrame frame = new MainFrame();

            // Switch to boss encounter mode
            frame.setEncounterType(MainFrame.EncounterType.BOSS);
            assertEquals(MainFrame.EncounterType.BOSS, frame.getEncounterType(), "Encounter type set to BOSS");
            assertTrue(frame.getHeroSelectPanel().getSubtitleText().contains("THE RIFT WARDEN"),
                    "Hero select panel displays boss title");

            // Launch battle with Warrior
            frame.startBattle("Warrior");
            assertTrue(frame.getCombatPanel().getEngine().getEnemy() instanceof RiftWarden,
                    "Combat launched against RiftWarden");
            assertEquals(200, frame.getCombatPanel().getEngine().getEnemy().getMaxHp(), "Boss has 200 HP");

            frame.dispose();
        });
    }

    private static void testFullWindowNavigation() {
        runTest("Full MainFrame Window Navigation and Battle Integration", () -> {
            MainFrame frame = new MainFrame();
            frame.startBattle("Mage");
            assertEquals(80, frame.getCombatPanel().getEngine().getHero().getMaxHp(), "Mage loaded in frame");

            frame.showResult(true, 5);
            assertEquals(5, frame.getResultPanel().getTurns(), "Turns reflected in result screen");
            assertTrue(frame.getResultPanel().isVictory(), "Victory reflected in result screen");

            frame.dispose();
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
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        throw new AssertionError(String.format("%s: Expected <%s> but got <%s>", message, expected, actual));
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(String.format("%s: Expected <%d> but got <%d>", message, expected, actual));
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

