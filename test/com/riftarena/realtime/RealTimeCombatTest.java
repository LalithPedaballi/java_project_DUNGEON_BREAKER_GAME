package com.riftarena.realtime;

import com.riftarena.game.Assassin;
import com.riftarena.game.Enemy;
import com.riftarena.game.Mage;
import com.riftarena.game.Warrior;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless-safe automated test suite for Milestone 1: Real-Time Combat Foundation.
 *
 * Verifies:
 *   1. Player continuous movement, velocity, and arena boundaries.
 *   2. Jumping, gravity, peak trajectory, and ground landing physics.
 *   3. Facing direction updates with movement and attack orientations.
 *   4. Attack cooldowns prevent triggering unlimited attacks every frame.
 *   5. Hitbox AABB overlap and spatial range detection.
 *   6. One hit per attack per target rule during active strike windows.
 *   7. Authoritative damage application and strict HP clamping [0, maxHp].
 *   8. Knockback impulse vectors and directional recoil.
 *   9. Damage invulnerability (i-frames) preventing repeated damage every frame.
 *  10. Enemy AI pursuit, distance tracking, and attack range triggering.
 *  11. Enemy attack telegraph windup, active strike window, and recovery cooldown.
 *  12. Player ability to dodge/avoid enemy strikes via movement and jumping.
 *  13. Hero class distinctions: Warrior melee reach, Mage projectiles, Assassin agility.
 *  14. Dash evasion mechanics and dash cooldowns.
 *  15. Pause state halts physics, combat logic, and timers.
 *  16. Defeat and victory terminal states block further actions.
 *  17. Restart restores completely fresh state with full HP and reset positions.
 */
