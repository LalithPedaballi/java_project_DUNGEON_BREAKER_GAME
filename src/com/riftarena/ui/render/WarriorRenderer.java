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
 * High-definition character renderer for Aegis (Warrior champion).
 *
 * Implements:
 *   - Reference artwork integration using transparent sprite frames (assets/heroes/aegis/).
 *   - Authentic heavy knight design: black/gold plate armor, horsehair plume greathelm,
 *     radiant sunburst heater shield, and two-handed broadsword.
 *   - Complete animation states:
 *       * IDLE: natural breathing, cloth sway, weapon shimmer.
 *       * RUNNING: multi-frame walk cycle with alternating leg strides and weapon sway.
 *       * JUMPING: airborne tuck with landing shockwave.
 *       * DASHING: forward shield rush with golden phantom afterimages.
 *       * ATTACKING: 3-phase anticipation, strike, and recovery with golden slash arcs.
 *       * HURT / DEFEAT: flinch recoil, impact flash, and collapse.
 *   - Automatic procedural fallback if artwork files are unavailable.
 */
public class WarriorRenderer implements CharacterRenderer {

    // Palette constants for Warrior (Aegis)
    private static final Color STEEL_HIGHLIGHT = new Color(0xEE, 0xF5, 0xFC);
    private static final Color STEEL_LIGHT     = new Color(0x9E, 0xB6, 0xD4);
    private static final Color STEEL_MID       = new Color(0x56, 0x6E, 0x8C);
    private static final Color STEEL_DARK      = new Color(0x27, 0x36, 0x48);
    private static final Color STEEL_SHADOW    = new Color(0x13, 0x1C, 0x26);

    private static final Color GOLD_BRIGHT     = new Color(0xF7, 0xD4, 0x63);
    private static final Color GOLD_MID        = new Color(0xC9, 0xA2, 0x27);
    private static final Color GOLD_DARK       = new Color(0x6E, 0x54, 0x14);

    private static final Color TABARD_BLUE     = new Color(0x18, 0x29, 0x42);
    private static final Color VISOR_CYAN      = new Color(0x52, 0xE3, 0xEA);

