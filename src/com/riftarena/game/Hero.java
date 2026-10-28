package com.riftarena.game;

import java.util.Objects;

/**
 * Abstract base class for all playable champions in the Rift Arena.
 *
 * Implements the {@link Combatant} interface, providing concrete encapsulation
 * for health points, standard attacks, healing, defensive stance mitigation,
 * and special ability cooldown tracking.
 *
 * Concrete subclasses ({@link Warrior}, {@link Mage}, {@link Assassin}) define
 * their distinct base statistics and polymorphic special ability implementations.
 *
 * Syllabus concepts demonstrated:
 *   - Abstract classes and method contracts.
 *   - Encapsulation with private fields and validating getters/mutators.
 *   - Implementation of interfaces (implements Combatant).
 *   - Constructor chaining and subclass inheritance.
 */
public abstract class Hero implements Combatant {

    private final String name;
    private final int maxHp;
    private int currentHp;
    private final int baseAttack;
    private final int healAmount;
    private boolean defending;

    private int specialCooldown;
    private final int maxCooldown;

    /**
     * Initializes a new Hero with validated baseline statistics.
     *
     * @param name         Hero display name (cannot be null or blank).
     * @param maxHp        Maximum health points (must be positive).
     * @param baseAttack   Standard attack damage (must be positive).
     * @param healAmount   Standard heal recovery (must be positive).
     * @param maxCooldown  Cooldown turns required between special uses (non-negative).
     */
    protected Hero(String name, int maxHp, int baseAttack, int healAmount, int maxCooldown) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Hero name cannot be null or blank");
        }
        if (maxHp <= 0) {
            throw new IllegalArgumentException("Max HP must be positive: " + maxHp);
        }
        if (baseAttack <= 0) {
            throw new IllegalArgumentException("Base attack must be positive: " + baseAttack);
        }
        if (healAmount <= 0) {
            throw new IllegalArgumentException("Heal amount must be positive: " + healAmount);
        }
        if (maxCooldown < 0) {
            throw new IllegalArgumentException("Max cooldown cannot be negative: " + maxCooldown);
        }

        this.name = name;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.baseAttack = baseAttack;
        this.healAmount = healAmount;
        this.maxCooldown = maxCooldown;
        this.specialCooldown = 0; // ready on battle start
        this.defending = false;
    }

    // ------------------------------------------------------------------
    // Combatant interface implementation
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

    /**
     * Applies damage to this hero. If currently in defensive stance, damage
     * is reduced by 50% (rounded down), and the defense stance expires.
     * Health is clamped to a minimum of 0.
     *
     * @param amount the incoming raw damage.
     */
    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || !isAlive()) {
            return;
        }

        int finalDamage = amount;
        if (defending) {
            finalDamage = amount / 2;
            defending = false; // Defense consumed upon mitigating a blow
        }

        currentHp = Math.max(0, currentHp - finalDamage);
    }

    /**
     * Restores health up to {@link #getMaxHp()}. Dead heroes cannot heal.
     *
     * @param amount health points to restore.
     */
    @Override
    public void heal(int amount) {
        if (amount <= 0 || !isAlive()) {
            return;
        }
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    // ------------------------------------------------------------------
    // Cooldown management
    // ------------------------------------------------------------------

    /** Returns true if the special ability is available for use. */
    public boolean isSpecialReady() {
        return specialCooldown == 0;
    }

    /** Triggers the cooldown timer for the special ability. */
    public void startSpecialCooldown() {
        this.specialCooldown = maxCooldown;
    }

    /** Decreases special cooldown by 1 turn at end of round (minimum 0). */
    public void tickCooldown() {
        if (specialCooldown > 0) {
            specialCooldown--;
        }
    }

    /** Returns remaining cooldown turns until special can be cast again. */
    public int getSpecialCooldown() {
        return specialCooldown;
    }

    /** Returns the base maximum cooldown duration for this hero's special. */
    public int getMaxCooldown() {
        return maxCooldown;
    }

    // ------------------------------------------------------------------
    // Polymorphic special ability contract
    // ------------------------------------------------------------------

    /**
     * Executes the hero's signature special ability against the target.
     *
     * @param target the opponent combatant receiving the special.
     * @return the actual damage dealt to the target.
     */
    public abstract int executeSpecial(Combatant target);

    /** Returns the display name of this hero's signature ability. */
    public abstract String getSpecialName();

    /** Returns a concise description of the special ability's mechanics. */
    public abstract String getSpecialDescription();
}

