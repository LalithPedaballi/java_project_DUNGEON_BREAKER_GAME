package com.riftarena.ui.render;

import java.awt.Graphics2D;

/**
 * Common contract for transient visual combat effects (floating numbers,
 * slash trails, spell bursts, shockwaves, shield flashes).
 *
 * Effects have a finite lifetime, update per frame, and clean up automatically.
 */
public interface CombatEffect {

    /**
     * Updates the effect state based on elapsed time.
     *
     * @param deltaSeconds time elapsed since last frame (in seconds)
     * @return true if the effect has completed its lifecycle and can be removed
     */
    boolean update(float deltaSeconds);

    /**
     * Renders the effect onto the graphics context.
     */
    void render(Graphics2D g2);
}

