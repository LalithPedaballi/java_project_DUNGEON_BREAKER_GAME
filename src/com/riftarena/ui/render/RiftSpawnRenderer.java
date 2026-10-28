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
 * Premium 2D illustration and animation renderer for the Rift Spawn void enemy.
 *
 * Visual design:
 *   - Multi-tiered void chitin carapace with iridescent violet edge highlights.
 *   - Cyclopean abyssal rift eye with dilating cyan-magenta pupil.
 *   - Twin razor scythe claws with serrated cutting edges.
 *   - Undulating shadow rift vapors and tentacles at the base.
 */
public class RiftSpawnRenderer implements CharacterRenderer {

    // Palette constants for Rift Spawn
    private static final Color VOID_DEEP   = new Color(0x0A, 0x04, 0x14);
    private static final Color VOID_DARK   = new Color(0x1B, 0x0C, 0x30);
    private static final Color VOID_MID    = new Color(0x40, 0x1E, 0x6E);
    private static final Color VOID_LIGHT  = new Color(0x78, 0x44, 0xC4);
    private static final Color CHITIN_EDGE = new Color(0xBD, 0x85, 0xF5);

    private static final Color EYE_CYAN    = new Color(0x00, 0xF5, 0xD4);
    private static final Color EYE_MAGENTA = new Color(0xE0, 0x2E, 0x8D);

    @Override
    public String getCharacterTitle() {
        return "Rift Spawn";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.0f;
    }

    @Override
    public AnimationState getOffensiveState(boolean isSpecial) {
        return AnimationState.ATTACKING;
    }

    /** Artwork used for this enemy and how tall it stands relative to a champion. */
    private String spriteKey = "rift_thrall";
    private float heightScale = 0.80f;

    /** Chooses which painted creature this renderer shows (called per encounter). */
    public void setArtwork(String spriteKey, float heightScale) {
        this.spriteKey = spriteKey;
        this.heightScale = heightScale;
    }

    @Override
    public void render(Graphics2D g2, int centerX, int groundY, int baseSize,
                       AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        BufferedImage artwork = HeroSpriteLoader.getEnemySprite(spriteKey);
        if (artwork != null) {
            renderArtwork(g2, artwork, centerX, groundY, baseSize, state, stateProgress, idleTime, facingRight);
            return;
        }
        renderProcedural(g2, centerX, groundY, baseSize, state, stateProgress, idleTime, facingRight);
    }

    private void renderArtwork(Graphics2D g2, BufferedImage artwork, int centerX, int groundY, int baseSize,
                               AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        Graphics2D g = (Graphics2D) g2.create();
        float height = baseSize * heightScale;
        FigureRenderer.Pose pose = FigureRenderer.poseFor(state, stateProgress, idleTime);

        FigureRenderer.shadow(g, centerX, groundY, height * 0.62f, state == AnimationState.JUMPING ? 0.5f : 1.0f);
        if (state == AnimationState.DASHING) {
            FigureRenderer.dashTrail(g, artwork, centerX, groundY, height, facingRight, pose);
        }
        FigureRenderer.draw(g, artwork, centerX, groundY, height, facingRight, pose);

        float face = facingRight ? 1f : -1f;
        if (state == AnimationState.ATTACKING) {
            float slash = (float) Math.sin(stateProgress * Math.PI);
            Graphics2D arc = (Graphics2D) g.create();
            arc.setStroke(new BasicStroke(3.0f));
            arc.setColor(new Color(0xBD, 0x85, 0xF5, (int) (220 * slash)));
            int arcX = Math.round(centerX + face * height * 0.22f);
            int arcY = Math.round(groundY - height * 0.62f);
            int radius = Math.round(height * 0.20f);
            arc.drawArc(arcX - radius, arcY - radius, radius * 2, radius * 2,
                    facingRight ? -50 : 130, 90);
            arc.dispose();
        }
        if (state == AnimationState.HURT) {
            FigureRenderer.hurtFlash(g, centerX, groundY, height, 1f - stateProgress);
        }
        g.dispose();
    }

