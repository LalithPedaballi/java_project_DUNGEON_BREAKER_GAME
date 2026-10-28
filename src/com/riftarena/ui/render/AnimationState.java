package com.riftarena.ui.render;

/**
 * Visual animation states for combatants in the Rift Arena.
 *
 * These states govern only visual pose, weapon positioning, particle effects,
 * and hit reactions. They do NOT modify or govern authoritative combat rules,
 * health points, or turn progression.
 *
 * Syllabus concepts demonstrated:
 *   - Enum types with descriptive methods and state queries.
 */
public enum AnimationState {
    /** Default breathing, stance swaying, and atmospheric aura. */
    IDLE,

    /** Locomotion run/walk cycle with alternating limbs. */
    RUNNING,

    /** Airborne leaping or falling trajectory. */
    JUMPING,

    /** High-velocity evasive dash with phantom trail. */
    DASHING,

    /** Active physical weapon swing, pincer strike, or halberd cleave. */
    ATTACKING,

    /** Mystical spell channeling, raising staff, and charging magical energy. */
    CASTING,

    /** Raising shield, barrier ward, or crossing defensive blades. */
    DEFENDING,

    /** Recoiling, flinching, and flashing upon taking damage. */
    HURT,

    /** Triumphant stance, weapon raised high, glowing victorious aura. */
    VICTORY,

    /** Defeated collapse, kneeling, or dissolving into the rift. */
    DEFEAT;

    /**
     * Checks if this state represents an active offensive move.
     */
    public boolean isOffensive() {
        return this == ATTACKING || this == CASTING;
    }

    /**
     * Checks if this state represents locomotion or airborne movement.
     */
    public boolean isLocomotion() {
        return this == RUNNING || this == JUMPING || this == DASHING;
    }

    /**
     * Checks if this state represents a terminal encounter outcome.
     */
    public boolean isTerminal() {
        return this == VICTORY || this == DEFEAT;
    }
}
