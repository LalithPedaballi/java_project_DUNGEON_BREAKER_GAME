package com.riftarena.ui.render;

import com.riftarena.ui.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * High-definition character renderer for Kael (Spellsword Duelist).
 *
 * Implements:
 *   - Reference artwork integration using transparent sprite frames (assets/heroes/kael/).
 *   - Fencing spellsword design: crimson military longcoat, silver hair, and piercing rapier.
 *   - Complete animation states (IDLE, RUNNING, JUMPING, DASHING, ATTACKING, HURT, DEFEAT).
 *   - Directional facing and grounded baseline alignment.
 */
public class KaelRenderer implements CharacterRenderer {

    private static final Color CRIMSON_GLOW = new Color(0xEF, 0x53, 0x50);
    private static final Color CRIMSON_DARK = new Color(0x8E, 0x00, 0x00);
    private static final Color SILVER_LIGHT = new Color(0xF5, 0xF5, 0xF5);
    private static final Color GOLD_TRIM    = new Color(0xFF, 0xD5, 0x4F);

    @Override
    public String getCharacterTitle() {
        return "Kael";
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

        BufferedImage sprite = HeroSpriteLoader.getHeroBody("kael");
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
            float thrust = (float) Math.sin((stateProgress - 0.18f) / 0.64f * Math.PI);
            Graphics2D trail = (Graphics2D) g.create();
            trail.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            trail.setColor(new Color(CRIMSON_GLOW.getRed(), CRIMSON_GLOW.getGreen(), CRIMSON_GLOW.getBlue(),
                    (int) (200 * thrust)));
            int tip = Math.round(centerX + face * height * (0.30f + 0.42f * thrust));
            trail.drawLine(fx, fy + 6, tip, fy + 2);
            trail.setStroke(new BasicStroke(1.4f));
            trail.setColor(new Color(SILVER_LIGHT.getRed(), SILVER_LIGHT.getGreen(), SILVER_LIGHT.getBlue(),
                    (int) (200 * thrust)));
            trail.drawLine(fx, fy + 6, tip, fy + 2);
            trail.dispose();
        }

        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, stateProgress < 0.5f ? 1f - stateProgress : 0f);
        }
    }

    private void renderProceduralFallback(Graphics2D g, int cx, int cy, int size,
                                          AnimationState state, float progress, float idleTime, boolean facingRight) {
        g.setColor(CRIMSON_DARK);
        g.fillRoundRect(cx - 20, cy - 60, 40, 100, 10, 10);
        g.setColor(SILVER_LIGHT);
        g.fillOval(cx - 15, cy - 85, 30, 30);
    }

    private static final class RectangleDrawClip implements java.awt.Shape {
        private final RoundRectangle2D.Float clip;
        RectangleDrawClip(int x, int y, int w, int h) {
            this.clip = new RoundRectangle2D.Float(x, y, w, h, 8, 8);
        }
        @Override public java.awt.Rectangle getBounds() { return clip.getBounds(); }
        @Override public java.awt.geom.Rectangle2D getBounds2D() { return clip.getBounds2D(); }
        @Override public boolean contains(double x, double y) { return clip.contains(x, y); }
        @Override public boolean contains(java.awt.geom.Point2D p) { return clip.contains(p); }
        @Override public boolean intersects(double x, double y, double w, double h) { return clip.intersects(x, y, w, h); }
        @Override public boolean intersects(java.awt.geom.Rectangle2D r) { return clip.intersects(r); }
        @Override public boolean contains(double x, double y, double w, double h) { return clip.contains(x, y, w, h); }
        @Override public boolean contains(java.awt.geom.Rectangle2D r) { return clip.contains(r); }
        @Override public java.awt.geom.PathIterator getPathIterator(AffineTransform at) { return clip.getPathIterator(at); }
        @Override public java.awt.geom.PathIterator getPathIterator(AffineTransform at, double flatness) { return clip.getPathIterator(at, flatness); }
    }
}