public class RealTimeCombatTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING REAL-TIME COMBAT FOUNDATION TEST SUITE  ");
        System.out.println("==================================================");

        testPlayerContinuousMovementAndArenaBoundaries();
        testJumpingGravityAndLandingPhysics();
        testFacingDirection();
        testAttackCooldowns();
        testHitboxCollisionDetection();
        testOneHitPerAttackPerTarget();
        testDamageAndHealthClamping();
        testKnockbackPhysics();
        testDamageInvulnerabilityIFrames();
        testEnemyAIPursuitAndAttackRange();
        testEnemyAttackTelegraphAndCooldown();
        testPlayerCanDodgeAvoidEnemyAttack();
        testHeroClassDifferences();
        testDashMechanicAndCooldown();
        testPauseHaltsSimulation();
        testDefeatAndVictoryTerminalStates();
        testRestartResetsCompletelyFreshState();

        System.out.println("==================================================");
        System.out.printf("Real-Time Test Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: One or more real-time combat tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All real-time combat foundation tests passed cleanly.");
            System.exit(0);
        }
    }

    private static void testPlayerContinuousMovementAndArenaBoundaries() {
        runTest("Player Continuous Movement and Arena Boundary Clamping", () -> {
            RealTimeArena arena = new RealTimeArena(1000, 600, 500, 50, 950);
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL), arena);
            RealTimePlayer player = engine.getPlayer();

            float initialX = player.getX();

            // Press and hold move right (D)
            engine.getInputState().setMoveRight(true);
            engine.update(0.05f);
            assertTrue(player.getX() > initialX, "Player moved right with key held");
            assertTrue(player.isFacingRight(), "Player faces right when moving right");
            assertEquals(EntityState.RUNNING, player.getState(), "Player in RUNNING state while moving");

            // Release right key
            engine.getInputState().setMoveRight(false);
            engine.update(0.05f);
            assertEquals(0.0f, player.getVx(), "Player stops horizontal velocity when key released");

            // Move left all the way to arena boundary
            engine.getInputState().setMoveLeft(true);
            for (int i = 0; i < 50; i++) {
                engine.update(0.05f);
            }
            float minAllowedX = arena.getLeftBound() + player.getWidth() * 0.5f;
            assertEquals(minAllowedX, player.getX(), "Player clamped at left arena boundary");
            assertFalse(player.isFacingRight(), "Player faces left when moving left");

            // Move right all the way to right boundary
            engine.getInputState().setMoveLeft(false);
            engine.getInputState().setMoveRight(true);
            for (int i = 0; i < 80; i++) {
                engine.update(0.05f);
            }
            float maxAllowedX = arena.getRightBound() - player.getWidth() * 0.5f;
            assertEquals(maxAllowedX, player.getX(), "Player clamped at right arena boundary");
        });
    }

    private static void testJumpingGravityAndLandingPhysics() {
        runTest("Jumping, Gravity Trajectory, Ground Collision, and Landing", () -> {
            RealTimeArena arena = new RealTimeArena(1000, 600, 500, 50, 950);
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL), arena);
            RealTimePlayer player = engine.getPlayer();

            assertEquals(500.0f, player.getY(), "Player starts on ground plane");
            assertTrue(player.isGrounded(), "Player starts grounded");

            // Trigger jump (W/Space)
            engine.getInputState().setJump(true);
            engine.update(0.016f);
            engine.getInputState().setJump(false);

            assertTrue(player.getVy() < 0, "Player velocity is upward upon jump");
            assertFalse(player.isGrounded(), "Player no longer grounded in mid-air");
            assertEquals(EntityState.JUMPING, player.getState(), "Player in JUMPING state");

            // Step physics forward as player rises into air
            float prevY = player.getY();
            engine.update(0.05f);
            engine.update(0.05f);
            assertTrue(player.getY() < prevY, "Player rises into the air (Y decreases)");

            // Step to peak and start of falling (approx 0.55s total)
            for (int i = 0; i < 9; i++) {
                engine.update(0.05f);
            }
            assertTrue(player.getVy() > 0, "Gravity pulls player downward (Vy > 0)");
            assertEquals(EntityState.FALLING, player.getState(), "Player enters FALLING state");

            // Step until landing on ground (after ~0.9s)
            for (int i = 0; i < 15; i++) {
                engine.update(0.05f);
            }
            assertEquals(500.0f, player.getY(), "Player Y clamped to ground floor (cannot fall through)");
            assertTrue(player.isGrounded(), "Player grounded upon landing");
            assertEquals(0.0f, player.getVy(), "Vertical velocity zeroed on landing");
            assertEquals(EntityState.IDLE, player.getState(), "Player returns to IDLE state on landing");
        });
    }

    private static void testFacingDirection() {
        runTest("Facing Direction Reflects Movement and Directs Attack Hitboxes", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();

            player.setX(400);
            engine.getInputState().setMoveRight(true);
            engine.update(0.02f);
            assertTrue(player.isFacingRight(), "Facing right when moving right");

            // Attack while facing right
            engine.getInputState().setMoveRight(false);
            engine.getInputState().setAttackRequested(true);
            for (int i = 0; i < 3; i++) {
                engine.update(0.04f); // Advance into active hit window (~0.12s)
            }

            Hitbox rightHitbox = player.getActiveAttackHitbox();
            assertTrue(rightHitbox != null, "Attack hitbox active");
            assertTrue(rightHitbox.getMinX() >= player.getX() - 5.0f, "Attack hitbox extends forward (to the right)");

            // Reset and attack facing left
            engine.restart();
            player = engine.getPlayer();
            player.setX(400);
            engine.getInputState().setMoveLeft(true);
            engine.update(0.02f);
            assertFalse(player.isFacingRight(), "Facing left when moving left");

            engine.getInputState().setMoveLeft(false);
            engine.getInputState().setAttackRequested(true);
            for (int i = 0; i < 3; i++) {
                engine.update(0.04f);
            }

            Hitbox leftHitbox = player.getActiveAttackHitbox();
            assertTrue(leftHitbox != null, "Attack hitbox active");
            assertTrue(leftHitbox.getMaxX() <= player.getX() + 5.0f, "Attack hitbox extends forward (to the left)");
        });
    }

    private static void testAttackCooldowns() {
        runTest("Attack Cooldowns Prevent Spamming Attacks Every Frame", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();

            assertEquals(0.0f, player.getAttackCooldownTimer(), "Cooldown initially 0");

            // Trigger attack
            engine.getInputState().setAttackRequested(true);
            engine.update(0.016f);
            assertTrue(player.getAttackCooldownTimer() > 0, "Cooldown engaged upon attack initiation");
            assertEquals(EntityState.ATTACKING, player.getState(), "Player in ATTACKING state");

            float recordedCooldown = player.getAttackCooldownTimer();

            // Next frame: attempt second attack while still on cooldown
            engine.getInputState().setAttackRequested(true);
            engine.update(0.016f);
            assertTrue(player.getAttackCooldownTimer() < recordedCooldown, "Cooldown decreased rather than reset");

            // Release attack key and wait until attack finishes and cooldown elapses
            engine.getInputState().setAttackRequested(false);
            for (int i = 0; i < 25; i++) {
                engine.update(0.02f);
            }
            assertEquals(0.0f, player.getAttackCooldownTimer(), "Cooldown cleared after duration");
            assertEquals(EntityState.IDLE, player.getState(), "Player returned to IDLE");
        });
    }

    private static void testHitboxCollisionDetection() {
        runTest("AABB Hitbox Intersection and Range Filtering", () -> {
            Hitbox boxA = new Hitbox(100, 100, 50, 50);
            Hitbox boxB = new Hitbox(130, 120, 50, 50); // overlapping
            Hitbox boxC = new Hitbox(300, 100, 50, 50); // distant

            assertTrue(boxA.intersects(boxB), "boxA intersects boxB");
            assertTrue(boxB.intersects(boxA), "boxB intersects boxA (symmetric)");
            assertFalse(boxA.intersects(boxC), "boxA does not intersect boxC");

            // Test combatant range intersection
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 100, 10, 10, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            // Place enemy inside Warrior sword reach (x: 200, enemy at 250)
            player.setX(200);
            player.setFacingRight(true);
            enemy.setX(250);

            engine.getInputState().setAttackRequested(true);
            for (int i = 0; i < 3; i++) {
                engine.update(0.04f); // reach active window
            }

            Hitbox attackBox = player.getActiveAttackHitbox();
            assertTrue(attackBox != null, "Active attack hitbox generated");
            assertTrue(attackBox.intersects(enemy.getHurtbox()), "Attack hitbox intersects enemy hurtbox in range");

            // Move enemy far away
            enemy.setX(700);
            assertFalse(attackBox.intersects(enemy.getHurtbox()), "Out-of-range enemy avoids hitbox");
        });
    }

    private static void testOneHitPerAttackPerTarget() {
        runTest("One Hit Per Attack Per Target Rule During Multi-Frame Swings", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            // Position enemy in front of player
            player.setX(300);
            player.setFacingRight(true);
            enemy.setX(350);

            int initialEnemyHp = enemy.getCurrentHp();

            // Initiate attack
            engine.getInputState().setAttackRequested(true);
            engine.update(0.02f);
            engine.getInputState().setAttackRequested(false);

            // Step through 20 frames spanning the entire active swing window (~0.34s)
            for (int i = 0; i < 20; i++) {
                engine.update(0.016f);
            }

            // Verify enemy took exactly one strike of 18 damage (Warrior base attack)
            int expectedHp = initialEnemyHp - 18;
            assertEquals(expectedHp, enemy.getCurrentHp(), "Enemy took exactly 18 damage across the whole swing");
            assertTrue(player.hasHitTarget(enemy), "Player registered hit on target");
        });
    }

    private static void testDamageAndHealthClamping() {
        runTest("Authoritative Damage Application and Strict Health Clamping", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();

            int initialHp = player.getCurrentHp();
            int dealt = player.takeDamage(30, -1, 100);
            assertEquals(30, dealt, "30 damage applied");
            assertEquals(initialHp - 30, player.getCurrentHp(), "HP reduced by 30");

            // Clear i-frames and deal lethal damage exceeding remaining HP
            player.setInvulnerableTimer(0.0f);
            int lethalDealt = player.takeDamage(500, -1, 100);
            assertEquals(initialHp - 30, lethalDealt, "Clamped damage equal to remaining HP");
            assertEquals(0, player.getCurrentHp(), "HP clamped at minimum 0 (never negative)");
            assertFalse(player.isAlive(), "Player is no longer alive");
            assertEquals(EntityState.DEFEATED, player.getState(), "Player entered DEFEATED state");

            // Dead player cannot take further damage or act
            int postDeathDmg = player.takeDamage(50, -1, 100);
            assertEquals(0, postDeathDmg, "Dead combatant cannot take further damage");
        });
    }

    private static void testKnockbackPhysics() {
        runTest("Directional Knockback Impulse Vector Applied on Impact", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimeEnemy enemy = engine.getEnemy();

            // Damage enemy from the left (knockback direction: +1)
            enemy.takeDamage(20, 1.0f, 300.0f);
            assertTrue(enemy.getVx() > 0, "Enemy knocked backward to the right (Vx > 0)");
            assertTrue(enemy.getVy() < 0, "Enemy lifted slightly upward on impact (Vy < 0)");
            assertEquals(EntityState.HURT, enemy.getState(), "Enemy entered HURT state");

            // Damage enemy from the right (knockback direction: -1)
            enemy.setInvulnerableTimer(0.0f); // clear i-frames
            enemy.takeDamage(20, -1.0f, 300.0f);
            assertTrue(enemy.getVx() < 0, "Enemy knocked backward to the left (Vx < 0)");
        });
    }

    private static void testDamageInvulnerabilityIFrames() {
        runTest("Temporary Damage Invulnerability (i-frames) Prevents Rapid Consecutive Damage", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();

            player.takeDamage(25, -1, 150);
            assertTrue(player.isInvulnerable(), "Player granted i-frames after taking damage");
            int hpAfterFirstHit = player.getCurrentHp();

            // Immediate second attack during i-frame window
            int secondHit = player.takeDamage(25, -1, 150);
            assertEquals(0, secondHit, "Attack during i-frames deals 0 damage");
            assertEquals(hpAfterFirstHit, player.getCurrentHp(), "HP unchanged during i-frame protection");

            // Step past i-frame duration (0.40s)
            for (int i = 0; i < 25; i++) {
                engine.update(0.02f);
            }
            assertFalse(player.isInvulnerable(), "i-frames expired");

            // Subsequent attack now deals damage
            int thirdHit = player.takeDamage(25, -1, 150);
            assertEquals(25, thirdHit, "Vulnerable player takes damage once i-frames expire");
            assertEquals(hpAfterFirstHit - 25, player.getCurrentHp(), "HP deducted correctly");
        });
    }

    private static void testEnemyAIPursuitAndAttackRange() {
        runTest("Enemy AI Detects, Pursues, and Attacks Player Within Range", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            player.setX(200);
            enemy.setX(500); // 300px away (within detection range, outside attack range)

            float initialEnemyX = enemy.getX();
            engine.update(0.05f);

            assertTrue(enemy.getX() < initialEnemyX, "Enemy moves left toward the player");
            assertEquals(EntityState.RUNNING, enemy.getState(), "Enemy is RUNNING toward player");
            assertFalse(enemy.isFacingRight(), "Enemy faces left toward player");

            // Step until enemy reaches attack range and attacks
            boolean initiatedAttack = false;
            for (int i = 0; i < 40; i++) {
                engine.update(0.05f);
                if (enemy.getState() == EntityState.ATTACKING) {
                    initiatedAttack = true;
                    break;
                }
            }
            assertTrue(initiatedAttack, "Enemy reached strike range of player and initiated attack");
            assertEquals(EntityState.ATTACKING, enemy.getState(), "Enemy transitioned to ATTACKING in strike range");
            assertTrue(Math.abs(enemy.getX() - player.getX()) <= 85.0f, "Enemy is within strike range of player");
        });
    }

    private static void testEnemyAttackTelegraphAndCooldown() {
        runTest("Enemy Attack Has Readable Windup and Post-Attack Cooldown", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            player.setX(300);
            enemy.setX(350);

            // Trigger enemy attack
            enemy.updateAI(player, engine.getArena(), 0.016f);
            assertEquals(EntityState.ATTACKING, enemy.getState(), "Enemy entered ATTACKING state");

            // Early in animation (progress < 36%): windup phase, hitbox NOT active
            enemy.updateTimers(0.08f);
            Hitbox earlyHitbox = enemy.getActiveAttackHitbox();
            assertEquals(null, earlyHitbox, "No active hitbox during telegraph windup (player can react/dodge)");

            // Advance into active strike window (progress ~45%)
            enemy.updateTimers(0.20f);
            Hitbox activeHitbox = enemy.getActiveAttackHitbox();
            assertTrue(activeHitbox != null, "Hitbox active during strike window");

            // Finish attack and verify recovery cooldown
            for (int i = 0; i < 25; i++) {
                enemy.updateTimers(0.02f);
            }
            assertTrue(enemy.getAttackCooldownTimer() > 0, "Enemy has attack cooldown preventing continuous attacks");
        });
    }

    private static void testPlayerCanDodgeAvoidEnemyAttack() {
        runTest("Player Can Avoid Enemy Attacks by Jumping or Moving Away", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            player.setX(300);
            enemy.setX(360);

            int playerHpStart = player.getCurrentHp();

            // Enemy initiates attack
            enemy.updateAI(player, engine.getArena(), 0.016f);
            assertEquals(EntityState.ATTACKING, enemy.getState(), "Enemy begins attack");

            // Player moves rapidly away to dodge the incoming strike
            engine.getInputState().setMoveLeft(true);
            for (int i = 0; i < 15; i++) {
                engine.update(0.025f);
            }

            // Player retreated out of enemy's attack range
            assertEquals(playerHpStart, player.getCurrentHp(), "Player took 0 damage by moving away before strike hit");
        });
    }

    private static void testHeroClassDifferences() {
        runTest("Distinct Attack Characteristics Across Warrior, Mage, and Assassin", () -> {
            // 1. Warrior
            RealTimeCombatEngine warriorEngine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer warrior = warriorEngine.getPlayer();
            assertEquals("Warrior", warrior.getHeroClass(), "Hero is Warrior");
            assertEquals(120, warrior.getMaxHp(), "Warrior has 120 Max HP");

            // 2. Mage (Ranged projectile casting)
            RealTimeCombatEngine mageEngine = new RealTimeCombatEngine(new Mage(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer mage = mageEngine.getPlayer();
            RealTimeEnemy spawn = mageEngine.getEnemy();
            assertEquals("Mage", mage.getHeroClass(), "Hero is Mage");
            assertEquals(80, mage.getMaxHp(), "Mage has 80 Max HP");

            mage.setX(150);
            spawn.setX(550); // 400px away (beyond melee reach)

            int spawnStartHp = spawn.getCurrentHp();

            // Mage basic attack casts projectile
            mageEngine.getInputState().setAttackRequested(true);
            for (int i = 0; i < 15; i++) {
                mageEngine.update(0.02f);
            }
            assertTrue(mageEngine.getProjectiles().size() > 0, "Mage spawned an Arcane Bolt projectile");

            // Step until projectile reaches distant enemy
            for (int i = 0; i < 35; i++) {
                mageEngine.update(0.025f);
            }
            assertTrue(spawn.getCurrentHp() < spawnStartHp, "Mage projectile traveled across screen and damaged enemy");

            // 3. Assassin (High speed and fast daggers)
            RealTimeCombatEngine assassinEngine = new RealTimeCombatEngine(new Assassin(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer assassin = assassinEngine.getPlayer();
            assertEquals("Assassin", assassin.getHeroClass(), "Hero is Assassin");
            assertEquals(95, assassin.getMaxHp(), "Assassin has 95 Max HP");

            assassinEngine.getInputState().setAttackRequested(true);
            assassinEngine.update(0.02f);
            assertTrue(assassin.getAttackCooldownTimer() <= 0.28f, "Assassin has very fast attack cooldown (0.26s)");
        });
    }

    private static void testDashMechanicAndCooldown() {
        runTest("Dash Evasion Moves Rapidly and Applies Cooldown", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();

            assertEquals(0.0f, player.getDashCooldownTimer(), "Dash initially ready");

            engine.getInputState().setDashRequested(true);
            engine.update(0.016f);

            assertEquals(EntityState.DASHING, player.getState(), "Player entered DASHING state");
            assertTrue(player.isInvulnerable(), "Player has i-frames during evade dash");
            assertTrue(Math.abs(player.getVx()) > 500.0f, "Player moves at high dash speed (>500 px/s)");
            assertTrue(player.getDashCooldownTimer() > 0, "Dash cooldown engaged");

            // Attempting immediate second dash fails
            engine.getInputState().setDashRequested(true);
            engine.update(0.016f);
            assertTrue(player.getDashCooldownTimer() > 0, "Dash cooldown prevents immediate second dash");
        });
    }

    private static void testPauseHaltsSimulation() {
        runTest("Pause State Freezes Physics, Positions, and Cooldowns", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();

            engine.setPaused(true);
            assertTrue(engine.isPaused(), "Engine is paused");

            float xBefore = player.getX();
            float timeBefore = engine.getMatchTime();

            // Attempt movement while paused
            engine.getInputState().setMoveRight(true);
            for (int i = 0; i < 20; i++) {
                engine.update(0.05f);
            }

            assertEquals(xBefore, player.getX(), "Player position unchanged while paused");
            assertEquals(timeBefore, engine.getMatchTime(), "Match timer frozen while paused");

            // Unpause resumes simulation
            engine.setPaused(false);
            engine.update(0.05f);
            assertTrue(player.getX() > xBefore, "Player moves right upon unpausing");
        });
    }

    private static void testDefeatAndVictoryTerminalStates() {
        runTest("Terminal Victory and Defeat States Block Further Actions", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            // Defeat player
            player.takeDamage(999, -1, 100);
            engine.update(0.016f);

            assertTrue(engine.isGameOver(), "Game over flagged on player death");
            assertFalse(engine.isVictory(), "Outcome is defeat");

            float pX = player.getX();
            engine.getInputState().setMoveRight(true);
            engine.update(0.05f);
            assertEquals(pX, player.getX(), "Defeated player cannot move or act");

            // Test Victory
            engine.restart();
            enemy = engine.getEnemy();
            enemy.takeDamage(999, 1, 100);
            engine.update(0.016f);

            assertTrue(engine.isGameOver(), "Game over flagged on enemy death");
            assertTrue(engine.isVictory(), "Outcome is victory");
        });
    }

    private static void testRestartResetsCompletelyFreshState() {
        runTest("Restart Completely Restores Fresh State With Full HP and Reset Positions", () -> {
            RealTimeCombatEngine engine = new RealTimeCombatEngine(new Warrior(), new Enemy("Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
            RealTimePlayer player = engine.getPlayer();
            RealTimeEnemy enemy = engine.getEnemy();

            // Mutate state
            player.takeDamage(50, -1, 100);
            enemy.takeDamage(40, 1, 100);
            player.setX(700);
            enemy.setX(100);

            // Restart
            engine.restart();

            RealTimePlayer freshPlayer = engine.getPlayer();
            RealTimeEnemy freshEnemy = engine.getEnemy();

            assertEquals(freshPlayer.getMaxHp(), freshPlayer.getCurrentHp(), "Player restored to full HP");
            assertEquals(freshEnemy.getMaxHp(), freshEnemy.getCurrentHp(), "Enemy restored to full HP");
            assertEquals(EntityState.IDLE, freshPlayer.getState(), "Player in IDLE state");
            assertEquals(EntityState.IDLE, freshEnemy.getState(), "Enemy in IDLE state");
            assertFalse(engine.isGameOver(), "Game over reset to false");
            assertFalse(engine.isPaused(), "Paused reset to false");
            assertEquals(0.0f, engine.getMatchTime(), "Match timer reset to 0");
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

    private static void assertEquals(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.001f) {
            throw new AssertionError(String.format("%s: Expected <%.3f> but got <%.3f>", message, expected, actual));
        }
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
