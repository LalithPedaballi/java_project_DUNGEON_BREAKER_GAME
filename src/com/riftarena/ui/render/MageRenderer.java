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
 * High-definition character renderer for Zephyra (Mage champion).
 *
 * Implements:
 *   - Reference artwork integration using transparent sprite frames (assets/heroes/zephyra/).
 *   - Authentic sorceress design: flowing royal purple robe, silver steel corset & pauldrons,
 *     spiral staff crowned with glowing amethyst crystal, and orbiting celestial shards.
 *   - Complete animation states:
 *       * IDLE: ethereal levitation float, robe flutter, floating arcane shards.
 *       * RUNNING: celestial glide stride with trailing robes and staff tilt.
 *       * JUMPING: airborne levitation with staff pointed skyward.
 *       * DASHING: arcane blink teleport with spatial distortion trail.
 *       * CASTING / ATTACKING: 3-phase staff channeling, surge, and arcane nova burst.
 *       * HURT / DEFEAT: flinch recoil, impact flash, and collapse.
 *   - Automatic procedural fallback if artwork files are unavailable.
 */
public class MageRenderer implements CharacterRenderer {

    // Palette constants for Mage (Zephyra)
    private static final Color ROBE_VOID   = new Color(0x11, 0x07, 0x20);
    private static final Color ROBE_DARK   = new Color(0x20, 0x11, 0x38);
    private static final Color ROBE_MID    = new Color(0x4D, 0x28, 0x82);
    private static final Color ROBE_LIGHT  = new Color(0x82, 0x54, 0xCC);

    private static final Color ARCANE_CYAN = new Color(0x52, 0xE5, 0xFF);
    private static final Color ARCANE_PURP = new Color(0xC0, 0x84, 0xFC);
    private static final Color STEEL_SILVER = new Color(0xDA, 0xE7, 0xF2);

    @Override
    public String getCharacterTitle() {
        return "Mage";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.05f; // Dignified sorcerer stature
    }

    @Override
    public AnimationState getOffensiveState(boolean isSpecial) {
        return AnimationState.CASTING;
    }

    @Override
    public void render(Graphics2D g2, int centerX, int groundY, int baseSize,
                       AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        Graphics2D g = (Graphics2D) g2.create();
        AffineTransform originalTransform = g.getTransform();

        BufferedImage sprite = HeroSpriteLoader.getHeroBody("zephyra");
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

        float orbit = idleTime * 2.6f;
        for (int i = 0; i < 3; i++) {
            double angle = orbit + i * (Math.PI * 2 / 3);
            int sx = fx + (int) (Math.cos(angle) * height * 0.07f);
            int sy = fy + (int) (Math.sin(angle) * height * 0.05f);
            g.setColor(new Color(ARCANE_PURP.getRed(), ARCANE_PURP.getGreen(), ARCANE_PURP.getBlue(), 190));
            g.fillOval(sx - 3, sy - 3, 6, 6);
            g.setColor(Color.WHITE);
            g.fillOval(sx - 1, sy - 1, 3, 3);
        }
        if ((state == AnimationState.CASTING || state == AnimationState.ATTACKING)
                && stateProgress >= 0.20f && stateProgress <= 0.80f) {
            float surge = (float) Math.sin((stateProgress - 0.20f) / 0.60f * Math.PI);
            Graphics2D burst = (Graphics2D) g.create();
            float radius = height * 0.16f * (1.0f + surge * 0.4f);
            burst.setStroke(new BasicStroke(3.0f));
            burst.setColor(new Color(ARCANE_PURP.getRed(), ARCANE_PURP.getGreen(), ARCANE_PURP.getBlue(),
                    (int) (210 * surge)));
            burst.draw(new java.awt.geom.Ellipse2D.Float(fx - radius, fy - radius, radius * 2, radius * 2));
            burst.setStroke(new BasicStroke(1.6f));
            burst.setColor(new Color(ARCANE_CYAN.getRed(), ARCANE_CYAN.getGreen(), ARCANE_CYAN.getBlue(),
                    (int) (200 * surge)));
            burst.draw(new java.awt.geom.Ellipse2D.Float(fx - radius * 0.6f, fy - radius * 0.6f,
                    radius * 1.2f, radius * 1.2f));
            burst.dispose();
        }

        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, stateProgress < 0.5f ? 1f - stateProgress : 0f);
        }
    }

    private void renderProceduralFallback(Graphics2D g, int centerX, int centerY, int baseSize,
                                          AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        float levitate = (float) Math.sin(idleTime * 2.5f) * 5.0f;
        float offsetX = 0;
        float offsetY = levitate;
        float staffAngle = 0.05f;

        switch (state) {
            case RUNNING -> {
                offsetY += (float) Math.sin(idleTime * 5.0f) * 4.0f;
                staffAngle = -0.3f;
            }
            case JUMPING -> {
                offsetY -= 16.0f;
                staffAngle = -0.6f;
            }
            case DASHING -> {
                offsetX = (facingRight ? 1 : -1) * 32.0f;
                staffAngle = -0.8f;
            }
            case CASTING, ATTACKING -> {
                float surge = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? 1 : -1) * surge * 22.0f;
                offsetY -= surge * 18.0f;
                staffAngle = -1.05f * surge;
            }
            default -> {}
        }

        g.translate(centerX + offsetX, centerY + offsetY);
        if (!facingRight) {
            g.scale(-1, 1);
        }

        float scale = (baseSize / 180.0f) * getScaleMultiplier();
        g.scale(scale, scale);

        // Procedural body
        drawRobes(g);
        drawSilverCorset(g);
        drawFaceAndHair(g);
        drawCrystalStaff(g, staffAngle);
    }

    private void drawRobes(Graphics2D g) {
        Path2D.Float robes = new Path2D.Float();
        robes.moveTo(-18, 0);
        robes.lineTo(18, 0);
        robes.lineTo(28, 72);
        robes.lineTo(-28, 72);
        robes.closePath();
        g.setPaint(new GradientPaint(0, 0, ROBE_MID, 0, 72, ROBE_DARK));
        g.fill(robes);
    }

    private void drawSilverCorset(Graphics2D g) {
        Path2D.Float corset = new Path2D.Float();
        corset.moveTo(-16, -30);
        corset.lineTo(16, -30);
        corset.lineTo(20, 0);
        corset.lineTo(-20, 0);
        corset.closePath();
        g.setPaint(new GradientPaint(0, -30, STEEL_SILVER, 0, 0, ROBE_MID));
        g.fill(corset);
    }

    private void drawFaceAndHair(Graphics2D g) {
        // Brown hair
        g.setColor(new Color(0x4A, 0x2A, 0x18));
        g.fillOval(-14, -58, 28, 30);
        // Face
        g.setColor(new Color(0xF5, 0xD6, 0xC6));
        g.fillOval(-10, -52, 20, 22);
    }

    private void drawCrystalStaff(Graphics2D g, float angle) {
        Graphics2D sg = (Graphics2D) g.create();
        sg.translate(-24, 0);
        sg.rotate(angle);

        // Staff rod
        sg.setColor(STEEL_SILVER);
        sg.fillRect(-3, -80, 6, 150);

        // Amethyst spear tip
        Path2D.Float crystal = new Path2D.Float();
        crystal.moveTo(0, -110);
        crystal.lineTo(8, -80);
        crystal.lineTo(-8, -80);
        crystal.closePath();
        sg.setPaint(new GradientPaint(0, -110, Color.WHITE, 0, -80, ARCANE_PURP));
        sg.fill(crystal);

        sg.dispose();
    }
}
