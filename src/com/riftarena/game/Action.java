package com.riftarena.game;

/**
 * All discrete tactical actions a combatant can take during their turn.
 *
 * Syllabus concepts demonstrated:
 *   - Enums as strongly-typed finite constants.
 */
public enum Action {
    /** Standard melee or physical strike against the opponent. */
    ATTACK,

    /** Enter defensive stance to reduce next incoming damage by 50%. */
    DEFEND,

    /** Restore health points up to the character's maximum HP. */
    HEAL,

    /** Unleash hero-specific signature special technique. */
    SPECIAL
}