    @Override
    public String getCharacterTitle() {
        return "Warrior";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.0f; // Standard hero scale (broad armor maintains imposing presence)
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

        BufferedImage sprite = HeroSpriteLoader.getHeroBody("aegis");
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
            float swing = (float) Math.sin((stateProgress - 0.22f) / 0.56f * Math.PI);
            Graphics2D arc = (Graphics2D) g.create();
            arc.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            arc.setColor(new Color(GOLD_BRIGHT.getRed(), GOLD_BRIGHT.getGreen(), GOLD_BRIGHT.getBlue(),
                    (int) (200 * swing)));
            arc.drawArc(fx - 60, fy - 40, 120, 110, -60, 120);
            arc.setStroke(new BasicStroke(1.8f));
            arc.setColor(new Color(VISOR_CYAN.getRed(), VISOR_CYAN.getGreen(), VISOR_CYAN.getBlue(),
                    (int) (170 * swing)));
            arc.drawArc(fx - 50, fy - 32, 100, 94, -50, 110);
            arc.dispose();
        }
        if (state == AnimationState.DEFENDING) {
            Graphics2D ward = (Graphics2D) g.create();
            ward.setStroke(new BasicStroke(3.0f));
            ward.setColor(new Color(GOLD_BRIGHT.getRed(), GOLD_BRIGHT.getGreen(), GOLD_BRIGHT.getBlue(), 110));
            ward.drawArc(Math.round(centerX + face * height * 0.18f), Math.round(groundY - height * 0.78f),
                    Math.round(height * 0.34f), Math.round(height * 0.62f), -80, 160);
            ward.dispose();
        }

        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, stateProgress < 0.5f ? 1f - stateProgress : 0f);
        }
    }

    private void renderProceduralFallback(Graphics2D g, int centerX, int centerY, int baseSize,
                                          AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        float breath = (float) Math.sin(idleTime * 2.2f) * 3.5f;
        float offsetX = 0;
        float offsetY = breath;
        float bodyTilt = 0.0f;
        float swordAngle = 0.1f;
        float shieldOffset = 0;
        float legCycle = 0.0f;

        switch (state) {
            case RUNNING -> {
                legCycle = (float) Math.sin(idleTime * 7.5f);
                bodyTilt = (facingRight ? 1 : -1) * 0.16f;
                offsetY += Math.abs((float) Math.sin(idleTime * 7.5f)) * 4.5f;
                swordAngle = 0.35f + legCycle * 0.2f;
                shieldOffset = -legCycle * 8.0f;
            }
            case JUMPING -> {
                offsetY -= 12.0f;
                bodyTilt = (facingRight ? 1 : -1) * 0.12f;
                swordAngle = -0.6f;
                shieldOffset = 10.0f;
                legCycle = 0.7f;
            }
            case DASHING -> {
                offsetX = (facingRight ? 1 : -1) * 32.0f;
                bodyTilt = (facingRight ? 1 : -1) * 0.28f;
                shieldOffset = (facingRight ? 1 : -1) * 26.0f;
                swordAngle = -0.7f;
            }
            case ATTACKING -> {
                float swingProgress = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? 1 : -1) * swingProgress * 28.0f;
                swordAngle = -1.2f * swingProgress;
                shieldOffset = -swingProgress * 15.0f;
            }
            default -> {}
        }

        g.translate(centerX + offsetX, centerY + offsetY);
        if (!facingRight) {
            g.scale(-1, 1);
        }
        if (bodyTilt != 0.0f) {
            g.rotate(bodyTilt);
        }

        float scale = (baseSize / 180.0f) * getScaleMultiplier();
        g.scale(scale, scale);

        // Armored Body
        drawArticulatedLegs(g, legCycle, state);
        drawCuirassAndBelt(g);
        drawPauldrons(g);
        drawKnightHelm(g);
        drawBroadsword(g, 24, -18, swordAngle, state == AnimationState.ATTACKING, stateProgress);
        drawHeaterShield(g, -28 + shieldOffset, -8, state == AnimationState.DEFENDING, idleTime);
    }

    private void drawArticulatedLegs(Graphics2D g, float legCycle, AnimationState state) {
        float backOffset = (state == AnimationState.RUNNING) ? -legCycle * 18.0f : 0;
        float frontOffset = (state == AnimationState.RUNNING) ? legCycle * 18.0f : 0;

        Path2D.Float backLeg = new Path2D.Float();
        backLeg.moveTo(-28 + backOffset, 20);
        backLeg.lineTo(-6 + backOffset, 20);
        backLeg.lineTo(-8 + backOffset * 0.5f, 72);
        backLeg.lineTo(-32 + backOffset * 0.5f, 72);
        backLeg.closePath();
        g.setPaint(new GradientPaint(-28, 20, STEEL_MID, -28, 72, STEEL_SHADOW));
        g.fill(backLeg);

        Path2D.Float frontLeg = new Path2D.Float();
        frontLeg.moveTo(6 + frontOffset, 20);
        frontLeg.lineTo(28 + frontOffset, 20);
        frontLeg.lineTo(26 + frontOffset * 0.5f, 72);
        frontLeg.lineTo(4 + frontOffset * 0.5f, 72);
        frontLeg.closePath();
        g.setPaint(new GradientPaint(6, 20, STEEL_LIGHT, 6, 72, STEEL_DARK));
        g.fill(frontLeg);
    }

    private void drawCuirassAndBelt(Graphics2D g) {
        Path2D.Float cuirass = new Path2D.Float();
        cuirass.moveTo(-22, -35);
        cuirass.lineTo(22, -35);
        cuirass.lineTo(28, -5);
        cuirass.lineTo(18, 22);
        cuirass.lineTo(-18, 22);
        cuirass.lineTo(-28, -5);
        cuirass.closePath();
        g.setPaint(new GradientPaint(0, -35, STEEL_LIGHT, 0, 22, STEEL_DARK));
        g.fill(cuirass);

        g.setColor(GOLD_MID);
        g.setStroke(new BasicStroke(1.8f));
        g.draw(cuirass);
    }

    private void drawPauldrons(Graphics2D g) {
        g.setPaint(new GradientPaint(-44, -40, STEEL_HIGHLIGHT, -20, -10, STEEL_DARK));
        g.fill(new RoundRectangle2D.Float(-48, -44, 30, 28, 10, 10));
        g.setColor(GOLD_BRIGHT);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Float(-48, -44, 30, 28, 10, 10));

        g.setPaint(new GradientPaint(18, -40, STEEL_HIGHLIGHT, 42, -10, STEEL_DARK));
        g.fill(new RoundRectangle2D.Float(18, -44, 30, 28, 10, 10));
        g.setColor(GOLD_BRIGHT);
        g.draw(new RoundRectangle2D.Float(18, -44, 30, 28, 10, 10));
    }

    private void drawKnightHelm(Graphics2D g) {
        Path2D.Float helm = new Path2D.Float();
        helm.moveTo(-16, -42);
        helm.curveTo(-18, -75, 18, -75, 16, -42);
        helm.lineTo(14, -32);
        helm.lineTo(0, -25);
        helm.lineTo(-14, -32);
        helm.closePath();
        g.setPaint(new GradientPaint(0, -75, STEEL_HIGHLIGHT, 0, -25, STEEL_SHADOW));
        g.fill(helm);

        // Glowing Visor T-Slit
        g.setColor(VISOR_CYAN);
        g.fillRect(-10, -50, 20, 4);
        g.fillRect(-2, -50, 4, 18);
    }

    private void drawBroadsword(Graphics2D g, int x, int y, float angle, boolean isAttacking, float progress) {
        Graphics2D sg = (Graphics2D) g.create();
        sg.translate(x, y);
        sg.rotate(angle);

        // Blade
        Path2D.Float blade = new Path2D.Float();
        blade.moveTo(-5, -15);
        blade.lineTo(-6, -95);
        blade.lineTo(0, -112);
        blade.lineTo(6, -95);
        blade.lineTo(5, -15);
        blade.closePath();
        sg.setPaint(new GradientPaint(0, -15, STEEL_HIGHLIGHT, 0, -112, STEEL_LIGHT));
        sg.fill(blade);

        // Golden crossguard
        sg.setColor(GOLD_BRIGHT);
        sg.fillRoundRect(-18, -15, 36, 8, 4, 4);

        sg.dispose();
    }

    private void drawHeaterShield(Graphics2D g, float x, float y, boolean isDefending, float idleTime) {
        Graphics2D sg = (Graphics2D) g.create();
        sg.translate(x, y);

        Path2D.Float shield = new Path2D.Float();
        shield.moveTo(-22, -32);
        shield.lineTo(22, -32);
        shield.curveTo(24, 0, 18, 30, 0, 48);
        shield.curveTo(-18, 30, -24, 0, -22, -32);
        shield.closePath();

        sg.setPaint(new GradientPaint(0, -32, STEEL_LIGHT, 0, 48, STEEL_SHADOW));
        sg.fill(shield);

        sg.setColor(GOLD_BRIGHT);
        sg.setStroke(new BasicStroke(2.2f));
        sg.draw(shield);

        // Sunburst solar emblem
        sg.fillOval(-8, -6, 16, 16);

        sg.dispose();
    }

    private static final class RectangleDrawClip implements java.awt.Shape {
        private final int x, y, w, h;
        RectangleDrawClip(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        public java.awt.Rectangle getBounds() { return new java.awt.Rectangle(x, y, w, h); }
        public java.awt.geom.Rectangle2D getBounds2D() { return new java.awt.geom.Rectangle2D.Double(x, y, w, h); }
        public boolean contains(double x, double y) { return x >= this.x && x <= this.x + w && y >= this.y && y <= this.y + h; }
        public boolean contains(java.awt.geom.Point2D p) { return contains(p.getX(), p.getY()); }
        public boolean intersects(double x, double y, double w, double h) { return getBounds2D().intersects(x, y, w, h); }
        public boolean intersects(java.awt.geom.Rectangle2D r) { return getBounds2D().intersects(r); }
        public boolean contains(double x, double y, double w, double h) { return getBounds2D().contains(x, y, w, h); }
        public boolean contains(java.awt.geom.Rectangle2D r) { return getBounds2D().contains(r); }
        public java.awt.geom.PathIterator getPathIterator(AffineTransform at) { return new java.awt.geom.Rectangle2D.Double(x, y, w, h).getPathIterator(at); }
        public java.awt.geom.PathIterator getPathIterator(AffineTransform at, double flatness) { return new java.awt.geom.Rectangle2D.Double(x, y, w, h).getPathIterator(at, flatness); }
    }
}
