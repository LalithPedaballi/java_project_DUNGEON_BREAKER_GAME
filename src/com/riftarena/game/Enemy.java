package com.riftarena.game;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Autonomous opponent combatant controlled by AI behavior heuristics.
 *
 * Implements {@link Combatant}. Enemy action decisions are mediated through
 * {@link #decideAction(Hero, Random)}, which accepts an injectable {@link Random}
 * instance to ensure fully deterministic and reproducible testing.
 *
 * Syllabus concepts demonstrated:
 *   - Classes, constructors, and encapsulation.
 *   - Nested enums (Behaviour).
 *   - Interface implementation (implements Combatant).
 *   - Deterministic algorithm design using dependency injection (Random).
 */
public class Enemy implements Combatant {

    /**
     * AI behavioral profiles that dictate tactical decision making.
     */
    public enum Behaviour {
        /** Heavy emphasis on attacking, rarely defends. */
        AGGRESSIVE,

        /** Prioritizes healing when below 40% HP and frequent defending. */
        DEFENSIVE,

        /** Adapts dynamically: heals or defends when the player shields, executes when player is low. */
        TACTICAL
    }

    public static final String DEFAULT_NAME = "Rift Spawn";
    public static final int DEFAULT_MAX_HP = 110;
    public static final int DEFAULT_ATTACK = 16;
    public static final int DEFAULT_HEAL = 16;

    private final String name;
    private final int maxHp;
    private int currentHp;
    private final int baseAttack;
    private final int healAmount;
    private boolean defending;
    private final Behaviour behaviour;

    public Enemy() {
        this(DEFAULT_NAME, DEFAULT_MAX_HP, DEFAULT_ATTACK, DEFAULT_HEAL, Behaviour.TACTICAL);
    }

    public Enemy(String name, int maxHp, int baseAttack, int healAmount, Behaviour behaviour) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Enemy name cannot be null or blank");
        }
        if (maxHp <= 0) {
            throw new IllegalArgumentException("Enemy max HP must be positive: " + maxHp);
        }
        if (baseAttack <= 0) {
            throw new IllegalArgumentException("Enemy base attack must be positive: " + baseAttack);
        }
        if (healAmount <= 0) {
            throw new IllegalArgumentException("Enemy heal amount must be positive: " + healAmount);
        }
        this.name = name;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.baseAttack = baseAttack;
        this.healAmount = healAmount;
        this.behaviour = Objects.requireNonNull(behaviour, "Enemy behaviour cannot be null");
        this.defending = false;
    }

    // ------------------------------------------------------------------
    // Combatant implementation
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCurrentHp() {
        return currentHp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    @Override
    public boolean isAlive() {
        return currentHp > 0;
    }

    @Override
    public boolean isDefending() {
        return defending;
    }

    @Override
    public void setDefending(boolean defending) {
        this.defending = defending;
    }

    @Override
    public int getBaseAttack() {
        return baseAttack;
    }

    @Override
    public int getHealAmount() {
        return healAmount;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || !isAlive()) {
            return;
        }

        int finalDamage = amount;
        if (defending) {
            finalDamage = amount / 2;
            defending = false; // Defense consumed upon taking hit
        }

        currentHp = Math.max(0, currentHp - finalDamage);
    }

    @Override
    public void heal(int amount) {
        if (amount <= 0 || !isAlive()) {
            return;
        }
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    /**
     * Checks if this enemy undergoes any state or phase transitions.
     * Default implementation returns an empty list; overridden by multi-phase bosses.
     */
    public List<CombatEvent> checkPhaseTransitions() {
        return List.of();
    }

    /**
     * Returns the description displayed when this enemy attacks.
     */
    public String getAttackMessage() {
        return name + " lunges forward to attack!";
    }

    /**
     * Returns the description displayed when this enemy unleashes a special attack.
     */
    public String getSpecialMessage() {
        return name + " channels a devastating strike!";
    }

    /**
     * Returns the damage inflicted by this enemy's special ability.
     * Defaults to base attack for ordinary enemies.
     */
    public int getSpecialAttackDamage() {
        return getBaseAttack();
    }

    // ------------------------------------------------------------------
    // Decision AI
    // ------------------------------------------------------------------

    /**
     * Chooses the enemy's next combat action based on its {@link Behaviour} profile,
     * the opponent's current status, and a provided random generator.
     *
     * @param hero the player hero being faced.
     * @param rng  random number generator (must not be null).
     * @return the selected {@link Action} (ATTACK, DEFEND, or HEAL).
     */
    public Action decideAction(Hero hero, Random rng) {
        if (!isAlive()) {
            throw new IllegalStateException("Dead enemy cannot decide actions");
        }
        Objects.requireNonNull(hero, "Hero cannot be null");
        Objects.requireNonNull(rng, "Random instance cannot be null");

        int roll = rng.nextInt(100);

        Action chosen;
        switch (behaviour) {
            case AGGRESSIVE -> {
                // 80% Attack, 20% Defend
                chosen = (roll < 80) ? Action.ATTACK : Action.DEFEND;
            }
            case DEFENSIVE -> {
                // When below 40% HP: 50% Heal, 30% Defend, 20% Attack
                if (currentHp <= (int) (maxHp * 0.40)) {
                    if (roll < 50) {
                        chosen = Action.HEAL;
                    } else if (roll < 80) {
                        chosen = Action.DEFEND;
                    } else {
                        chosen = Action.ATTACK;
                    }
                } else {
                    // Above 40% HP: 60% Attack, 40% Defend
                    chosen = (roll < 60) ? Action.ATTACK : Action.DEFEND;
                }
            }
            case TACTICAL -> {
                // If player is defending, avoid wasting damage: heal if damaged, else defend
                if (hero.isDefending()) {
                    if (currentHp < maxHp) {
                        chosen = (roll < 60) ? Action.HEAL : Action.DEFEND;
                    } else {
                        chosen = Action.DEFEND;
                    }
                } else if (hero.getCurrentHp() <= (int) (hero.getMaxHp() * 0.35)) {
                    // Hero is low on HP: press the advantage (80% Attack, 20% Defend)
                    chosen = (roll < 80) ? Action.ATTACK : Action.DEFEND;
                } else {
                    // Standard tactical flow: 70% Attack, 20% Defend, 10% Heal (if damaged)
                    if (roll < 70) {
                        chosen = Action.ATTACK;
                    } else if (roll < 90) {
                        chosen = Action.DEFEND;
                    } else {
                        chosen = (currentHp < maxHp) ? Action.HEAL : Action.ATTACK;
                    }
                }
            }
            default -> chosen = Action.ATTACK;
        }

        // Safety fallback: if HEAL was chosen but enemy is at max health, convert to ATTACK
        if (chosen == Action.HEAL && currentHp >= maxHp) {
            chosen = Action.ATTACK;
        }

        return chosen;
    }
}

