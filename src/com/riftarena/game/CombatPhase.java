package com.riftarena.game;

/**
 * Represents the distinct states of the combat lifecycle.
 *
 * The engine moves through these phases deterministically. The player may only
 * execute actions during {@link #PLAYER_TURN}.
 *
 * Syllabus concepts demonstrated:
 *   - Enums and state machine modeling.
 */
public enum CombatPhase {
    /** Awaiting player command input (Attack, Defend, Heal, Special). */
    PLAYER_TURN,

    /** Engine is resolving autonomous enemy AI action. */
    ENEMY_TURN,

    /** Terminal state: the enemy has fallen; player won. */
    VICTORY,

    /** Terminal state: the hero has fallen; enemy won. */
    DEFEAT;

    /** Returns true if this phase represents an ended encounter. */
    public boolean isTerminal() {
        return this == VICTORY || this == DEFEAT;
    }
}

