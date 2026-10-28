package com.riftarena.ui.render;

import com.riftarena.ui.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * High-definition character renderer for Nyx (Assassin champion).
 *
 * Implements:
 *   - Reference artwork integration using transparent sprite frames (assets/heroes/nyx/).
 *   - Authentic rogue design: dark green & black hooded tunic, ninja half-mask,
 *     leather crisscross vambraces, and dual curved serrated silver kris daggers.
 *   - Complete animation states:
 *       * IDLE: predatory low stance, breathing sway, gleaming dagger tips.
 *       * RUNNING: multi-frame low sprint cycle with alternating strides.
 *       * JUMPING: athletic acrobatic leap with drawn daggers.
 *       * DASHING: shadow surge with smokey emerald afterimages.
 *       * ATTACKING: 3-phase anticipation, lunging strike, and recovery with twin venom slash trails.
 *       * HURT / DEFEAT: flinch recoil, impact flash, and collapse.
 *   - Automatic procedural fallback if artwork files are unavailable.
 */
public class AssassinRenderer implements CharacterRenderer {

    // Palette constants for Assassin (Nyx)
    private static final Color LEATHER_BASE    = new Color(0x12, 0x1A, 0x16);
    private static final Color LEATHER_DARK    = new Color(0x0A, 0x10, 0x0C);
    private static final Color TUNIC_GREEN     = new Color(0x1B, 0x4D, 0x34);
    private static final Color TUNIC_ACCENT    = new Color(0x27, 0x73, 0x4D);

    private static final Color EMERALD_GLOW    = new Color(0x38, 0xEF, 0x7D);
    private static final Color BLADE_STEEL     = new Color(0xDA, 0xE7, 0xF0);
    private static final Color BLADE_CORE      = new Color(0x8A, 0xA2, 0xB8);

    @Override
    public String getCharacterTitle() {
        return "Assassin";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.0f; // Standard hero scale
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

        BufferedImage sprite = HeroSpriteLoader.getHeroBody("nyx");
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

        if (state == AnimationState.ATTACKING && stateProgress >= 0.20f && stateProgress <= 0.80f) {
            float swing = (float) Math.sin((stateProgress - 0.20f) / 0.60f * Math.PI);
            Graphics2D arc = (Graphics2D) g.create();
            arc.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            arc.setColor(new Color(EMERALD_GLOW.getRed(), EMERALD_GLOW.getGreen(), EMERALD_GLOW.getBlue(),
                    (int) (210 * swing)));
            arc.drawArc(fx - 45, fy - 30, 90, 80, -55, 120);
            arc.setStroke(new BasicStroke(1.5f));
            arc.setColor(new Color(255, 255, 255, (int) (170 * swing)));
            arc.drawArc(fx - 36, fy - 18, 74, 64, -40, 105);
            arc.dispose();
        }

        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, stateProgress < 0.5f ? 1f - stateProgress : 0f);
        }
    }

    private void renderProceduralFallback(Graphics2D g, int centerX, int centerY, int baseSize,
                                          AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        float crouch = (float) Math.sin(idleTime * 2.8f) * 3.5f;
        float offsetX = 0;
        float offsetY = crouch + 4.0f;
        float legCycle = 0.0f;

        switch (state) {
            case RUNNING -> {
                legCycle = (float) Math.sin(idleTime * 8.5f);
                offsetY += Math.abs((float) Math.sin(idleTime * 8.5f)) * 4.0f;
            }
            case JUMPING -> {
                offsetY -= 14.0f;
            }
            case DASHING -> {
                offsetX = (facingRight ? 1 : -1) * 34.0f;
            }
            case ATTACKING -> {
                float surge = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? 1 : -1) * surge * 26.0f;
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
        drawArticulatedLegs(g, legCycle, state);
        drawTunicAndCorset(g);
        drawCowlAndMask(g);
        drawDaggers(g, state == AnimationState.ATTACKING);
    }

    private void drawArticulatedLegs(Graphics2D g, float legCycle, AnimationState state) {
        float backOffset = (state == AnimationState.RUNNING) ? -legCycle * 16.0f : 0;
        float frontOffset = (state == AnimationState.RUNNING) ? legCycle * 16.0f : 0;

        Path2D.Float backLeg = new Path2D.Float();
        backLeg.moveTo(-24 + backOffset, 15);
        backLeg.lineTo(-4 + backOffset, 15);
        backLeg.lineTo(-6 + backOffset * 0.5f, 68);
        backLeg.lineTo(-28 + backOffset * 0.5f, 68);
        backLeg.closePath();
        g.setPaint(new GradientPaint(-24, 15, LEATHER_BASE, -24, 68, LEATHER_DARK));
        g.fill(backLeg);

        Path2D.Float frontLeg = new Path2D.Float();
        frontLeg.moveTo(4 + frontOffset, 15);
        frontLeg.lineTo(24 + frontOffset, 15);
        frontLeg.lineTo(22 + frontOffset * 0.5f, 68);
        frontLeg.lineTo(2 + frontOffset * 0.5f, 68);
        frontLeg.closePath();
        g.setPaint(new GradientPaint(4, 15, TUNIC_GREEN, 4, 68, LEATHER_BASE));
        g.fill(frontLeg);
    }

    private void drawTunicAndCorset(Graphics2D g) {
        Path2D.Float tunic = new Path2D.Float();
        tunic.moveTo(-18, -30);
        tunic.lineTo(18, -30);
        tunic.lineTo(24, 0);
        tunic.lineTo(14, 20);
        tunic.lineTo(-14, 20);
        tunic.lineTo(-24, 0);
        tunic.closePath();
        g.setPaint(new GradientPaint(0, -30, TUNIC_ACCENT, 0, 20, TUNIC_GREEN));
        g.fill(tunic);
    }

    private void drawCowlAndMask(Graphics2D g) {
        Path2D.Float cowl = new Path2D.Float();
        cowl.moveTo(-14, -38);
        cowl.curveTo(-16, -65, 16, -65, 14, -38);
        cowl.lineTo(10, -28);
        cowl.lineTo(-10, -28);
        cowl.closePath();
        g.setPaint(new GradientPaint(0, -65, TUNIC_GREEN, 0, -28, LEATHER_DARK));
        g.fill(cowl);

        // Glowing emerald eyes
        g.setColor(EMERALD_GLOW);
        g.fillRect(-8, -44, 4, 3);
        g.fillRect(4, -44, 4, 3);
    }

    private void drawDaggers(Graphics2D g, boolean isAttacking) {
        // Forward dagger
        Graphics2D d1 = (Graphics2D) g.create();
        d1.translate(22, -10);
        d1.rotate(isAttacking ? -0.5f : 0.2f);
        Path2D.Float blade = new Path2D.Float();
        blade.moveTo(0, 0);
        blade.lineTo(25, -10);
        blade.lineTo(35, -5);
        blade.lineTo(15, 6);
        blade.closePath();
        d1.setPaint(new GradientPaint(0, 0, BLADE_STEEL, 35, 0, BLADE_CORE));
        d1.fill(blade);
        d1.setColor(EMERALD_GLOW);
        d1.setStroke(new BasicStroke(1.2f));
        d1.draw(blade);
        d1.dispose();
    }
}
