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
 * High-definition character renderer for Vexara (Nether Reaper).
 *
 * Implements:
 *   - Reference artwork integration using transparent sprite frames (assets/heroes/vexara/).
 *   - Dark necromancer design: spectral cowl, dark plate, and colossal purple scythe.
 *   - Complete animation states (IDLE, RUNNING, JUMPING, DASHING, ATTACKING, HURT, DEFEAT).
 *   - Directional facing and grounded baseline alignment.
 */
public class VexaraRenderer implements CharacterRenderer {

    private static final Color REAPER_VIOLET = new Color(0xBA, 0x68, 0xC8);
    private static final Color SPECTRAL_CYAN = new Color(0x00, 0xE5, 0xFF);
    private static final Color SHADOW_DARK   = new Color(0x1A, 0x0A, 0x2A);

    @Override
    public String getCharacterTitle() {
        return "Vexara";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.0f;
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

        BufferedImage sprite = HeroSpriteLoader.getHeroBody("vexara");
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

        if (state == AnimationState.ATTACKING && stateProgress >= 0.18f && stateProgress <= 0.82f) {
            float sweep = (float) Math.sin((stateProgress - 0.18f) / 0.64f * Math.PI);
            Graphics2D arc = (Graphics2D) g.create();
            arc.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            arc.setColor(new Color(REAPER_VIOLET.getRed(), REAPER_VIOLET.getGreen(), REAPER_VIOLET.getBlue(),
                    (int) (210 * sweep)));
            arc.drawArc(fx - 65, fy - 55, 130, 120, -60, 135);
            arc.setStroke(new BasicStroke(1.8f));
            arc.setColor(new Color(SPECTRAL_CYAN.getRed(), SPECTRAL_CYAN.getGreen(), SPECTRAL_CYAN.getBlue(),
                    (int) (190 * sweep)));
            arc.drawArc(fx - 55, fy - 46, 112, 102, -50, 120);
            arc.dispose();
        }

        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, stateProgress < 0.5f ? 1f - stateProgress : 0f);
        }
    }

    private void renderProceduralFallback(Graphics2D g, int cx, int cy, int size,
                                          AnimationState state, float progress, float idleTime, boolean facingRight) {
        g.setColor(SHADOW_DARK);
        g.fillRoundRect(cx - 20, cy - 60, 40, 100, 10, 10);
        g.setColor(REAPER_VIOLET);
        g.fillOval(cx - 15, cy - 85, 30, 30);
    }
}

