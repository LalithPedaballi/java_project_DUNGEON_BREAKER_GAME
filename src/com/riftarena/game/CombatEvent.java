package com.riftarena.game;

import java.util.Objects;

/**
 * An immutable descriptor of a single state transition or combat occurrence.
 *
 * Returned in an ordered list by {@link CombatEngine#executePlayerAction(Action)}
 * so that any user interface or testing framework can inspect, log, or animate
 * combat progress without mutating or calculating game rules independently.
 *
 * Syllabus concepts demonstrated:
 *   - Java 17 records: compact, immutable data-carrier classes.
 *   - Nested enum definitions.
 *   - Constructor parameter validation.
 */
public record CombatEvent(
        EventKind kind,
        String sourceName,
        String targetName,
        int value,
        String message
) {

    /**
     * Categories of combat events.
     */
    public enum EventKind {
        /** An action was initiated by a combatant. */
        ACTION_USED,

        /** Damage was applied to a target. */
        DAMAGE_DEALT,

        /** Health points were restored to a combatant. */
        HEALED,

        /** A combatant braced and entered defensive stance. */
        DEFENSE_RAISED,

        /** The combat state machine transitioned to a new phase. */
        PHASE_CHANGED,

        /** The battle concluded with victory or defeat. */
        BATTLE_ENDED
    }

    /** Compact canonical constructor with parameter validation. */
    public CombatEvent {
        Objects.requireNonNull(kind, "EventKind cannot be null");
        Objects.requireNonNull(message, "Event message cannot be null");
        if (sourceName == null) {
            sourceName = "";
        }
        if (targetName == null) {
            targetName = "";
        }
    }
}

