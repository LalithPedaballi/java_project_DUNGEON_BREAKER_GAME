package com.riftarena.ui.render;

import com.riftarena.ui.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * High-definition character renderer for Draven (Volcanic Brawler).
 *
 * Implements:
 *   - Reference artwork integration using transparent sprite frames (assets/heroes/draven/).
 *   - Magma juggernaut design: molten volcanic armor, glowing fire fists, and molten cracks.
 *   - Complete animation states (IDLE, RUNNING, JUMPING, DASHING, ATTACKING, HURT, DEFEAT).
 *   - Directional facing and grounded baseline alignment.
 */
public class DravenRenderer implements CharacterRenderer {

    private static final Color LAVA_ORANGE  = new Color(0xFF, 0x6D, 0x00);
    private static final Color LAVA_GOLD    = new Color(0xFF, 0xD5, 0x4F);
    private static final Color STONE_DARK   = new Color(0x21, 0x18, 0x15);

    @Override
    public String getCharacterTitle() {
        return "Draven";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.05f; // Imposing heavy juggernaut scale
    }

    @Override
    public AnimationState getOffensiveState(boolean isSpecial) {
        return AnimationState.ATTACKING;
    }

    @Override
    public void render(Graphics2D g2, int centerX, int groundY, int baseSize,
                       AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        Graphics2D g = (Graphics2D) g2.create();
        AffineTransform originalTransform = g.getTransform();

        BufferedImage sprite = HeroSpriteLoader.getHeroBody("draven");
        if (sprite != null) {
            renderSpriteBased(g, sprite, centerX, groundY, baseSize, state, stateProgress, idleTime, facingRight);
        } else {
            renderProceduralFallback(g, centerX, groundY, baseSize, state, stateProgress, idleTime, facingRight);
        }

        g.setTransform(originalTransform);
        g.dispose();
    }

    /**
     * Paints the reference artwork anchored by its own feet on the ground plane, with the
     * pose supplied by {@link FigureRenderer} plus this champion's ability effects.
     */
    private void renderSpriteBased(Graphics2D g, BufferedImage sprite, int centerX, int groundY,
                                   int baseSize, AnimationState state, float stateProgress,
                                   float idleTime, boolean facingRight) {
        float height = baseSize * 1.15f * getScaleMultiplier();
        FigureRenderer.Pose pose = FigureRenderer.poseFor(state, stateProgress, idleTime);

        float shadowStrength = state == AnimationState.JUMPING ? 0.55f : 1.0f;
        FigureRenderer.shadow(g, centerX, groundY, height * 0.60f, shadowStrength);
        if (state == AnimationState.DASHING) {
            FigureRenderer.dashTrail(g, sprite, centerX, groundY, height, facingRight, pose);
        }
        FigureRenderer.draw(g, sprite, centerX, groundY, height, facingRight, pose);

        float face = facingRight ? 1f : -1f;
        int fx = Math.round(centerX + face * height * 0.30f);
        int fy = Math.round(groundY - height * 0.58f);

        if (state == AnimationState.ATTACKING && stateProgress >= 0.22f && stateProgress <= 0.78f) {
            float punch = (float) Math.sin((stateProgress - 0.22f) / 0.56f * Math.PI);
            int fistX = Math.round(centerX + face * height * (0.26f + 0.16f * punch));
            int fistY = Math.round(groundY - height * 0.52f);
            int r = Math.round(height * 0.06f);
            Graphics2D blast = (Graphics2D) g.create();
            blast.setColor(new Color(LAVA_ORANGE.getRed(), LAVA_ORANGE.getGreen(), LAVA_ORANGE.getBlue(),
                    (int) (220 * punch)));
            blast.fillOval(fistX - r, fistY - r, r * 2, r * 2);
            blast.setColor(new Color(LAVA_GOLD.getRed(), LAVA_GOLD.getGreen(), LAVA_GOLD.getBlue(),
                    (int) (230 * punch)));
            blast.fillOval(fistX - r / 2, fistY - r / 2, r, r);
            blast.setColor(new Color(255, 255, 255, (int) (230 * punch)));
            blast.fillOval(fistX - r / 5, fistY - r / 5, Math.max(2, r / 2), Math.max(2, r / 2));
            blast.dispose();
        }

        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, stateProgress < 0.5f ? 1f - stateProgress : 0f);
        }
    }

    private void renderProceduralFallback(Graphics2D g, int cx, int cy, int size,
                                          AnimationState state, float progress, float idleTime, boolean facingRight) {
        g.setColor(STONE_DARK);
        g.fillRoundRect(cx - 25, cy - 60, 50, 100, 12, 12);
        g.setColor(LAVA_ORANGE);
        g.fillOval(cx - 15, cy - 85, 30, 30);
    }
}

