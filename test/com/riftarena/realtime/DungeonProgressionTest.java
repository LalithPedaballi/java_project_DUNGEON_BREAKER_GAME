package com.riftarena.realtime;

import com.riftarena.game.Mage;
import com.riftarena.game.Warrior;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless, zero-dependency automated test suite for Dungeon Progression
 * and Encounter Management.
 *
 * Verifies:
 *   1. Initial dungeon state starts in ENTRANCE with full HP, reset timers, and 0 kills.
 *   2. ENCOUNTER_ONE spawns the expected vanguard group (Melee Spawn + Agile Skulker).
 *   3. Next encounters cannot begin before the current wave is completely cleared.
 *   4. Clearing waves unlocks linear stage progression in the exact required sequence.
 *   5. ENCOUNTER_TWO introduces the Abyssal Caster alongside melee skirmishers.
 *   6. Boss cannot spawn early while prior encounters remain uncleared.
 *   7. Boss Gate transition triggers atmosphere shift and opens the Boss Arena.
 *   8. Boss encounter spawns The Rift Warden with full boss health and phase tracking.
 *   9. Boss executes phase transition at <= 50% HP threshold.
 *  10. Defeating the boss completes the dungeon exactly once.
 *  11. Player defeat at any point halts combat and sets the terminal DEFEAT outcome.
 *  12. Restart completely restores fresh state: full HP, reset stage, 0 kills, 0 leftover projectiles.
 *  13. Multi-enemy collision handling: single attack hits multiple targets once each.
 *  14. Enemy projectiles damage the player on collision.
 *  15. Jumping evades the boss ground shockwave slam.
 */
