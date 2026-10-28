package com.riftarena.game;

import java.util.List;
import java.util.Random;

/**
 * Headless, zero-dependency unit and integration test suite for {@link CombatEngine}
 * and the {@code com.riftarena.game} character model.
 *
 * Can be compiled and executed directly from the command line:
 *   javac -d out $(find src test -name "*.java")
 *   java -cp out com.riftarena.game.CombatEngineTest
 *
 * Exits with status code 0 if all tests pass; exits with status code 1 if any test fails.
 */
public class CombatEngineTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING RIFT ARENA COMBAT ENGINE TEST SUITE    ");
        System.out.println("==================================================");

        testHeroInitialization();
        testDamageAndHpClamping();
        testDefenseReductionAndConsumption();
        testHealingAndMaxHpClamping();
        testWarriorSpecialShieldSlam();
        testMageSpecialArcaneNovaDefensePierce();
        testAssassinSpecialExecuteThreshold();
        testSpecialCooldownTiming();
        testMageSpecialCooldownExactSequence();
        testAssassinSpecialCooldownExactSequence();
        testEnemyDefenseExpiresWhenPlayerDoesNotAttack();
        testWarriorShieldSlamDefenseLifecycle();
        testEnemyAggressiveBehaviour();
        testEnemyDefensiveBehaviourHeal();
        testEnemyTacticalBehaviourAgainstDefend();
        testEnemyTacticalFullCoverage();
        testReproducibleRandomizedDecisions();
        testVictoryPreventsEnemyCounterattack();
        testHeroDefeatCondition();
        testInvalidActionAndPhaseExceptions();
        testInvalidActionDoesNotMutateState();
        testDeadCharacterCannotAct();
        testEventOrderingAndTerminalConsistency();
        testCombatEventsValuesAndImmutability();
        testRiftWardenStartsInPhaseIWith200Hp();
        testRiftWardenPhaseTransitionsAndThresholds();
        testRiftWardenEachTransitionOccursOnlyOnce();
        testRiftWardenSingleAttackCrossesBothThresholds();
        testRiftWardenDamageAndBehaviorAcrossPhases();
        testRiftWardenCannotActAfterDefeat();
        testRiftWardenTerminalStateAndVictory();

        System.out.println("==================================================");
        System.out.printf("Test Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: One or more combat engine tests failed.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All combat engine tests passed cleanly.");
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    private static void testHeroInitialization() {
        runTest("Hero Initialization & Baseline Stats", () -> {
            Warrior warrior = new Warrior();
            assertEquals(120, warrior.getMaxHp(), "Warrior Max HP");
            assertEquals(120, warrior.getCurrentHp(), "Warrior Current HP");
            assertEquals(18, warrior.getBaseAttack(), "Warrior Base Attack");
            assertEquals(20, warrior.getHealAmount(), "Warrior Heal Amount");
            assertTrue(warrior.isAlive(), "Warrior is alive");
            assertFalse(warrior.isDefending(), "Warrior starts without shield");
            assertTrue(warrior.isSpecialReady(), "Warrior special starts ready");

            Mage mage = new Mage();
            assertEquals(80, mage.getMaxHp(), "Mage Max HP");
            assertEquals(25, mage.getBaseAttack(), "Mage Base Attack");

            Assassin assassin = new Assassin();
            assertEquals(95, assassin.getMaxHp(), "Assassin Max HP");
            assertEquals(22, assassin.getBaseAttack(), "Assassin Base Attack");
        });
    }

    private static void testDamageAndHpClamping() {
        runTest("Damage & HP Clamping (Minimum 0)", () -> {
            Warrior warrior = new Warrior();
            warrior.takeDamage(30);
            assertEquals(90, warrior.getCurrentHp(), "HP reduced by 30");

            warrior.takeDamage(200); // Exceeds remaining 90 HP
            assertEquals(0, warrior.getCurrentHp(), "HP clamped to 0");
            assertFalse(warrior.isAlive(), "Warrior is dead at 0 HP");

            // Further damage to dead combatant has no effect
            warrior.takeDamage(50);
            assertEquals(0, warrior.getCurrentHp(), "HP remains 0");
        });
    }

    private static void testDefenseReductionAndConsumption() {
        runTest("Defense 50% Reduction & Consumption", () -> {
            Warrior warrior = new Warrior();
            warrior.setDefending(true);
            assertTrue(warrior.isDefending(), "Warrior entered defense");

            // Enemy hits for 20 damage -> should be mitigated to 10
            warrior.takeDamage(20);
            assertEquals(110, warrior.getCurrentHp(), "Damage halved (20 / 2 = 10)");
            assertFalse(warrior.isDefending(), "Defense consumed after mitigating attack");

            // Second hit does full unmitigated damage
            warrior.takeDamage(20);
            assertEquals(90, warrior.getCurrentHp(), "Full 20 damage applied on second hit");
        });
    }

    private static void testHealingAndMaxHpClamping() {
        runTest("Healing & Maximum HP Clamping", () -> {
            Mage mage = new Mage();
            mage.takeDamage(30);
            assertEquals(50, mage.getCurrentHp(), "Mage down to 50 HP");

            mage.heal(15);
            assertEquals(65, mage.getCurrentHp(), "Mage healed to 65 HP");

            mage.heal(50); // Exceeds 80 Max HP
            assertEquals(80, mage.getCurrentHp(), "HP clamped to Max HP (80)");

            // Dead combatant cannot be healed
            mage.takeDamage(80);
            assertEquals(0, mage.getCurrentHp(), "Mage dead");
            mage.heal(25);
            assertEquals(0, mage.getCurrentHp(), "Dead mage cannot heal");
        });
    }

    private static void testWarriorSpecialShieldSlam() {
        runTest("Warrior Special (Shield Slam: 22 dmg + Defend)", () -> {
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Test Enemy", 100, 10, 10, Enemy.Behaviour.AGGRESSIVE);

            int damage = warrior.executeSpecial(enemy);
            assertEquals(22, damage, "Shield Slam deals 22 damage");
            assertEquals(78, enemy.getCurrentHp(), "Enemy HP reduced from 100 to 78");
            assertTrue(warrior.isDefending(), "Warrior automatically enters defense");
        });
    }

    private static void testMageSpecialArcaneNovaDefensePierce() {
        runTest("Mage Special (Arcane Nova: 40 dmg, Pierces Defense)", () -> {
            Mage mage = new Mage();
            Enemy enemy = new Enemy("Test Enemy", 100, 10, 10, Enemy.Behaviour.DEFENSIVE);
            enemy.setDefending(true);
            assertTrue(enemy.isDefending(), "Enemy was defending");

            int damage = mage.executeSpecial(enemy);
            assertEquals(40, damage, "Arcane Nova deals full 40 damage, ignoring defense");
            assertEquals(60, enemy.getCurrentHp(), "Enemy HP is exactly 60 (100 - 40)");
            assertFalse(enemy.isDefending(), "Enemy defense broken");
        });
    }

    private static void testAssassinSpecialExecuteThreshold() {
        runTest("Assassin Special (Shadow Strike: 32 normal vs 45 execute)", () -> {
            Assassin assassin = new Assassin();
            Enemy healthyEnemy = new Enemy("Healthy", 100, 10, 10, Enemy.Behaviour.AGGRESSIVE);

            // Target at 100% HP (>= 50% HP threshold) -> 32 damage
            int normalDamage = assassin.executeSpecial(healthyEnemy);
            assertEquals(32, normalDamage, "Standard damage when target >= 50% HP");
            assertEquals(68, healthyEnemy.getCurrentHp(), "Enemy HP is 68");

            // Target below 50% HP -> Execute triggers for 45 damage
            Enemy woundedEnemy = new Enemy("Wounded", 100, 10, 10, Enemy.Behaviour.AGGRESSIVE);
            woundedEnemy.takeDamage(60); // 40 HP remaining (< 50% of 100)
            int executeDamage = assassin.executeSpecial(woundedEnemy);
            assertEquals(40, executeDamage, "Dealt remaining 40 HP to reach 0");
            assertEquals(0, woundedEnemy.getCurrentHp(), "Wounded enemy executed");
        });
    }

    private static void testSpecialCooldownTiming() {
        runTest("Special Cooldown Timing & Progression", () -> {
            Warrior warrior = new Warrior(); // Max cooldown = 3
            Enemy enemy = new Enemy("Dummy", 200, 10, 10, Enemy.Behaviour.DEFENSIVE);
            // Injected RNG: force enemy to defend (roll = 99 for DEFENSIVE above 40% HP)
            Random fixedRng = new Random(10) {
                @Override
                public int nextInt(int bound) { return 90; } // Defend
            };
            CombatEngine engine = new CombatEngine(warrior, enemy, fixedRng);

            assertTrue(warrior.isSpecialReady(), "Special ready at start");
            assertEquals(0, warrior.getSpecialCooldown(), "Cooldown starts at 0");

            // Turn 1: Use Special
            engine.executePlayerAction(Action.SPECIAL);
            // Cooldown was set to 3, then ticked down once at end of Turn 1 -> now 2
            assertEquals(2, warrior.getSpecialCooldown(), "Cooldown is 2 after Turn 1 ends");
            assertFalse(warrior.isSpecialReady(), "Special not ready on Turn 2");

            // Turn 2: Attempting to use special on cooldown throws exception
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.SPECIAL),
                    "Cannot use special while on cooldown");

            // Turn 2: Use Attack instead
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(1, warrior.getSpecialCooldown(), "Cooldown is 1 after Turn 2 ends");
            assertFalse(warrior.isSpecialReady(), "Special not ready on Turn 3");

            // Turn 3: Use Attack
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(0, warrior.getSpecialCooldown(), "Cooldown is 0 after Turn 3 ends");
            assertTrue(warrior.isSpecialReady(), "Special is ready again on Turn 4");

            // Turn 4: Can use Special again!
            List<CombatEvent> events = engine.executePlayerAction(Action.SPECIAL);
            assertTrue(events.stream().anyMatch(e -> e.kind() == CombatEvent.EventKind.ACTION_USED
                    && e.message().contains("Shield Slam")), "Successfully used special again");
        });
    }

    private static void testEnemyAggressiveBehaviour() {
        runTest("Enemy AGGRESSIVE Behaviour Pattern", () -> {
            Hero hero = new Warrior();
            Enemy enemy = new Enemy("Brute", 100, 16, 16, Enemy.Behaviour.AGGRESSIVE);

            // Roll 70 (< 80) -> ATTACK
            Action action1 = enemy.decideAction(hero, new MockRandom(70));
            assertEquals(Action.ATTACK, action1, "Aggressive attacks on roll < 80");

            // Roll 85 (>= 80) -> DEFEND
            Action action2 = enemy.decideAction(hero, new MockRandom(85));
            assertEquals(Action.DEFEND, action2, "Aggressive defends on roll >= 80");
        });
    }

    private static void testEnemyDefensiveBehaviourHeal() {
        runTest("Enemy DEFENSIVE Behaviour Pattern (Healing under 40% HP)", () -> {
            Hero hero = new Warrior();
            Enemy enemy = new Enemy("Guardian", 100, 16, 16, Enemy.Behaviour.DEFENSIVE);

            // At 100 HP (> 40% HP), roll 30 -> ATTACK
            Action action1 = enemy.decideAction(hero, new MockRandom(30));
            assertEquals(Action.ATTACK, action1, "Attacks when healthy");

            // Reduce to 35 HP (<= 40% HP)
            enemy.takeDamage(65);
            // Under 40% HP, roll 40 (< 50) -> HEAL
            Action action2 = enemy.decideAction(hero, new MockRandom(40));
            assertEquals(Action.HEAL, action2, "Heals when wounded below 40% HP");

            // Under 40% HP, roll 70 (50 <= roll < 80) -> DEFEND
            Action action3 = enemy.decideAction(hero, new MockRandom(70));
            assertEquals(Action.DEFEND, action3, "Defends when wounded");
        });
    }

    private static void testEnemyTacticalBehaviourAgainstDefend() {
        runTest("Enemy TACTICAL Behaviour against Defending Player", () -> {
            Hero hero = new Warrior();
            hero.setDefending(true);
            Enemy enemy = new Enemy("Tactician", 100, 16, 16, Enemy.Behaviour.TACTICAL);
            enemy.takeDamage(20); // 80 HP (< max)

            // When player is defending and enemy is wounded, roll 30 (< 60) -> HEAL
            Action action1 = enemy.decideAction(hero, new MockRandom(30));
            assertEquals(Action.HEAL, action1, "Tactical heals while hero shields");

            // When player is defending and enemy is wounded, roll 80 (>= 60) -> DEFEND
            Action action2 = enemy.decideAction(hero, new MockRandom(80));
            assertEquals(Action.DEFEND, action2, "Tactical defends instead of attacking into shield");
        });
    }

    private static void testReproducibleRandomizedDecisions() {
        runTest("Reproducible Randomized Decisions with Seeded Random", () -> {
            Hero hero1 = new Warrior();
            Hero hero2 = new Warrior();
            Enemy enemy1 = new Enemy("E1", 100, 16, 16, Enemy.Behaviour.TACTICAL);
            Enemy enemy2 = new Enemy("E2", 100, 16, 16, Enemy.Behaviour.TACTICAL);

            Random rng1 = new Random(4242);
            Random rng2 = new Random(4242);

            for (int i = 0; i < 20; i++) {
                Action a1 = enemy1.decideAction(hero1, rng1);
                Action a2 = enemy2.decideAction(hero2, rng2);
                assertEquals(a1, a2, "Identical decisions at step " + i);
            }
        });
    }

    private static void testVictoryPreventsEnemyCounterattack() {
        runTest("Victory Immediately Prevents Enemy Counterattack", () -> {
            Warrior warrior = new Warrior();
            // Enemy with only 10 HP
            Enemy weakEnemy = new Enemy("Fragile", 10, 50, 10, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, weakEnemy);

            // Warrior base attack is 18 -> lethal blow on 10 HP enemy
            List<CombatEvent> events = engine.executePlayerAction(Action.ATTACK);

            assertEquals(0, weakEnemy.getCurrentHp(), "Enemy HP is 0");
            assertFalse(weakEnemy.isAlive(), "Enemy is dead");
            assertEquals(CombatPhase.VICTORY, engine.getCurrentPhase(), "Phase changed to VICTORY");
            assertTrue(engine.isGameOver(), "Game is over");
            assertTrue(engine.isVictory(), "Victory achieved");

            // Warrior took 0 damage because enemy was defeated before counterattacking
            assertEquals(120, warrior.getCurrentHp(), "Warrior HP undamaged by counterattack");

            // Last event must be BATTLE_ENDED
            CombatEvent lastEvent = events.get(events.size() - 1);
            assertEquals(CombatEvent.EventKind.BATTLE_ENDED, lastEvent.kind(), "Last event is BATTLE_ENDED");
        });
    }

    private static void testHeroDefeatCondition() {
        runTest("Hero Defeat Condition", () -> {
            Mage mage = new Mage(); // 80 HP
            mage.takeDamage(75); // Mage at 5 HP
            Enemy enemy = new Enemy("Lethal", 100, 30, 10, Enemy.Behaviour.AGGRESSIVE);

            // Enemy rolls 10 (< 80) -> ATTACK for 30 damage, killing Mage
            CombatEngine engine = new CombatEngine(mage, enemy, new MockRandom(10));
            List<CombatEvent> events = engine.executePlayerAction(Action.DEFEND);

            // Defend reduces 30 to 15 damage; 5 - 15 = 0 HP
            assertEquals(0, mage.getCurrentHp(), "Mage HP reaches 0");
            assertFalse(mage.isAlive(), "Mage is dead");
            assertEquals(CombatPhase.DEFEAT, engine.getCurrentPhase(), "Phase is DEFEAT");
            assertTrue(engine.isGameOver(), "Game is over");
            assertFalse(engine.isVictory(), "Not a victory");

            CombatEvent lastEvent = events.get(events.size() - 1);
            assertEquals(CombatEvent.EventKind.BATTLE_ENDED, lastEvent.kind(), "Battle ended on defeat");
        });
    }

    private static void testInvalidActionAndPhaseExceptions() {
        runTest("Invalid Actions & Out-of-Phase Exceptions", () -> {
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Test", 10, 10, 10, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy);

            // Null action
            assertThrows(IllegalArgumentException.class, () -> engine.executePlayerAction(null),
                    "Null action rejected");

            // Defeat enemy to trigger terminal phase
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(CombatPhase.VICTORY, engine.getCurrentPhase(), "In VICTORY phase");

            // Action in terminal phase rejected
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.ATTACK),
                    "Cannot act after battle ended");
        });
    }

    private static void testDeadCharacterCannotAct() {
        runTest("Dead Character Cannot Act", () -> {
            Warrior warrior = new Warrior();
            warrior.takeDamage(warrior.getMaxHp());
            assertEquals(0, warrior.getCurrentHp(), "Warrior dead");

            Enemy enemy = new Enemy();
            CombatEngine engine = new CombatEngine(warrior, enemy);

            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.ATTACK),
                    "Dead hero cannot attack");
        });
    }

    private static void testEventOrderingAndTerminalConsistency() {
        runTest("Event Sequence & Phase Ordering Consistency", () -> {
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Test", 100, 10, 10, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy, new MockRandom(10)); // Force enemy attack

            List<CombatEvent> events = engine.executePlayerAction(Action.ATTACK);

            // Expected order:
            // 0: Player ACTION_USED
            // 1: DAMAGE_DEALT to enemy
            // 2: PHASE_CHANGED to ENEMY_TURN
            // 3: Enemy ACTION_USED
            // 4: DAMAGE_DEALT to player
            // 5: PHASE_CHANGED to PLAYER_TURN (Round 2)
            assertTrue(events.size() >= 5, "Expected at least 5 events in full round");
            assertEquals(CombatEvent.EventKind.ACTION_USED, events.get(0).kind(), "Event 0: Action used");
            assertEquals(CombatEvent.EventKind.DAMAGE_DEALT, events.get(1).kind(), "Event 1: Damage dealt");
            assertEquals(CombatEvent.EventKind.PHASE_CHANGED, events.get(2).kind(), "Event 2: Phase changed");
            assertEquals(2, engine.getTurnCount(), "Turn count incremented to 2");
            assertEquals(CombatPhase.PLAYER_TURN, engine.getCurrentPhase(), "Back to PLAYER_TURN");
        });
    }

    private static void testMageSpecialCooldownExactSequence() {
        runTest("Mage Special Cooldown (Max 4): Exact Turn-by-Turn Trace", () -> {
            Mage mage = new Mage(); // Max cooldown = 4
            Enemy enemy = new Enemy("Boss", 300, 5, 5, Enemy.Behaviour.DEFENSIVE);
            // Enemy defends on every turn (roll = 90)
            CombatEngine engine = new CombatEngine(mage, enemy, new MockRandom(90));

            // Start: Cooldown 0, ready
            assertEquals(0, mage.getSpecialCooldown(), "Mage starts with 0 cooldown");
            assertTrue(mage.isSpecialReady(), "Mage special ready at start");

            // Turn 1: Cast Special (Arcane Nova)
            engine.executePlayerAction(Action.SPECIAL);
            // Cooldown set to 4, decremented to 3 at round end
            assertEquals(3, mage.getSpecialCooldown(), "Cooldown is 3 after Turn 1 ends");
            assertFalse(mage.isSpecialReady(), "Special not ready on Turn 2");

            // Turn 2: Subsequent Action 1 (Attack)
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.SPECIAL),
                    "Special blocked on Turn 2");
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(2, mage.getSpecialCooldown(), "Cooldown is 2 after Turn 2 ends");
            assertFalse(mage.isSpecialReady(), "Special not ready on Turn 3");

            // Turn 3: Subsequent Action 2 (Attack)
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.SPECIAL),
                    "Special blocked on Turn 3");
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(1, mage.getSpecialCooldown(), "Cooldown is 1 after Turn 3 ends");
            assertFalse(mage.isSpecialReady(), "Special not ready on Turn 4");

            // Turn 4: Subsequent Action 3 (Attack)
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.SPECIAL),
                    "Special blocked on Turn 4");
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(0, mage.getSpecialCooldown(), "Cooldown is 0 after Turn 4 ends");
            assertTrue(mage.isSpecialReady(), "Special is ready again for Turn 5");

            // Turn 5: Special available again (exactly 3 subsequent player actions required)
            List<CombatEvent> events = engine.executePlayerAction(Action.SPECIAL);
            assertTrue(events.stream().anyMatch(e -> e.kind() == CombatEvent.EventKind.ACTION_USED
                    && e.message().contains("Arcane Nova")), "Arcane Nova executed successfully on Turn 5");
            assertEquals(3, mage.getSpecialCooldown(), "Cooldown resets to 3 after Turn 5 ends");
        });
    }

    private static void testAssassinSpecialCooldownExactSequence() {
        runTest("Assassin Special Cooldown (Max 3): Exact Turn-by-Turn Trace", () -> {
            Assassin assassin = new Assassin(); // Max cooldown = 3
            Enemy enemy = new Enemy("Boss", 300, 5, 5, Enemy.Behaviour.DEFENSIVE);
            CombatEngine engine = new CombatEngine(assassin, enemy, new MockRandom(90));

            assertEquals(0, assassin.getSpecialCooldown(), "Starts ready");
            assertTrue(assassin.isSpecialReady(), "Starts ready");

            // Turn 1: Special
            engine.executePlayerAction(Action.SPECIAL);
            assertEquals(2, assassin.getSpecialCooldown(), "Cooldown is 2 after Turn 1");
            assertFalse(assassin.isSpecialReady(), "Not ready on Turn 2");

            // Turn 2: Subsequent Action 1 (Attack)
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(1, assassin.getSpecialCooldown(), "Cooldown is 1 after Turn 2");
            assertFalse(assassin.isSpecialReady(), "Not ready on Turn 3");

            // Turn 3: Subsequent Action 2 (Attack)
            engine.executePlayerAction(Action.ATTACK);
            assertEquals(0, assassin.getSpecialCooldown(), "Cooldown is 0 after Turn 3");
            assertTrue(assassin.isSpecialReady(), "Ready again for Turn 4");

            // Turn 4: Special usable again (exactly 2 subsequent player actions required)
            List<CombatEvent> events = engine.executePlayerAction(Action.SPECIAL);
            assertTrue(events.stream().anyMatch(e -> e.kind() == CombatEvent.EventKind.ACTION_USED
                    && e.message().contains("Shadow Strike")), "Shadow Strike executed on Turn 4");
        });
    }

    private static void testEnemyDefenseExpiresWhenPlayerDoesNotAttack() {
        runTest("Enemy Defense Stance Expires When Player Does Not Attack", () -> {
            Warrior warrior = new Warrior(); // 18 attack
            Enemy enemy = new Enemy("Test Enemy", 110, 10, 10, Enemy.Behaviour.AGGRESSIVE);

            // Injected deterministic mock RNG to control enemy decisions per turn:
            // Round 1: Enemy DEFENDS (roll 85 in AGGRESSIVE -> DEFEND)
            // Round 2: Enemy ATTACKS (roll 10 in AGGRESSIVE -> ATTACK)
            // Round 3: Enemy ATTACKS (roll 10 in AGGRESSIVE -> ATTACK)
            Random sequenceRng = new Random() {
                private int callCount = 0;
                @Override
                public int nextInt(int bound) {
                    callCount++;
                    if (callCount == 1) return 85; // Round 1: DEFEND
                    return 10;                     // Subsequent: ATTACK
                }
            };
            CombatEngine engine = new CombatEngine(warrior, enemy, sequenceRng);

            // Round 1: Player DEFENDS. Enemy DEFENDS.
            engine.executePlayerAction(Action.DEFEND);
            assertTrue(enemy.isDefending(), "Enemy entered defensive stance at end of Round 1");

            // Round 2: Player HEALS (does NOT attack). Enemy ATTACKS.
            engine.executePlayerAction(Action.HEAL);
            // Enemy took no damage from player on Round 2, but Enemy attacked on Round 2.
            // Therefore, enemy defense from Round 1 MUST have expired!
            assertFalse(enemy.isDefending(), "Enemy defense expired because Enemy took an attack action");

            // Round 3: Player ATTACKS.
            int enemyHpBefore = enemy.getCurrentHp();
            engine.executePlayerAction(Action.ATTACK);
            int damageDealt = enemyHpBefore - enemy.getCurrentHp();

            // Warrior attack is 18. If enemy defense persisted improperly, damage would be 9.
            assertEquals(18, damageDealt, "Enemy takes full 18 unmitigated damage (defense did not persist)");
        });
    }

    private static void testWarriorShieldSlamDefenseLifecycle() {
        runTest("Warrior Shield Slam Defense Expiration & Mitigation Lifecycle", () -> {
            // Case A: Shield Slam defense mitigates incoming attack
            Warrior w1 = new Warrior();
            Enemy e1 = new Enemy("Attacker", 100, 20, 10, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine1 = new CombatEngine(w1, e1, new MockRandom(10)); // Force enemy attack
            engine1.executePlayerAction(Action.SPECIAL); // Shield Slam deals 22 dmg + enters defense
            // Enemy hits for 20 dmg -> halved to 10 dmg (120 - 10 = 110 HP)
            assertEquals(110, w1.getCurrentHp(), "Warrior took halved damage (10 dmg)");
            assertFalse(w1.isDefending(), "Warrior defense consumed by attack");

            // Case B: Shield Slam defense expires if enemy does NOT attack
            Warrior w2 = new Warrior();
            Enemy e2 = new Enemy("Guardian", 100, 20, 10, Enemy.Behaviour.DEFENSIVE);
            CombatEngine engine2 = new CombatEngine(w2, e2, new MockRandom(90)); // Force enemy defend
            engine2.executePlayerAction(Action.SPECIAL); // Shield Slam deals 22 dmg + enters defense
            // Enemy defended, did not attack. At round upkeep, Warrior defense expires!
            assertFalse(w2.isDefending(), "Warrior defense expired at end of round when not attacked");
        });
    }

    private static void testInvalidActionDoesNotMutateState() {
        runTest("Invalid Actions Do Not Mutate Any Combat State", () -> {
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Target", 100, 10, 10, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy);

            int initialHeroHp = warrior.getCurrentHp();
            int initialEnemyHp = enemy.getCurrentHp();
            int initialTurn = engine.getTurnCount();
            int initialCooldown = warrior.getSpecialCooldown();
            CombatPhase initialPhase = engine.getCurrentPhase();

            // 1. Null action
            assertThrows(IllegalArgumentException.class, () -> engine.executePlayerAction(null),
                    "Null action rejected");
            assertEquals(initialHeroHp, warrior.getCurrentHp(), "Hero HP unchanged after null action");
            assertEquals(initialEnemyHp, enemy.getCurrentHp(), "Enemy HP unchanged after null action");
            assertEquals(initialTurn, engine.getTurnCount(), "Turn count unchanged");
            assertEquals(initialCooldown, warrior.getSpecialCooldown(), "Cooldown unchanged");
            assertEquals(initialPhase, engine.getCurrentPhase(), "Phase unchanged");

            // 2. Put special on cooldown
            engine.executePlayerAction(Action.SPECIAL);
            int heroHpAfterSpecial = warrior.getCurrentHp();
            int enemyHpAfterSpecial = enemy.getCurrentHp();
            int turnAfterSpecial = engine.getTurnCount();
            int cooldownAfterSpecial = warrior.getSpecialCooldown();

            // Attempt to cast special again while on cooldown
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.SPECIAL),
                    "Cooldown violation rejected");
            assertEquals(heroHpAfterSpecial, warrior.getCurrentHp(), "Hero HP unchanged after failed action");
            assertEquals(enemyHpAfterSpecial, enemy.getCurrentHp(), "Enemy HP unchanged after failed action");
            assertEquals(turnAfterSpecial, engine.getTurnCount(), "Turn count unchanged after failed action");
            assertEquals(cooldownAfterSpecial, warrior.getSpecialCooldown(), "Cooldown unchanged after failed action");
        });
    }

    private static void testEnemyTacticalFullCoverage() {
        runTest("Enemy TACTICAL Profile: Full Coverage of Strategy Branches", () -> {
            Hero hero = new Warrior();
            Enemy enemy = new Enemy("Strategist", 100, 16, 16, Enemy.Behaviour.TACTICAL);

            // Branch 1: Player defending + Enemy at full HP (100) -> Enemy chooses DEFEND (never wastes heal)
            hero.setDefending(true);
            Action action1 = enemy.decideAction(hero, new MockRandom(20));
            assertEquals(Action.DEFEND, action1, "Tactical chooses DEFEND when hero shields and enemy is full HP");

            // Branch 2: Hero is low HP (<= 35% of 120 = 42 HP) -> Enemy presses advantage (roll 10 < 80 -> ATTACK)
            hero.setDefending(false);
            hero.takeDamage(85); // 35 HP remaining (<= 42 HP)
            Action action2 = enemy.decideAction(hero, new MockRandom(10));
            assertEquals(Action.ATTACK, action2, "Tactical relentlessly attacks low-HP hero");

            // Branch 3: Defensive enemy wounded (<= 40% HP) with roll >= 80 -> ATTACK
            Enemy defEnemy = new Enemy("Defensive", 100, 16, 16, Enemy.Behaviour.DEFENSIVE);
            defEnemy.takeDamage(70); // 30 HP (<= 40)
            Action action3 = defEnemy.decideAction(hero, new MockRandom(90));
            assertEquals(Action.ATTACK, action3, "Defensive enemy attacks on high roll when wounded");
        });
    }

    private static void testCombatEventsValuesAndImmutability() {
        runTest("Combat Events: Immutability, Exact Values & Single Battle End Event", () -> {
            Warrior warrior = new Warrior();
            Enemy enemy = new Enemy("Victim", 100, 10, 10, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine engine = new CombatEngine(warrior, enemy, new MockRandom(10));

            List<CombatEvent> events = engine.executePlayerAction(Action.ATTACK);

            // Verify list immutability
            assertThrows(UnsupportedOperationException.class, () -> events.add(null),
                    "Returned combat events list must be immutable");

            // Verify exact values in events
            CombatEvent playerAttack = events.stream()
                    .filter(e -> e.kind() == CombatEvent.EventKind.DAMAGE_DEALT && e.sourceName().equals(warrior.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing player DAMAGE_DEALT event"));
            assertEquals(warrior.getBaseAttack(), playerAttack.value(), "Damage event value matches hero base attack");

            // Verify victory event occurs exactly once upon winning
            Enemy weakEnemy = new Enemy("Weak", 15, 5, 5, Enemy.Behaviour.AGGRESSIVE);
            CombatEngine winEngine = new CombatEngine(warrior, weakEnemy);
            List<CombatEvent> winEvents = winEngine.executePlayerAction(Action.ATTACK);

            long battleEndCount = winEvents.stream()
                    .filter(e -> e.kind() == CombatEvent.EventKind.BATTLE_ENDED)
                    .count();
            assertEquals(1L, battleEndCount, "BATTLE_ENDED must appear exactly once on victory");
            assertEquals(CombatPhase.VICTORY, winEngine.getCurrentPhase(), "Final phase is VICTORY");
        });
    }

    private static void testRiftWardenStartsInPhaseIWith200Hp() {
        runTest("Rift Warden Initializes in Phase I with 200 HP", () -> {
            RiftWarden warden = new RiftWarden();
            assertEquals(200, warden.getMaxHp(), "Max HP is 200");
            assertEquals(200, warden.getCurrentHp(), "Current HP starts at 200");
            assertEquals(RiftWarden.Phase.PHASE_I_GUARDIAN, warden.getCurrentPhase(), "Starts in Phase I");
            assertFalse(warden.isPhase2Triggered(), "Phase II not triggered at start");
            assertFalse(warden.isPhase3Triggered(), "Phase III not triggered at start");
            assertEquals(15, warden.getBaseAttack(), "Phase I attack is 15");
            assertEquals("The Rift Warden", warden.getName(), "Name is The Rift Warden");
        });
    }

    private static void testRiftWardenPhaseTransitionsAndThresholds() {
        runTest("Rift Warden Sequential Phase Transitions at 65% and 30% Thresholds", () -> {
            Warrior warrior = new Warrior();
            RiftWarden warden = new RiftWarden();
            CombatEngine engine = new CombatEngine(warrior, warden, new MockRandom(90));

            // Reduce boss to 135 HP (> 65% of 200) -> remains Phase I
            warden.takeDamage(65);
            List<CombatEvent> events1 = warden.checkPhaseTransitions();
            assertEquals(0, events1.size(), "No transition above 65%");
            assertEquals(RiftWarden.Phase.PHASE_I_GUARDIAN, warden.getCurrentPhase(), "Still Phase I at 135 HP");

            // Reduce boss to 130 HP (<= 65% of 200) -> triggers Phase II
            warden.takeDamage(5);
            List<CombatEvent> events2 = warden.checkPhaseTransitions();
            assertEquals(1, events2.size(), "Phase II transition event emitted");
            assertEquals(CombatEvent.EventKind.PHASE_CHANGED, events2.get(0).kind(), "Event kind is PHASE_CHANGED");
            assertTrue(events2.get(0).message().contains("PHASE II: RIFT AWAKENING"), "Announcement mentions Awakening");
            assertEquals(RiftWarden.Phase.PHASE_II_AWAKENING, warden.getCurrentPhase(), "Phase changed to Phase II");
            assertTrue(warden.isPhase2Triggered(), "Phase II triggered flag set");
            assertEquals(20, warden.getBaseAttack(), "Phase II attack escalated to 20");

            // Reduce boss to 65 HP (> 30% of 200) -> remains Phase II
            warden.takeDamage(65);
            List<CombatEvent> events3 = warden.checkPhaseTransitions();
            assertEquals(0, events3.size(), "No transition above 30%");
            assertEquals(RiftWarden.Phase.PHASE_II_AWAKENING, warden.getCurrentPhase(), "Still Phase II at 65 HP");

            // Reduce boss to 60 HP (<= 30% of 200) -> triggers Phase III
            warden.takeDamage(5);
            List<CombatEvent> events4 = warden.checkPhaseTransitions();
            assertEquals(1, events4.size(), "Phase III transition event emitted");
            assertTrue(events4.get(0).message().contains("PHASE III: CATACLYSM"), "Announcement mentions Cataclysm");
            assertEquals(RiftWarden.Phase.PHASE_III_CATACLYSM, warden.getCurrentPhase(), "Phase changed to Phase III");
            assertTrue(warden.isPhase3Triggered(), "Phase III triggered flag set");
            assertEquals(25, warden.getBaseAttack(), "Phase III attack escalated to 25");
        });
    }

    private static void testRiftWardenEachTransitionOccursOnlyOnce() {
        runTest("Each Phase Transition Occurs Exactly Once Without Duplicate Events", () -> {
            RiftWarden warden = new RiftWarden();

            // Trigger Phase II
            warden.takeDamage(80); // 120 HP (<= 65%)
            List<CombatEvent> firstCheck = warden.checkPhaseTransitions();
            assertEquals(1, firstCheck.size(), "First crossing triggers Phase II");

            // Subsequent damage while still in Phase II (100 HP)
            warden.takeDamage(20);
            List<CombatEvent> secondCheck = warden.checkPhaseTransitions();
            assertEquals(0, secondCheck.size(), "Subsequent damage in Phase II does not re-emit");

            // Trigger Phase III
            warden.takeDamage(50); // 50 HP (<= 30%)
            List<CombatEvent> thirdCheck = warden.checkPhaseTransitions();
            assertEquals(1, thirdCheck.size(), "First crossing triggers Phase III");

            // Subsequent damage while in Phase III (30 HP)
            warden.takeDamage(20);
            List<CombatEvent> fourthCheck = warden.checkPhaseTransitions();
            assertEquals(0, fourthCheck.size(), "Subsequent damage in Phase III does not re-emit");
        });
    }

    private static void testRiftWardenSingleAttackCrossesBothThresholds() {
        runTest("Single Massive Strike Crossing Both Thresholds Emits Ordered Events", () -> {
            RiftWarden warden = new RiftWarden(); // 200 HP

            // Massive strike takes 160 HP in one hit (200 -> 40 HP, crossing 65% and 30%)
            warden.takeDamage(160);
            assertEquals(40, warden.getCurrentHp(), "Warden down to 40 HP");

            List<CombatEvent> events = warden.checkPhaseTransitions();
            assertEquals(2, events.size(), "Both phase transitions emitted");
            assertTrue(events.get(0).message().contains("PHASE II"), "First transition event is Phase II");
            assertTrue(events.get(1).message().contains("PHASE III"), "Second transition event is Phase III");
            assertEquals(RiftWarden.Phase.PHASE_III_CATACLYSM, warden.getCurrentPhase(), "Final phase is Phase III");
            assertTrue(warden.isPhase2Triggered(), "Phase II triggered");
            assertTrue(warden.isPhase3Triggered(), "Phase III triggered");

            // Subsequent hit does not re-emit
            warden.takeDamage(10);
            assertEquals(0, warden.checkPhaseTransitions().size(), "No re-emissions");
        });
    }

    private static void testRiftWardenDamageAndBehaviorAcrossPhases() {
        runTest("Rift Warden Behavior and Attack Scaling Across All Phases", () -> {
            Warrior warrior = new Warrior();
            RiftWarden warden = new RiftWarden();

            // Phase I: 60% Attack (15 dmg), 40% Defend
            assertEquals(15, warden.getBaseAttack(), "Phase I attack = 15");
            assertEquals(Action.ATTACK, warden.decideAction(warrior, new MockRandom(50)), "Phase I attacks on roll < 60");
            assertEquals(Action.DEFEND, warden.decideAction(warrior, new MockRandom(75)), "Phase I defends on roll >= 60");

            // Transition to Phase II: 70% Attack (20 dmg), 20% Special (22 dmg), 10% Defend
            warden.takeDamage(80); // 120 HP (Phase II)
            warden.checkPhaseTransitions();
            assertEquals(RiftWarden.Phase.PHASE_II_AWAKENING, warden.getCurrentPhase(), "Now in Phase II");
            assertEquals(20, warden.getBaseAttack(), "Phase II attack = 20");
            assertEquals(22, warden.getSpecialAttackDamage(), "Phase II special = 22");
            assertEquals(Action.ATTACK, warden.decideAction(warrior, new MockRandom(50)), "Phase II attacks on roll < 70");
            assertEquals(Action.SPECIAL, warden.decideAction(warrior, new MockRandom(80)), "Phase II specials on 70 <= roll < 90");
            assertEquals(Action.DEFEND, warden.decideAction(warrior, new MockRandom(95)), "Phase II defends on roll >= 90");

            // Transition to Phase III: 50% Special (28 dmg), 40% Attack (25 dmg), 10% Defend
            warden.takeDamage(70); // 50 HP (Phase III)
            warden.checkPhaseTransitions();
            assertEquals(RiftWarden.Phase.PHASE_III_CATACLYSM, warden.getCurrentPhase(), "Now in Phase III");
            assertEquals(25, warden.getBaseAttack(), "Phase III attack = 25");
            assertEquals(28, warden.getSpecialAttackDamage(), "Phase III special = 28");
            assertEquals(Action.SPECIAL, warden.decideAction(warrior, new MockRandom(30)), "Phase III specials on roll < 50");
            assertEquals(Action.ATTACK, warden.decideAction(warrior, new MockRandom(70)), "Phase III attacks on 50 <= roll < 90");
            assertEquals(Action.DEFEND, warden.decideAction(warrior, new MockRandom(95)), "Phase III defends on roll >= 90");
        });
    }

    private static void testRiftWardenCannotActAfterDefeat() {
        runTest("Defeated Rift Warden Cannot Act or Retaliate", () -> {
            RiftWarden warden = new RiftWarden();
            warden.takeDamage(200); // Dead at 0 HP
            assertEquals(0, warden.getCurrentHp(), "Warden HP is 0");
            assertFalse(warden.isAlive(), "Warden is dead");

            // Direct decideAction call throws
            assertThrows(IllegalStateException.class, () -> warden.decideAction(new Warrior(), new MockRandom(0)),
                    "Dead boss cannot decide actions");

            // Transition checks return empty
            assertEquals(0, warden.checkPhaseTransitions().size(), "Dead boss emits no phase transitions");

            // CombatEngine prevents retaliatory actions on defeat
            Warrior warrior = new Warrior();
            RiftWarden fragileBoss = new RiftWarden("Fragile Warden", 10);
            CombatEngine engine = new CombatEngine(warrior, fragileBoss);
            List<CombatEvent> events = engine.executePlayerAction(Action.ATTACK);

            assertEquals(CombatPhase.VICTORY, engine.getCurrentPhase(), "Victory achieved");
            assertTrue(engine.isGameOver(), "Game over");
            assertEquals(120, warrior.getCurrentHp(), "Warrior took 0 damage because boss did not act");
        });
    }

    private static void testRiftWardenTerminalStateAndVictory() {
        runTest("Rift Warden Victory and Defeat Remain Strictly Terminal", () -> {
            Warrior warrior = new Warrior();
            RiftWarden warden = new RiftWarden("Boss", 15);
            CombatEngine engine = new CombatEngine(warrior, warden);

            // Lethal attack
            engine.executePlayerAction(Action.ATTACK);
            assertTrue(engine.isGameOver(), "Encounter is over");
            assertTrue(engine.isVictory(), "Outcome is victory");

            // Actions in terminal phase are rejected
            assertThrows(IllegalStateException.class, () -> engine.executePlayerAction(Action.ATTACK),
                    "Cannot act after boss battle victory");
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

    private static void assertThrows(Class<? extends Throwable> expected, Runnable block, String message) {
        try {
            block.run();
            throw new AssertionError(message + " (Expected " + expected.getSimpleName() + " was not thrown)");
        } catch (Throwable t) {
            if (!expected.isInstance(t)) {
                throw new AssertionError(message + " (Expected " + expected.getSimpleName()
                        + " but caught " + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
            }
        }
    }

    /** Mock Random that returns a deterministic value for nextInt. */
    private static class MockRandom extends Random {
        private final int fixedValue;

        MockRandom(int fixedValue) {
            this.fixedValue = fixedValue;
        }

        @Override
        public int nextInt(int bound) {
            return fixedValue % bound;
        }
    }
}

