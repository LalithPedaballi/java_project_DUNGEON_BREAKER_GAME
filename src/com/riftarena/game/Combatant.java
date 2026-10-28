package com.riftarena.game;

/**
 * Common contract for any participant in the Rift Arena combat engine.
 *
 * Both the player's {@link Hero} and non-player {@link Enemy} implementations
 * adhere to this interface, ensuring the combat engine can interact with them
 * polymorphically without needing to know concrete subclass types.
 *
 * Syllabus concepts demonstrated:
 *   - Interfaces and interface abstraction.
 *   - Method contract specification without implementation details.
 */
public interface Combatant {

    /** Returns the display name of this combatant. */
    String getName();

    /** Returns the current health points, clamped between 0 and {@link #getMaxHp()}. */
    int getCurrentHp();

    /** Returns the maximum health points for this combatant. */
    int getMaxHp();

    /** Returns true if current HP is strictly greater than zero. */
    boolean isAlive();

    /**
     * Returns true if the combatant is currently in a defensive stance,
     * mitigating the next incoming attack's damage by 50%.
     */
    boolean isDefending();

    /**
     * Enables or disables the defensive stance.
     *
     * @param defending true to enter defensive stance; false to exit.
     */
    void setDefending(boolean defending);

    /**
     * Applies damage to this combatant, correctly respecting defense mitigation
     * and clamping HP to a minimum of 0.
     *
     * @param amount the incoming raw damage (must be non-negative).
     */
    void takeDamage(int amount);

    /**
     * Restores health to this combatant, clamping HP to a maximum of {@link #getMaxHp()}.
     * Dead combatants cannot be healed.
     *
     * @param amount the amount of health to restore (must be non-negative).
     */
    void heal(int amount);

    /** Returns the base attack damage dealt by a standard strike. */
    int getBaseAttack();

    /** Returns the baseline amount healed when selecting the Heal action. */
    int getHealAmount();
}