    private void renderProcedural(Graphics2D g2, int centerX, int centerY, int baseSize,
                                  AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        Graphics2D g = (Graphics2D) g2.create();
        AffineTransform originalTransform = g.getTransform();

        // 1. Pose offsets and hovering levitation
        float pulse = (float) Math.sin(idleTime * 3.2f) * 4.5f;
        float offsetX = 0;
        float offsetY = pulse;
        float clawAngle = 0.1f;
        float hurtFlash = 0.0f;

        switch (state) {
            case IDLE -> {
                clawAngle = 0.1f + (float) Math.sin(idleTime * 2.2f) * 0.08f;
            }
            case RUNNING -> {
                // Skittering void stalk
                offsetX += (float) Math.sin(idleTime * 8.0f) * 4.0f;
                clawAngle = 0.35f + (float) Math.sin(idleTime * 8.0f) * 0.25f;
            }
            case JUMPING, DASHING -> {
                offsetY -= 14.0f;
                clawAngle = -0.6f;
            }
            case ATTACKING, CASTING -> {
                // Forward lunge and scissor pincer scythe snap
                float lunge = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? 1 : -1) * lunge * 48.0f;
                offsetY += (float) Math.sin(stateProgress * Math.PI * 2) * 5.0f;
                clawAngle = -1.35f * lunge;
            }
            case DEFENDING -> {
                // Folded claws forming defensive chitinous shell
                clawAngle = 0.9f;
                offsetY += 5.0f;
            }
            case HURT -> {
                // Flinch back with screech
                float recoil = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? -1 : 1) * recoil * 28.0f;
                offsetY -= recoil * 7.0f;
                clawAngle = recoil * 0.6f;
                hurtFlash = recoil;
            }
            case VICTORY -> {
                offsetY -= 16.0f;
                clawAngle = -1.05f;
            }
            case DEFEAT -> {
                offsetY += 32.0f;
                clawAngle = 1.35f;
            }
        }

        float scale = (baseSize / 180.0f) * getScaleMultiplier();
        // The procedural figure's own ground line sits at local y = 70; lift it so its
        // feet rest exactly on the arena ground line.
        g.translate(centerX + offsetX, centerY - 70.0f * scale + offsetY);
        if (!facingRight) {
            g.scale(-1, 1);
        }

        g.scale(scale, scale);

        // Procedural fallback (used only if the artwork cannot be loaded)
            g.setColor(new Color(0, 0, 0, 140));
            g.fill(new Ellipse2D.Float(-50, 68 - pulse * 0.5f, 100, 24));
            drawBaseTentacles(g, idleTime);
            drawCarapaceBody(g, idleTime);
            drawRiftEye(g, idleTime, hurtFlash > 0.1f);
            drawScytheClaw(g, -32, 10, -clawAngle * 0.7f, false);
            drawScytheClaw(g, 32, 8, clawAngle, true);
            if (hurtFlash > 0.05f) {
                g.setColor(new Color(255, 60, 60, (int) (hurtFlash * 155)));
                g.setClip(new Ellipse2D.Float(-50, -80, 100, 160));
                g.fillRect(-50, -80, 100, 160);
            }

        g.setTransform(originalTransform);
        g.dispose();
    }

    private void drawBaseTentacles(Graphics2D g, float idleTime) {
        Graphics2D tg = (Graphics2D) g.create();
        for (int i = -2; i <= 2; i++) {
            float phase = idleTime * 3.5f + i * 1.1f;
            float tx = i * 14.0f;
            float sw = (float) Math.sin(phase) * 8.0f;

            Path2D.Float tentacle = new Path2D.Float();
            tentacle.moveTo(tx - 4, 34);
            tentacle.curveTo(tx - 6, 46, tx + sw, 58, tx + sw * 1.4f, 70);
            tentacle.curveTo(tx + 6, 58, tx + 6, 46, tx + 4, 34);
            tentacle.closePath();

            tg.setPaint(new GradientPaint(tx, 34, VOID_DARK, tx + sw, 70, VOID_DEEP));
            tg.fill(tentacle);
            tg.setColor(new Color(CHITIN_EDGE.getRed(), CHITIN_EDGE.getGreen(), CHITIN_EDGE.getBlue(), 60));
            tg.setStroke(new BasicStroke(1.0f));
            tg.draw(tentacle);
        }
        tg.dispose();
    }

    private void drawCarapaceBody(Graphics2D g, float idleTime) {
        // Lower abdomen
        g.setPaint(new GradientPaint(-24, 0, VOID_MID, 24, 40, VOID_DEEP));
        g.fill(new RoundRectangle2D.Float(-26, 0, 52, 38, 16, 16));

        // Thorax plates (Layer 1)
        Path2D.Float thorax = new Path2D.Float();
        thorax.moveTo(-36, -30);
        thorax.lineTo(36, -30);
        thorax.lineTo(28, 12);
        thorax.lineTo(-28, 12);
        thorax.closePath();

        g.setPaint(new GradientPaint(-36, -30, VOID_LIGHT, 36, 12, VOID_DARK));
        g.fill(thorax);
        g.setColor(CHITIN_EDGE);
        g.setStroke(new BasicStroke(1.8f));
        g.draw(thorax);

        // Segmented dorsal armor ridges
        g.setColor(VOID_DARK);
        g.fill(new RoundRectangle2D.Float(-20, -22, 40, 8, 3, 3));
        g.fill(new RoundRectangle2D.Float(-16, -10, 32, 8, 3, 3));
        g.fill(new RoundRectangle2D.Float(-12, 2, 24, 8, 3, 3));

        // Upper carapace crest / Horns
        Path2D.Float head = new Path2D.Float();
        head.moveTo(-28, -32);
        head.lineTo(-38, -64);
        head.lineTo(-20, -52);
        head.lineTo(0, -66);
        head.lineTo(20, -52);
        head.lineTo(38, -64);
        head.lineTo(28, -32);
        head.closePath();

        g.setPaint(new GradientPaint(0, -66, VOID_LIGHT.brighter(), 0, -32, VOID_DEEP));
        g.fill(head);
        g.setColor(CHITIN_EDGE);
        g.setStroke(new BasicStroke(2.0f));
        g.draw(head);
    }

    private void drawRiftEye(Graphics2D g, float idleTime, boolean isHurt) {
        int eyX = 0;
        int eyY = -34;

        // Eye socket glow
        RadialGradientPaint socket = new RadialGradientPaint(eyX, eyY, 26f,
                new float[]{0f, 0.4f, 1f},
                new Color[]{new Color(0xE0, 0x2E, 0x8D, 180), new Color(0x60, 0x10, 0x80, 80), new Color(0, 0, 0, 0)});
        g.setPaint(socket);
        g.fillOval(eyX - 26, eyY - 26, 52, 52);

        // Large sclera
        g.setColor(isHurt ? Color.WHITE : EYE_CYAN);
        g.fillOval(eyX - 16, eyY - 16, 32, 32);

        // Dilating vertical slit pupil
        float dilate = (float) Math.sin(idleTime * 4.0f) * 2.0f + 5.0f;
        g.setColor(new Color(0x18, 0x05, 0x24));
        g.fill(new RoundRectangle2D.Float(eyX - dilate / 2, eyY - 14, dilate, 28, 4, 4));

        // Glint highlight
        g.setColor(Color.WHITE);
        g.fillOval(eyX + 3, eyY - 9, 6, 6);
    }

    private void drawScytheClaw(Graphics2D g, float x, float y, float angle, boolean isFront) {
        Graphics2D cg = (Graphics2D) g.create();
        cg.translate(x, y);
        cg.rotate(angle);

        // Forearm bone segment
        cg.setPaint(new GradientPaint(-6, -10, VOID_LIGHT, 6, 20, VOID_DEEP));
        cg.fill(new RoundRectangle2D.Float(-6, -10, 12, 34, 4, 4));
        cg.setColor(CHITIN_EDGE);
        cg.setStroke(new BasicStroke(1.2f));
        cg.drawRoundRect(-6, -10, 12, 34, 4, 4);

        // Serrated Scythe Blade (Length 65px)
        Path2D.Float scythe = new Path2D.Float();
        scythe.moveTo(-6, 24);
        scythe.lineTo(-14, 55);
        scythe.lineTo(0, 82); // Needle pincer point
        scythe.lineTo(16, 48);
        scythe.lineTo(6, 24);
        scythe.closePath();

        cg.setPaint(new GradientPaint(-6, 24, CHITIN_EDGE, 0, 82, VOID_DEEP));
        cg.fill(scythe);
        cg.setColor(EYE_CYAN);
        cg.setStroke(new BasicStroke(1.8f));
        cg.draw(scythe);

        // Razor spine teeth on blade edge
        cg.setColor(Color.WHITE);
        cg.drawLine(-6, 34, -12, 38);
        cg.drawLine(-8, 46, -14, 52);

        cg.dispose();
    }
}
