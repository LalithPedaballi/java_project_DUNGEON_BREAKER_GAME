package com.riftarena.ui.render;

import java.awt.Graphics2D;

/**
 * Standard contract for drawing 2D character figures, armor layers, weapons,
 * and animation states in the Rift Arena using Java2D.
 *
 * Each concrete hero and enemy provides its own distinct rendering logic,
 * completely decoupled from the game model and combat engine.
 *
 * Syllabus concepts demonstrated:
 *   - Interfaces (Unit IV): defining uniform rendering contracts for polymorphic fighters.
 *   - Graphics2D transformations: scaling, translation, and rotation.
 */
public interface CharacterRenderer {

    /**
     * Renders the character standing on the arena ground plane.
     *
     * @param g2            the graphics context to draw into
     * @param centerX       horizontal center of the character
     * @param groundY       ground contact line the character's feet must rest on
     * @param baseSize      reference height for the fighter (~220px); the artwork is scaled to it
     * @param state         the active visual animation state (IDLE, ATTACKING, etc.)
     * @param stateProgress normalized progress of current state animation, clamped [0.0f, 1.0f]
     * @param idleTime      accumulated continuous time for subtle breathing and particle drift
     * @param facingRight   true if facing toward the right (heroes), false if facing left (enemies)
     */
    void render(Graphics2D g2, int centerX, int groundY, int baseSize,
                AnimationState state, float stateProgress, float idleTime, boolean facingRight);

    /**
     * Returns the title or identity name of this character renderer.
     */
    String getCharacterTitle();

    /**
     * Multiplier applied to standard base size. Bosses return > 1.0 for imposing scale.
     */
    default float getScaleMultiplier() {
        return 1.0f;
    }

    /**
     * Returns the natural offensive animation state used for regular attacks.
     * (e.g. Warrior/Assassin return ATTACKING, Mage returns CASTING).
     */
    default AnimationState getOffensiveState(boolean isSpecial) {
        return AnimationState.ATTACKING;
    }
}

