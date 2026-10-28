package com.riftarena.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Headless, deterministic turn-based combat engine for Dungeon Clash: Rift Arena.
 *
 * Coordinates turn progression, damage calculation, defense mitigation,
 * cooldown timers, and enemy AI invocation. It produces an immutable sequence
 * of {@link CombatEvent} records representing everything that transpired during
 * a round.
 *
 * Architectural boundaries:
 *   - Strictly decoupled from Swing and java.awt.
 *   - The UI never computes combat logic or mutates HP directly.
 *   - Enemy decision randomness is injectable via {@link Random} for test reproducibility.
 *
 * Combat rules summary:
 *   1. Player acts first in each round during {@link CombatPhase#PLAYER_TURN}.
 *   2. If the player's action defeats the enemy, combat immediately transitions
 *      to {@link CombatPhase#VICTORY} and the enemy counterattack is prevented.
 *   3. If the enemy survives, phase switches to {@link CombatPhase#ENEMY_TURN}
 *      and the autonomous enemy acts.
 *   4. If the enemy defeats the hero, combat transitions to {@link CombatPhase#DEFEAT}.
 *   5. At round end, player special cooldowns tick down, stale player defense expires,
 *      and control returns to {@link CombatPhase#PLAYER_TURN}.
 *
 * Syllabus concepts demonstrated:
 *   - State machine modeling via enums (CombatPhase).
 *   - Event-driven notifications via immutable records (CombatEvent).
 *   - Collections framework (List, ArrayList, List.copyOf).
 *   - Defensive programming and exception handling (IllegalStateException).
 *   - Dependency injection for deterministic testing.
 */
public class CombatEngine {

    private final Hero hero;
    private final Enemy enemy;
    private final Random rng;

    private CombatPhase currentPhase;
    private int turnCount;

    /**
     * Initializes a new CombatEngine with a default random generator.
     *
     * @param hero  the player's champion (cannot be null).
     * @param enemy the opponent combatant (cannot be null).
     */
    public CombatEngine(Hero hero, Enemy enemy) {
        this(hero, enemy, new Random());
    }

    /**
     * Initializes a new CombatEngine with an injected random generator for deterministic testing.
     *
     * @param hero  the player's champion (cannot be null).
     * @param enemy the opponent combatant (cannot be null).
     * @param rng   the random generator (cannot be null).
     */
    public CombatEngine(Hero hero, Enemy enemy, Random rng) {
        this.hero = Objects.requireNonNull(hero, "Hero cannot be null");
        this.enemy = Objects.requireNonNull(enemy, "Enemy cannot be null");
        this.rng = Objects.requireNonNull(rng, "Random instance cannot be null");

        this.currentPhase = CombatPhase.PLAYER_TURN;
        this.turnCount = 1;
    }

    // ------------------------------------------------------------------
    // State inspectors
    // ------------------------------------------------------------------

    public Hero getHero() {
        return hero;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public CombatPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public boolean isGameOver() {
        return currentPhase.isTerminal();
    }

    public boolean isVictory() {
        return currentPhase == CombatPhase.VICTORY;
    }

    // ------------------------------------------------------------------
    // Action resolution
    // ------------------------------------------------------------------

    /**
     * Executes the player's chosen action and resolves the subsequent enemy turn.
     *
     * @param action the player's chosen tactical action.
     * @return an unmodifiable, ordered list of events detailing all actions and consequences.
     * @throws IllegalArgumentException if action is null.
     * @throws IllegalStateException    if called outside {@link CombatPhase#PLAYER_TURN},
     *                                  if hero is dead, or if special is chosen on cooldown.
     */
    public List<CombatEvent> executePlayerAction(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Combat action cannot be null");
        }
        if (currentPhase != CombatPhase.PLAYER_TURN) {
            throw new IllegalStateException("Cannot act outside PLAYER_TURN. Current phase: " + currentPhase);
        }
        if (!hero.isAlive()) {
            throw new IllegalStateException("Dead hero cannot execute actions");
        }
        if (action == Action.SPECIAL && !hero.isSpecialReady()) {
            throw new IllegalStateException("Special is on cooldown (" + hero.getSpecialCooldown() + " turns remaining)");
        }

        List<CombatEvent> events = new ArrayList<>();

        // --------------------------------------------------------------
        // 1. Resolve Player Action
        // --------------------------------------------------------------
        switch (action) {
            case ATTACK -> {
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        hero.getName(),
                        enemy.getName(),
                        0,
                        hero.getName() + " attacks with their weapon!"
                ));
                boolean enemyWasDefending = enemy.isDefending();
                int initialHp = enemy.getCurrentHp();
                enemy.takeDamage(hero.getBaseAttack());
                int damageDealt = initialHp - enemy.getCurrentHp();

                String dmgMessage = enemyWasDefending
                        ? enemy.getName() + " takes " + damageDealt + " damage (mitigated by defense)."
                        : enemy.getName() + " takes " + damageDealt + " damage.";
                events.add(new CombatEvent(
                        CombatEvent.EventKind.DAMAGE_DEALT,
                        hero.getName(),
                        enemy.getName(),
                        damageDealt,
                        dmgMessage
                ));
                events.addAll(enemy.checkPhaseTransitions());
            }
            case DEFEND -> {
                hero.setDefending(true);
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        hero.getName(),
                        hero.getName(),
                        0,
                        hero.getName() + " raises their guard to Defend."
                ));
                events.add(new CombatEvent(
                        CombatEvent.EventKind.DEFENSE_RAISED,
                        hero.getName(),
                        hero.getName(),
                        0,
                        hero.getName() + " entered defensive stance (incoming damage reduced by 50%)."
                ));
            }
            case HEAL -> {
                int initialHp = hero.getCurrentHp();
                hero.heal(hero.getHealAmount());
                int restored = hero.getCurrentHp() - initialHp;
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        hero.getName(),
                        hero.getName(),
                        0,
                        hero.getName() + " channels a restorative surge."
                ));
                events.add(new CombatEvent(
                        CombatEvent.EventKind.HEALED,
                        hero.getName(),
                        hero.getName(),
                        restored,
                        hero.getName() + " restored " + restored + " HP (Current HP: "
                                + hero.getCurrentHp() + "/" + hero.getMaxHp() + ")."
                ));
            }
            case SPECIAL -> {
                hero.startSpecialCooldown();
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        hero.getName(),
                        enemy.getName(),
                        0,
                        hero.getName() + " unleashes signature special: " + hero.getSpecialName() + "!"
                ));
                int damageDealt = hero.executeSpecial(enemy);
                events.add(new CombatEvent(
                        CombatEvent.EventKind.DAMAGE_DEALT,
                        hero.getName(),
                        enemy.getName(),
                        damageDealt,
                        hero.getSpecialName() + " dealt " + damageDealt + " damage to " + enemy.getName() + "!"
                ));
                events.addAll(enemy.checkPhaseTransitions());
                // If Warrior used special, they also enter defensive stance
                if (hero.isDefending()) {
                    events.add(new CombatEvent(
                        CombatEvent.EventKind.DEFENSE_RAISED,
                        hero.getName(),
                        hero.getName(),
                        0,
                        hero.getName() + " braces behind a raised shield!"
                    ));
                }
            }
        }

        // --------------------------------------------------------------
        // 2. Check for Enemy Defeat (Victory prevents counterattack)
        // --------------------------------------------------------------
        if (!enemy.isAlive()) {
            hero.setDefending(false);
            enemy.setDefending(false);
            currentPhase = CombatPhase.VICTORY;
            events.add(new CombatEvent(
                    CombatEvent.EventKind.PHASE_CHANGED,
                    "Engine",
                    "",
                    0,
                    "Phase transitioned to VICTORY."
            ));
            events.add(new CombatEvent(
                    CombatEvent.EventKind.BATTLE_ENDED,
                    hero.getName(),
                    enemy.getName(),
                    0,
                    enemy.getName() + " has collapsed! Victory is yours!"
            ));
            return List.copyOf(events);
        }

        // --------------------------------------------------------------
        // 3. Enemy Turn Execution
        // --------------------------------------------------------------
        currentPhase = CombatPhase.ENEMY_TURN;
        events.add(new CombatEvent(
                CombatEvent.EventKind.PHASE_CHANGED,
                "Engine",
                "",
                0,
                "Phase transitioned to ENEMY_TURN."
        ));

        // Stale enemy defense from a prior round expires before enemy takes their new action
        enemy.setDefending(false);

        Action enemyAction = enemy.decideAction(hero, rng);
        switch (enemyAction) {
            case ATTACK -> {
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        enemy.getName(),
                        hero.getName(),
                        0,
                        enemy.getAttackMessage()
                ));
                boolean heroWasDefending = hero.isDefending();
                int initialHp = hero.getCurrentHp();
                hero.takeDamage(enemy.getBaseAttack());
                int damageDealt = initialHp - hero.getCurrentHp();

                String dmgMessage = heroWasDefending
                        ? hero.getName() + " takes " + damageDealt + " damage (mitigated by shield)."
                        : hero.getName() + " takes " + damageDealt + " damage.";
                events.add(new CombatEvent(
                        CombatEvent.EventKind.DAMAGE_DEALT,
                        enemy.getName(),
                        hero.getName(),
                        damageDealt,
                        dmgMessage
                ));
            }
            case DEFEND -> {
                enemy.setDefending(true);
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        enemy.getName(),
                        enemy.getName(),
                        0,
                        enemy.getName() + " hardens its form in defensive posture."
                ));
                events.add(new CombatEvent(
                        CombatEvent.EventKind.DEFENSE_RAISED,
                        enemy.getName(),
                        enemy.getName(),
                        0,
                        enemy.getName() + " entered defensive stance."
                ));
            }
            case HEAL -> {
                int initialHp = enemy.getCurrentHp();
                enemy.heal(enemy.getHealAmount());
                int restored = enemy.getCurrentHp() - initialHp;
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        enemy.getName(),
                        enemy.getName(),
                        0,
                        enemy.getName() + " absorbs ambient rift essence to heal."
                ));
                events.add(new CombatEvent(
                        CombatEvent.EventKind.HEALED,
                        enemy.getName(),
                        enemy.getName(),
                        restored,
                        enemy.getName() + " restored " + restored + " HP."
                ));
            }
            case SPECIAL -> {
                events.add(new CombatEvent(
                        CombatEvent.EventKind.ACTION_USED,
                        enemy.getName(),
                        hero.getName(),
                        0,
                        enemy.getSpecialMessage()
                ));
                boolean heroWasDefending = hero.isDefending();
                int initialHp = hero.getCurrentHp();
                hero.takeDamage(enemy.getSpecialAttackDamage());
                int damageDealt = initialHp - hero.getCurrentHp();
                String dmgMessage = heroWasDefending
                        ? hero.getName() + " takes " + damageDealt + " damage (mitigated by shield)."
                        : hero.getName() + " takes " + damageDealt + " damage.";
                events.add(new CombatEvent(
                        CombatEvent.EventKind.DAMAGE_DEALT,
                        enemy.getName(),
                        hero.getName(),
                        damageDealt,
                        dmgMessage
                ));
            }
        }

        // --------------------------------------------------------------
        // 4. Check for Hero Defeat
        // --------------------------------------------------------------
        if (!hero.isAlive()) {
            hero.setDefending(false);
            enemy.setDefending(false);
            currentPhase = CombatPhase.DEFEAT;
            events.add(new CombatEvent(
                    CombatEvent.EventKind.PHASE_CHANGED,
                    "Engine",
                    "",
                    0,
                    "Phase transitioned to DEFEAT."
            ));
            events.add(new CombatEvent(
                    CombatEvent.EventKind.BATTLE_ENDED,
                    enemy.getName(),
                    hero.getName(),
                    0,
                    hero.getName() + " has fallen. The rift claims another victim."
            ));
            return List.copyOf(events);
        }

        // --------------------------------------------------------------
        // 5. Round Upkeep & Return to Player Turn
        // --------------------------------------------------------------
        // Player's defensive stance ends with the round if it was not already consumed
        hero.setDefending(false);

        // Player special cooldown ticks down once per completed round
        hero.tickCooldown();

        turnCount++;
        currentPhase = CombatPhase.PLAYER_TURN;
        events.add(new CombatEvent(
                CombatEvent.EventKind.PHASE_CHANGED,
                "Engine",
                "",
                turnCount,
                "Turn " + turnCount + " begins: PLAYER_TURN."
        ));

        return List.copyOf(events);
    }
}