public class DungeonProgressionTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING DUNGEON PROGRESSION & ENCOUNTER SUITE   ");
        System.out.println("==================================================");

        testDungeonStartsInEntranceState();
        testEntranceTransitionToFirstEncounter();
        testNextEncounterCannotBeginBeforePreviousCleared();
        testClearingEncounterOneUnlocksProgressionInOrder();
        testEncounterTwoSpawnsExpectedEnemyCombination();
        testBossCannotSpawnEarly();
        testBossGateTransitionToBossEncounter();
        testBossPhaseTransitionAtHalfHp();
        testBossDefeatCompletesDungeonExactlyOnce();
        testPlayerDefeatHaltsCombatAndSetsOutcome();
        testDungeonRestartRestoresCompletelyCleanNewRun();
        testMultiEnemySimultaneousHitAndIndependentCooldowns();
        testEnemyProjectileDamagesPlayer();
        testJumpEvadesBossGroundShockwave();
        testDashThroughProjectileDeactivatesWithoutDamage();
        testCorneredCasterAttacksInsteadOfFreezing();
        testPauseAndGameOverInputAndVelocityHandling();
        testRestartKeyDuringTerminalStateRestoresDungeon();
        testResultPanelReceivesAuthoritativeDungeonMetrics();
        testEntityStateProgressAndHurtFlash();
        testCombatFeedbackCarriesAttackerAndTargetNames();
        testCombatEffectManagerHitSparksAndDeathDissolution();
        testRestartClearsCooldownsAndUnpausesEngine();
        testClearingWaveOneDoesNotFreezePlayerMovement();
        testBossPhaseTwoEmitsPhaseTransitionCallback();
        testPlayerDefeatPreservesFallenEncounterName();

        System.out.println("==================================================");
        System.out.printf("Dungeon Test Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: One or more dungeon progression tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All dungeon progression tests passed cleanly.");
        }
    }

    private static void testDungeonStartsInEntranceState() {
        runTest("Dungeon Starts in Correct ENTRANCE State With Clean Metrics", () -> {
            Warrior warrior = new Warrior();
            DungeonRun run = new DungeonRun(warrior);

            assertEquals(DungeonStage.ENTRANCE, run.getCurrentStage(), "Initial stage is ENTRANCE");
            assertEquals(0, run.getTotalEnemiesDefeated(), "0 enemies defeated at start");
            assertEquals(0.0f, run.getTotalElapsedTime(), "Elapsed time is 0");
            assertTrue(run.getCombatEngine().getEnemies().isEmpty(), "No enemies spawned during entrance transition");
            assertTrue(run.getPlayer().getCurrentHp() == warrior.getMaxHp(), "Player starts at full HP");
            assertTrue(run.isAnnouncementActive(), "Entrance announcement banner is active");
            assertTrue(run.getAnnouncementTitle().contains("DUNGEON ENTERED"), "Announcement displays dungeon entrance");
        });
    }

    private static void testEntranceTransitionToFirstEncounter() {
        runTest("Entrance Transition Advances to First Encounter After Duration", () -> {
            DungeonRun run = new DungeonRun(new Warrior());

            // 1.0s step (under 2.0s duration) -> still in ENTRANCE
            run.update(1.0f);
            assertEquals(DungeonStage.ENTRANCE, run.getCurrentStage(), "Still in ENTRANCE at 1.0s");

            // Additional 1.1s (total 2.1s >= 2.0s) -> transitions to ENCOUNTER_ONE
            run.update(1.1f);
            assertEquals(DungeonStage.ENCOUNTER_ONE, run.getCurrentStage(), "Advanced to ENCOUNTER_ONE");
            assertEquals(2, run.getCombatEngine().getEnemies().size(), "Encounter 1 spawned exactly 2 enemies");
            assertEquals(EnemyType.RIFT_SPAWN, run.getCombatEngine().getEnemies().get(0).getType(), "First enemy is Rift Spawn");
            assertEquals(EnemyType.RIFT_SKULKER, run.getCombatEngine().getEnemies().get(1).getType(), "Second enemy is Rift Skulker");
            assertEquals(2, run.getEnemiesRemainingInCurrentWave(), "2 enemies remaining in wave 1");
        });
    }

    private static void testNextEncounterCannotBeginBeforePreviousCleared() {
        runTest("Next Encounter Cannot Begin Before Previous Group Is Defeated", () -> {
            DungeonRun run = new DungeonRun(new Warrior());
            // Advance to ENCOUNTER_ONE
            run.update(2.1f);
            assertEquals(DungeonStage.ENCOUNTER_ONE, run.getCurrentStage(), "In ENCOUNTER_ONE");

            // Advance simulation for 10 seconds with enemies alive
            for (int i = 0; i < 100; i++) {
                run.update(0.1f);
            }

            assertEquals(DungeonStage.ENCOUNTER_ONE, run.getCurrentStage(),
                    "Cannot progress past ENCOUNTER_ONE while enemies are alive");
            assertTrue(run.getEnemiesRemainingInCurrentWave() > 0, "Enemies are still alive");
        });
    }

    private static void testClearingEncounterOneUnlocksProgressionInOrder() {
        runTest("Clearing Encounter 1 Unlocks Progression Through Room Transition", () -> {
            DungeonRun run = new DungeonRun(new Warrior());
            run.update(2.1f); // Enter ENCOUNTER_ONE

            // Slay all enemies in wave 1
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) {
                e.takeDamage(999, 1, 0);
            }

            run.update(0.04f);
            assertEquals(DungeonStage.ENCOUNTER_ONE_CLEARED, run.getCurrentStage(),
                    "Transitions to ENCOUNTER_ONE_CLEARED when wave 1 is eliminated");
            assertTrue(run.getAnnouncementTitle().contains("AREA SECURED"), "Announces area secured");

            // Advance through transition duration (1.9s >= 1.8s)
            run.update(1.9f);
            assertEquals(DungeonStage.ENCOUNTER_TWO, run.getCurrentStage(),
                    "Transitions to ENCOUNTER_TWO after room clear transition");
        });
    }

    private static void testEncounterTwoSpawnsExpectedEnemyCombination() {
        runTest("Encounter 2 Spawns Challenging Combination Including Abyssal Caster", () -> {
            DungeonRun run = new DungeonRun(new Warrior());
            run.update(2.1f); // ENCOUNTER_ONE
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) {
                e.takeDamage(999, 1, 0);
            }
            run.update(0.04f); // ENCOUNTER_ONE_CLEARED
            run.update(1.9f);  // ENCOUNTER_TWO

            assertEquals(DungeonStage.ENCOUNTER_TWO, run.getCurrentStage(), "Currently in ENCOUNTER_TWO");
            assertEquals(3, run.getCombatEngine().getEnemies().size(), "Encounter 2 spawned 3 enemies");
            assertEquals(EnemyType.RIFT_SPAWN, run.getCombatEngine().getEnemies().get(0).getType(), "Enemy 1 is Rift Spawn");
            assertEquals(EnemyType.RIFT_SKULKER, run.getCombatEngine().getEnemies().get(1).getType(), "Enemy 2 is Rift Skulker");
            assertEquals(EnemyType.RIFT_CASTER, run.getCombatEngine().getEnemies().get(2).getType(), "Enemy 3 is Rift Caster");
            assertEquals(3, run.getEnemiesRemainingInCurrentWave(), "3 enemies remaining in wave 2");
        });
    }

    private static void testBossCannotSpawnEarly() {
        runTest("Boss Cannot Spawn Early While Prior Encounters Are Active", () -> {
            DungeonRun run = new DungeonRun(new Warrior());
            run.update(2.1f); // ENCOUNTER_ONE
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) {
                e.takeDamage(999, 1, 0);
            }
            run.update(0.04f); // ENCOUNTER_ONE_CLEARED
            run.update(1.9f);  // ENCOUNTER_TWO

            // Slay only 2 out of 3 enemies in Encounter 2
            run.getCombatEngine().getEnemies().get(0).takeDamage(999, 1, 0);
            run.getCombatEngine().getEnemies().get(1).takeDamage(999, 1, 0);

            // Step for 5 seconds
            for (int i = 0; i < 50; i++) {
                run.update(0.1f);
            }

            assertEquals(DungeonStage.ENCOUNTER_TWO, run.getCurrentStage(),
                    "Boss cannot spawn early while 1 enemy remains in Encounter 2");
            assertFalse(run.isBossActive(), "Boss is not active");
        });
    }

    private static void testBossGateTransitionToBossEncounter() {
        runTest("Boss Gate Transition Triggers and Opens Boss Encounter", () -> {
            DungeonRun run = new DungeonRun(new Warrior());
            // Fast forward through wave 1 & wave 2
            run.update(2.1f);
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
            run.update(0.04f);
            run.update(1.9f);
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
            run.update(0.04f);

            assertEquals(DungeonStage.BOSS_GATE, run.getCurrentStage(),
                    "Stage transitions to BOSS_GATE after wave 2 is cleared");
            assertTrue(run.getAnnouncementTitle().contains("ANCIENT SEAL"), "Seal breaking announcement active");

            // Advance through gate duration (2.3s >= 2.2s)
            run.update(2.3f);
            assertEquals(DungeonStage.BOSS_ENCOUNTER, run.getCurrentStage(),
                    "Transitions to BOSS_ENCOUNTER after gate transition");
            assertTrue(run.isBossActive(), "Boss is active");
            assertTrue(run.getBoss() != null, "Boss entity is instantiated");
            assertEquals("The Rift Warden", run.getBoss().getEnemyName(), "Boss is The Rift Warden");
            assertEquals(240, run.getBoss().getMaxHp(), "Boss has 240 HP");
        });
    }

    private static void testBossPhaseTransitionAtHalfHp() {
        runTest("Boss Executes Phase Transition When Reaching 50% HP", () -> {
            RealTimeBoss boss = new RealTimeBoss(500, 520);
            assertEquals(1, boss.getBossPhase(), "Starts in Phase 1");

            // Damage boss to 140 HP (> 120 HP threshold for 240 max HP)
            boss.takeDamage(100, 1, 0);
            boss.updateTimers(0.05f);
            assertEquals(1, boss.getBossPhase(), "Still in Phase 1 above 50% HP");

            // Clear i-frames and damage boss below 50% HP (to 110 HP <= 120 HP)
            boss.setInvulnerableTimer(0.0f);
            boss.takeDamage(30, 1, 0);
            boss.updateTimers(0.05f);
            assertEquals(2, boss.getBossPhase(), "Triggered Phase 2 at <= 50% HP");
            assertTrue(boss.isPhaseTransitionActive(), "Invulnerable phase transition active");
        });
    }

    private static void testBossDefeatCompletesDungeonExactlyOnce() {
        runTest("Defeating the Boss Completes the Dungeon Exactly Once", () -> {
            DungeonRun run = new DungeonRun(new Warrior());
            // Fast forward to boss encounter
            run.update(2.1f);
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
            run.update(0.04f);
            run.update(1.9f);
            for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
            run.update(0.04f);
            run.update(2.3f); // Now in BOSS_ENCOUNTER

            AtomicInteger victoryEvents = new AtomicInteger(0);
            run.addListener(new DungeonRun.DungeonEventListener() {
                @Override
                public void onStageChanged(DungeonStage prev, DungeonStage next) {}
                @Override
                public void onAnnouncement(String title, String subtext, float duration) {}
                @Override
                public void onEnemyKilled(RealTimeEnemy enemy, int totalKills) {}
                @Override
                public void onDungeonConcluded(boolean victory, float elapsedTime, int totalKills) {
                    if (victory) victoryEvents.incrementAndGet();
                }
            });

            // Slay the boss
            run.getBoss().takeDamage(999, 1, 0);
            run.update(0.05f);

            assertEquals(DungeonStage.VICTORY, run.getCurrentStage(), "Stage is VICTORY");
            assertTrue(run.isVictory(), "Dungeon marked as Victory");
            assertTrue(run.isGameOver(), "Game marked as Over");
            assertEquals(1, victoryEvents.get(), "Victory event emitted exactly once");

            // Further updates must not re-emit or advance
            run.update(1.0f);
            run.update(2.0f);
            assertEquals(1, victoryEvents.get(), "No duplicate victory events on subsequent frames");
        });
    }

    private static void testPlayerDefeatHaltsCombatAndSetsOutcome() {
        runTest("Player Defeat Stops Active Combat and Transitions to DEFEAT", () -> {
            Warrior warrior = new Warrior();
            DungeonRun run = new DungeonRun(warrior);
            run.update(2.1f); // ENCOUNTER_ONE

            AtomicBoolean defeatEmitted = new AtomicBoolean(false);
            run.addListener(new DungeonRun.DungeonEventListener() {
                @Override
                public void onStageChanged(DungeonStage prev, DungeonStage next) {}
                @Override
                public void onAnnouncement(String title, String subtext, float duration) {}
                @Override
                public void onEnemyKilled(RealTimeEnemy enemy, int totalKills) {}
                @Override
                public void onDungeonConcluded(boolean victory, float elapsedTime, int totalKills) {
                    if (!victory) defeatEmitted.set(true);
                }
            });

            // Lethal damage to player
            run.getPlayer().takeDamage(999, -1, 0);
            run.update(0.05f);

            assertEquals(DungeonStage.DEFEAT, run.getCurrentStage(), "Stage transitions to DEFEAT");
            assertTrue(run.isDefeat(), "Dungeon run marked as Defeat");
            assertTrue(run.isGameOver(), "Game marked as Over");
            assertTrue(defeatEmitted.get(), "Defeat event was emitted to listeners");
        });
    }

    private static void testDungeonRestartRestoresCompletelyCleanNewRun() {
        runTest("Restart Completely Restores Clean New Run Without Leftover State", () -> {
            Warrior warrior = new Warrior();
            DungeonRun run = new DungeonRun(warrior);
            run.update(2.1f);
            // Defeat 1 enemy and take player damage
            run.getCombatEngine().getEnemies().get(0).takeDamage(999, 1, 0);
            run.getPlayer().takeDamage(40, -1, 0);
            run.update(0.1f);

            assertTrue(run.getTotalEnemiesDefeated() > 0, "Kills accumulated");
            assertTrue(run.getPlayer().getCurrentHp() < warrior.getMaxHp(), "Player HP was reduced");

            // Execute full restart
            run.restart();

            assertEquals(DungeonStage.ENTRANCE, run.getCurrentStage(), "Reset to ENTRANCE stage");
            assertEquals(warrior.getMaxHp(), run.getPlayer().getCurrentHp(), "Player HP fully restored");
            assertEquals(0, run.getTotalEnemiesDefeated(), "Defeated count reset to 0");
            assertEquals(0.0f, run.getTotalElapsedTime(), "Elapsed time reset to 0");
            assertTrue(run.getCombatEngine().getProjectiles().isEmpty(), "All projectiles cleared");
            assertTrue(run.getCombatEngine().getEnemies().isEmpty(), "Enemies reset for entrance sequence");
            assertFalse(run.isGameOver(), "Game is not over");
        });
    }

    private static void testMultiEnemySimultaneousHitAndIndependentCooldowns() {
        runTest("Multiple Enemies in Arena Can Be Struck Simultaneously by One Cleave", () -> {
            Warrior warrior = new Warrior();
            RealTimeCombatEngine engine = new RealTimeCombatEngine(warrior, null);
            RealTimePlayer player = engine.getPlayer();

            RealTimeEnemy enemyA = new RealTimeEnemy(EnemyType.RIFT_SPAWN, 260, 520);
            RealTimeEnemy enemyB = new RealTimeEnemy(EnemyType.RIFT_SPAWN, 290, 520);
            engine.addEnemy(enemyA);
            engine.addEnemy(enemyB);

            player.setX(200);
            player.setFacingRight(true);

            // Execute basic attack cleave
            engine.getInputState().onKeyPressed(java.awt.event.KeyEvent.VK_J);
            engine.update(0.016f); // Triggers ATTACKING
            engine.getInputState().onKeyReleased(java.awt.event.KeyEvent.VK_J);

            // Step into active strike window (~0.12s)
            for (int i = 0; i < 3; i++) {
                engine.update(0.04f);
            }

            assertTrue(enemyA.getCurrentHp() < enemyA.getMaxHp(), "Enemy A took damage from cleave");
            assertTrue(enemyB.getCurrentHp() < enemyB.getMaxHp(), "Enemy B took damage from same cleave");

            // Advance through remaining frames of the swing; verify neither takes double damage from same swing
            int hpA = enemyA.getCurrentHp();
            int hpB = enemyB.getCurrentHp();
            for (int i = 0; i < 5; i++) {
                engine.update(0.04f);
            }
            assertEquals(hpA, enemyA.getCurrentHp(), "Enemy A took damage only once during multi-frame swing");
            assertEquals(hpB, enemyB.getCurrentHp(), "Enemy B took damage only once during multi-frame swing");
        });
    }

    private static void testEnemyProjectileDamagesPlayer() {
        runTest("Enemy Projectiles Travel and Damage Player on Collision", () -> {
            Warrior warrior = new Warrior();
            RealTimeCombatEngine engine = new RealTimeCombatEngine(warrior, null);
            RealTimePlayer player = engine.getPlayer();
            player.setX(200);
            player.setY(520);

            // Spawn enemy void bolt moving toward player at x=200
            Projectile voidBolt = new Projectile(260, 520 - 40, -300.0f, 0.0f, 15.0f, 16, 120.0f, false);
            engine.getProjectiles().add(voidBolt);

            int startHp = player.getCurrentHp();

            // Step projectile until collision
            for (int i = 0; i < 10; i++) {
                engine.update(0.03f);
                if (!voidBolt.isActive()) break;
            }

            assertFalse(voidBolt.isActive(), "Projectile deactivated upon hitting player");
            assertEquals(startHp - 16, player.getCurrentHp(), "Player took 16 damage from enemy void bolt");
        });
    }

    private static void testJumpEvadesBossGroundShockwave() {
        runTest("Jumping Over Ground Shockwave Evades Boss Shockwave Slam", () -> {
            Warrior warrior = new Warrior();
            RealTimeCombatEngine engine = new RealTimeCombatEngine(warrior, null);
            RealTimePlayer player = engine.getPlayer();
            float groundY = engine.getArena().getGroundY();
            player.setX(200);
            player.setY(groundY);

            // Case 1: Player on the ground gets hit by shockwave
            Projectile groundWave1 = new Projectile(250, groundY - 18.0f, -280.0f, 0.0f, 20.0f, 30, 250.0f, false);
            engine.getProjectiles().add(groundWave1);
            int initialHp = player.getCurrentHp();

            for (int i = 0; i < 10; i++) {
                engine.update(0.03f);
                if (!groundWave1.isActive()) break;
            }
            assertFalse(groundWave1.isActive(), "Ground wave 1 collided with grounded player");
            assertEquals(initialHp - 30, player.getCurrentHp(), "Grounded player took shockwave damage");

            // Case 2: Player JUMPS into the air (feet well above floor-level wave)
            player.setInvulnerableTimer(0.0f);
            player.setY(groundY - 110.0f); // In mid-air above the wave
            player.setVy(0.0f);

            int airHp = player.getCurrentHp();
            Projectile groundWave2 = new Projectile(250, groundY - 18.0f, -280.0f, 0.0f, 20.0f, 30, 250.0f, false);
            engine.getProjectiles().add(groundWave2);

            for (int i = 0; i < 10; i++) {
                engine.update(0.03f);
                player.setY(groundY - 110.0f); // hold in air for test frame sampling
            }

            assertEquals(airHp, player.getCurrentHp(), "Airborne jumping player avoided all ground shockwave damage");
        });
    }

    private static void testDashThroughProjectileDeactivatesWithoutDamage() {
        runTest("Dashing Through Enemy Projectile Safely Deactivates It Without Damage", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), null);
            RealTimePlayer player = engine.getPlayer();
            player.setX(300.0f);
            player.setY(engine.getArena().getGroundY());
            int hpBefore = player.getCurrentHp();

            // Spawn incoming enemy projectile right in front of player
            Projectile enemyProj = new Projectile(340.0f, player.getY() - 40.0f, -300.0f, 0.0f, 15.0f, 25, 150.0f, false);
            engine.getProjectiles().add(enemyProj);

            // Initiate player dash (provides i-frames)
            engine.getInputState().setDashRequested(true);
            engine.update(0.016f);
            assertTrue(player.isInvulnerable(), "Player is invulnerable during dash");

            // Advance engine
            engine.update(0.04f);

            assertFalse(enemyProj.isActive(), "Projectile was deactivated upon collision with dashed player");
            assertEquals(hpBefore, player.getCurrentHp(), "Player took 0 damage while dashing through projectile");

            // Advance time past dash i-frames; ensure deactivated projectile does not linger and hit player
            for (int i = 0; i < 15; i++) {
                engine.update(0.04f);
            }
            assertEquals(hpBefore, player.getCurrentHp(), "No delayed damage after dash i-frames expire");
        });
    }

    private static void testCorneredCasterAttacksInsteadOfFreezing() {
        runTest("Cornered Abyssal Caster Casts Spell Instead of Freezing at Arena Boundary", () -> {
            RealTimeArena arena = new RealTimeArena();
            // Corner caster at the left wall
            float leftWallX = arena.getLeftBound() + 50.0f;
            RealTimeEnemy caster = new RealTimeEnemy(null, EnemyType.RIFT_CASTER, leftWallX, arena.getGroundY());
            RealTimePlayer player = new RealTimePlayer(new Warrior(), leftWallX + 80.0f, arena.getGroundY());

            java.util.List<Projectile> projectiles = new java.util.ArrayList<>();

            // Update Caster AI while cornered against left wall
            caster.updateAI(player, arena, projectiles, 0.05f);

            // Caster cannot retreat left (wall blocked), so it must stand ground and initiate attack
            assertEquals(EntityState.ATTACKING, caster.getState(), "Cornered caster initiates cast attack rather than freezing");
            assertEquals(0.0f, caster.getVx(), "Caster stands ground with zero velocity when initiating cast");
        });
    }

    private static void testPauseAndGameOverInputAndVelocityHandling() {
        runTest("Pause and Terminal States Prevent Player Motion and Action Triggers", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), null);
            RealTimePlayer player = engine.getPlayer();

            // 1. Pause freezes motion and cancels attack triggers
            engine.setPaused(true);
            engine.getInputState().setAttackRequested(true);
            engine.getInputState().setMoveRight(true);
            player.setVx(150.0f);

            engine.update(0.05f);

            assertEquals(0.0f, player.getVx(), "Player velocity is clamped to 0 while paused");
            assertFalse(engine.getInputState().isAttackRequested(), "Attack request cleared during pause");
            assertEquals(EntityState.IDLE, player.getState(), "Player does not transition to attacking while paused");

            // 2. Terminal Defeat state discards all input and locks velocity
            engine.setPaused(false);
            player.takeDamage(999, 1, 0); // Defeat player
            engine.update(0.05f);

            assertTrue(engine.isGameOver(), "Combat engine flagged game over on defeat");
            engine.getInputState().setMoveRight(true);
            engine.getInputState().setAttackRequested(true);
            engine.getInputState().setSpecialRequested(true);
            player.setVx(200.0f);

            engine.update(0.05f);

            assertEquals(0.0f, player.getVx(), "Player velocity remains 0 after defeat");
            assertFalse(engine.getInputState().isMoveRight(), "Move input is discarded after defeat");
            assertFalse(engine.getInputState().isAttackRequested(), "Attack input is discarded after defeat");
            assertEquals(EntityState.DEFEATED, player.getState(), "Player remains in DEFEATED state");
        });
    }

    private static void testRestartKeyDuringTerminalStateRestoresDungeon() {
        runTest("Pressing [R] Restart Key in Terminal State Fully Resets Dungeon Run", () -> {
            DungeonRun run = new DungeonRun(new Warrior());

            // Advance through encounters to defeat
            run.update(2.5f); // in Encounter 1
            run.getPlayer().takeDamage(999, 1, 0); // kill player
            run.update(0.05f);

            assertEquals(DungeonStage.DEFEAT, run.getCurrentStage(), "Run is in DEFEAT");
            assertTrue(run.isGameOver(), "Run is in game over state");

            // Player hits [R] key to restart
            run.getCombatEngine().getInputState().onKeyPressed(java.awt.event.KeyEvent.VK_R);
            run.update(0.05f);

            assertEquals(DungeonStage.ENTRANCE, run.getCurrentStage(), "Dungeon cleanly restarted to ENTRANCE");
            assertFalse(run.isGameOver(), "Run is no longer in game over");
            assertTrue(run.getPlayer().getCurrentHp() == run.getPlayer().getMaxHp(), "Player health restored to full");
            assertEquals(0, run.getTotalEnemiesDefeated(), "Defeated count reset to 0");
            assertEquals(0.0f, run.getTotalElapsedTime(), "Timer reset to 0");
            assertTrue(run.getCombatEngine().getEnemies().isEmpty(), "Enemies reset for entrance stage");
        });
    }

    private static void testResultPanelReceivesAuthoritativeDungeonMetrics() {
        runTest("ResultPanel Receives and Visualizes Authoritative Clear Time, Kills, and Rank", () -> {
            com.riftarena.ui.ResultPanel panel = new com.riftarena.ui.ResultPanel(null);

            // Victory outcome with 52 seconds, 4 kills, S-Rank
            panel.setOutcome(true, 52, 4, "S-RANK (LEGENDARY HUNTER)");

            assertTrue(panel.isVictory(), "Panel reports victory");
            assertEquals(52, panel.getElapsedSeconds(), "Elapsed seconds stored correctly");
            assertEquals(4, panel.getEnemiesDefeated(), "Enemies defeated stored correctly");
            assertEquals("S-RANK (LEGENDARY HUNTER)", panel.getPerformanceRank(), "Performance rank stored correctly");

            // Defeat outcome with 18 seconds, 1 kill, D-Rank
            panel.setOutcome(false, 18, 1, "D-RANK (FALLEN)");
            assertFalse(panel.isVictory(), "Panel reports defeat");
            assertEquals(18, panel.getElapsedSeconds(), "Defeat elapsed seconds stored correctly");
            assertEquals(1, panel.getEnemiesDefeated(), "Defeat kills stored correctly");
            assertEquals("D-RANK (FALLEN)", panel.getPerformanceRank(), "Defeat rank stored correctly");
        });
    }

    private static void testEntityStateProgressAndHurtFlash() {
        runTest("Entity State Progress & Hurt Flash Lifecycle Behaves Correctly Across States", () -> {
            RealTimePlayer player = new RealTimePlayer(new Warrior(), 200, 500);

            // 1. Idle state
            assertEquals(0.0f, player.getStateProgress(), "State progress starts at 0 for idle");
            assertFalse(player.isHurt(), "Player is not hurt initially");

            // 2. Taking damage triggers hurt state and hurtTimer
            player.takeDamage(20, -1.0f, 150.0f);
            assertTrue(player.isHurt(), "Player is flagged as hurt after taking damage");
            assertTrue(player.getHurtTimer() > 0.0f, "Hurt timer is positive");
            assertTrue(player.getStateProgress() >= 0.0f && player.getStateProgress() <= 1.0f,
                    "State progress returns normalized hurt curve value");

            // 3. Advancing timers completes hurt state
            player.updateTimers(0.30f);
            assertFalse(player.isHurt(), "Player recovers from hurt state after timer elapses");
        });
    }

    private static void testCombatFeedbackCarriesAttackerAndTargetNames() {
        runTest("CombatFeedbackListener Accurately Receives Attacker and Target Identity", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), null);
            RealTimeEnemy skulker = new RealTimeEnemy(null, EnemyType.RIFT_SKULKER, 250, engine.getArena().getGroundY());
            engine.addEnemy(skulker);

            AtomicBoolean hitVerified = new AtomicBoolean(false);
            engine.addFeedbackListener(new RealTimeCombatEngine.CombatFeedbackListener() {
                @Override
                public void onDamageDealt(String attackerName, String targetName, float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
                    if ("Warrior".equals(attackerName) && "Rift Skulker".equals(targetName) && !isPlayerTarget) {
                        hitVerified.set(true);
                    }
                }

                @Override
                public void onDamageDealt(float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
                }

                @Override
                public void onAttackPerformed(String attacker, boolean isSpecial) {
                }

                @Override
                public void onGameOver(boolean victory) {
                }
            });

            // Player attacks Skulker
            engine.getPlayer().setX(220);
            engine.getInputState().setAttackRequested(true);
            for (int i = 0; i < 10; i++) {
                engine.update(0.03f);
                if (hitVerified.get()) break;
            }

            assertTrue(hitVerified.get(), "Feedback listener received exact attacker 'Warrior' and target 'Rift Skulker'");
        });
    }

    private static void testCombatEffectManagerHitSparksAndDeathDissolution() {
        runTest("CombatEffectManager Successfully Manages Hit Sparks and Dissolution Shakes", () -> {
            com.riftarena.ui.render.CombatEffectManager manager = new com.riftarena.ui.render.CombatEffectManager();

            assertEquals(0, manager.getActiveEffectCount(), "Effect manager starts empty");

            // Spawn hit sparks
            manager.spawnHitSparks(300, 400, java.awt.Color.ORANGE, 1.0f);
            assertEquals(1, manager.getActiveEffectCount(), "Hit sparks registered as active effect");

            // Spawn death dissolution
            manager.spawnDeathDissolution(350, 400, java.awt.Color.MAGENTA, true);
            assertEquals(2, manager.getActiveEffectCount(), "Death dissolution registered as active effect");
            assertTrue(manager.isScreenShakeActive(), "Screen shake active after boss death dissolution");

            // Advancing time updates effects and calculates shake offsets
            manager.update(0.016f);
            assertTrue(manager.getShakeOffsetX() != 0.0f || manager.getShakeOffsetY() != 0.0f,
                    "Screen shake offset active during frame update");
            assertTrue(manager.getActiveEffectCount() >= 1, "Effects persist through active animation frames");

            // Clearing all wipes effects and reset shake
            manager.clearAll();
            assertEquals(0, manager.getActiveEffectCount(), "Effects cleared completely");
            assertEquals(0.0f, manager.getShakeOffsetX(), "Screen shake offset reset to 0");
        });
    }

    private static void testRestartClearsCooldownsAndUnpausesEngine() {
        runTest("Restart Clears Cooldowns, Buffers, and Unpauses Engine", () -> {
            Warrior warrior = new Warrior();
            DungeonRun run = new DungeonRun(warrior);
            RealTimeCombatEngine engine = run.getCombatEngine();
            RealTimePlayer player = run.getPlayer();

            // Set cooldowns and pause state
            engine.setPaused(true);
            player.setAttackCooldownTimer(1.5f);
            player.setSpecialCooldownTimer(2.5f);
            player.setDashCooldownTimer(1.0f);
            player.takeDamage(40, 1.0f, 100.0f);

            // Execute restart
            run.restart();

            assertFalse(engine.isPaused(), "Engine is unpaused after restart");
            assertFalse(engine.isGameOver(), "Engine is not game over after restart");
            assertEquals(0.0f, player.getAttackCooldownTimer(), "Attack cooldown reset to 0");
            assertEquals(0.0f, player.getSpecialCooldownTimer(), "Special cooldown reset to 0");
            assertEquals(0.0f, player.getDashCooldownTimer(), "Dash cooldown reset to 0");
            assertEquals(warrior.getMaxHp(), player.getCurrentHp(), "Player HP restored to max");
        });
    }

    private static void testClearingWaveOneDoesNotFreezePlayerMovement() {
        runTest("Clearing Wave 1 Does Not Prematurely Freeze Player in Room Transitions", () -> {
            Warrior warrior = new Warrior();
            DungeonRun run = new DungeonRun(warrior);
            RealTimeCombatEngine engine = run.getCombatEngine();
            RealTimePlayer player = run.getPlayer();

            // Transition from entrance to Encounter 1
            run.update(2.1f);
            assertEquals(DungeonStage.ENCOUNTER_ONE, run.getCurrentStage(), "In Encounter 1");
            assertFalse(engine.isGameOver(), "Combat engine is not game over");

            // Defeat all enemies in encounter 1
            for (RealTimeEnemy enemy : engine.getEnemies()) {
                enemy.takeDamage(enemy.getMaxHp(), 0, 0);
            }

            // Step dungeon run: should advance to ENCOUNTER_ONE_CLEARED
            run.update(0.05f);
            assertEquals(DungeonStage.ENCOUNTER_ONE_CLEARED, run.getCurrentStage(), "Transitioned to ENCOUNTER_ONE_CLEARED");
            assertFalse(engine.isGameOver(), "Engine is not game over in wave transition room");

            // Player can still move during wave clear transition
            engine.getInputState().onKeyPressed(java.awt.event.KeyEvent.VK_D);
            run.update(0.05f);
            assertTrue(player.getVx() > 0, "Player can move right during transition room (not frozen)");
            engine.getInputState().onKeyReleased(java.awt.event.KeyEvent.VK_D);
        });
    }

    private static void testBossPhaseTwoEmitsPhaseTransitionCallback() {
        runTest("Boss Phase 2 Cataclysm Triggers Announcement and Event Emission", () -> {
            Warrior warrior = new Warrior();
            RealTimeCombatEngine engine = new RealTimeCombatEngine(warrior, null);
            DungeonRun run = new DungeonRun(warrior, engine);

            AtomicBoolean phase2Announced = new AtomicBoolean(false);
            engine.addFeedbackListener(new RealTimeCombatEngine.CombatFeedbackListener() {
                @Override
                public void onDamageDealt(float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {}
                @Override
                public void onAttackPerformed(String attacker, boolean isSpecial) {}
                @Override
                public void onGameOver(boolean victory) {}
                @Override
                public void onBossPhaseTransition(RealTimeBoss boss, int newPhase) {
                    if (newPhase == 2) {
                        phase2Announced.set(true);
                    }
                }
            });

            run.startBossDirectly();
            assertEquals(DungeonStage.BOSS_ENCOUNTER, run.getCurrentStage(), "In Boss Encounter");

            RealTimeBoss boss = (RealTimeBoss) engine.getEnemies().get(0);
            // Deal damage down to <= 50% HP
            boss.takeDamage(boss.getMaxHp() / 2 + 10, 0, 0);

            // Step engine to update timers and evaluate phase transition
            engine.update(0.05f);

            assertTrue(boss.getBossPhase() >= 2, "Boss transitioned to phase 2");
            assertTrue(phase2Announced.get(), "Boss phase transition event was emitted");
            assertTrue(run.getAnnouncementTitle().contains("CATACLYSM"), "Announcement updated with CATACLYSM phase warning");
        });
    }

    private static void testPlayerDefeatPreservesFallenEncounterName() {
        runTest("Player Defeat Correctly Announces the Stage Where the Hero Fell", () -> {
            Warrior warrior = new Warrior();
            DungeonRun run = new DungeonRun(warrior);

            // Advance through entrance (2.0s) and wave 1 clear to get to Encounter 2
            run.update(2.1f);
            assertEquals(DungeonStage.ENCOUNTER_ONE, run.getCurrentStage(), "In Encounter 1");
            for (RealTimeEnemy enemy : run.getCombatEngine().getEnemies()) {
                enemy.takeDamage(enemy.getMaxHp(), 0, 0);
            }
            // Clear duration is 1.8s
            run.update(1.9f);
            assertEquals(DungeonStage.ENCOUNTER_TWO, run.getCurrentStage(), "Reached Encounter 2");

            // Kill player in Encounter 2
            run.getPlayer().takeDamage(run.getPlayer().getMaxHp(), 0, 0);
            run.update(0.05f);

            assertEquals(DungeonStage.DEFEAT, run.getCurrentStage(), "Dungeon reached DEFEAT stage");
            assertTrue(run.getAnnouncementSubtext().contains("SHADOW GUARD"),
                    "Announcement subtext records 'SHADOW GUARD', got: " + run.getAnnouncementSubtext());
        });
    }

    // ------------------------------------------------------------------
    // Test Assertion Utilities
    // ------------------------------------------------------------------

    private static void runTest(String name, TestRunnable action) {
        testsRun++;
        try {
            action.run();
            testsPassed++;
            System.out.println("  [PASS] " + name);
        } catch (Throwable t) {
            testsFailed++;
            System.err.println("  [FAIL] " + name + ": " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    @FunctionalInterface
    interface TestRunnable {
        void run() throws Throwable;
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

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(String.format("%s: expected <%s> but was <%s>", message, expected, actual));
    }
}
