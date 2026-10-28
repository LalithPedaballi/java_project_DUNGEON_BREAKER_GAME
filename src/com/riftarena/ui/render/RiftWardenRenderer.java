package com.riftarena.ui.render;

import com.riftarena.game.RiftWarden;
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
 * High-fidelity 2D illustration and animation renderer for the multi-phase boss, The Rift Warden.
 *
 * Imposing 1.45x scale dark-fantasy titan featuring:
 *   - Jagged horned obsidian battle plate with ancient carved rift runes.
 *   - Floating runic megaliths that orbit in 3D perspective and crackle with energy arcs.
 *   - Colossal executioner halberd with a glowing serrated energy blade.
 *   - Distinct visual escalation across all 3 phases:
 *       * Phase I: Ominous violet rift aura, unbroken obsidian plates.
 *       * Phase II: Fractured chest armor exposing a roaring amber-gold abyssal core,
 *                   swirling vortex aura, and energized halberd.
 *       * Phase III: Cataclysmic deep-crimson storm aura, space-warping shock rings,
 *                    horns ablaze with fire, and blazing catastrophic plasma.
 */
public class RiftWardenRenderer implements CharacterRenderer {

    // Palette constants for Rift Warden
    private static final Color OBSIDIAN_DEEP  = new Color(0x08, 0x06, 0x0F);
    private static final Color OBSIDIAN_DARK  = new Color(0x16, 0x11, 0x22);
    private static final Color OBSIDIAN_MID   = new Color(0x2D, 0x23, 0x40);
    private static final Color OBSIDIAN_LIGHT = new Color(0x56, 0x44, 0x73);
    private static final Color STONE_EDGE     = new Color(0x8D, 0x77, 0xB0);

    private static final Color P1_VIOLET = new Color(0x8A, 0x66, 0xEB);
    private static final Color P2_GOLD   = new Color(0xF7, 0xC8, 0x43);
    private static final Color P3_DANGER = new Color(0xF0, 0x2B, 0x42);

    private RiftWarden.Phase currentPhase = RiftWarden.Phase.PHASE_I_GUARDIAN;

    @Override
    public String getCharacterTitle() {
        return "The Rift Warden";
    }

    @Override
    public float getScaleMultiplier() {
        return 1.45f; // Imposing boss scale
    }

    @Override
    public AnimationState getOffensiveState(boolean isSpecial) {
        return AnimationState.ATTACKING;
    }

    public void setPhase(RiftWarden.Phase phase) {
        if (phase != null) {
            this.currentPhase = phase;
        }
    }

    public RiftWarden.Phase getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public void render(Graphics2D g2, int centerX, int centerY, int baseSize,
                       AnimationState state, float stateProgress, float idleTime, boolean facingRight) {
        Graphics2D g = (Graphics2D) g2.create();
        AffineTransform originalTransform = g.getTransform();

        // 1. Pose offsets and titan breathing heave
        float heave = (float) Math.sin(idleTime * 2.0f) * 4.5f;
        float offsetX = 0;
        float offsetY = heave;
        float halberdAngle = 0.18f;
        float hurtFlash = 0.0f;

        switch (state) {
            case IDLE -> {
                halberdAngle = 0.18f + (float) Math.sin(idleTime * 1.6f) * 0.06f;
            }
            case RUNNING -> {
                // Heavy titan stride
                float stride = (float) Math.sin(idleTime * 6.0f);
                offsetX += (facingRight ? 1 : -1) * stride * 3.0f;
                halberdAngle = 0.28f + stride * 0.15f;
            }
            case JUMPING, DASHING -> {
                offsetY -= 12.0f;
                halberdAngle = -0.5f;
            }
            case ATTACKING, CASTING -> {
                // Earth-shattering overhead halberd cleave
                float lunge = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? 1 : -1) * lunge * 52.0f;
                offsetY += (float) Math.sin(stateProgress * Math.PI * 2) * 7.0f;
                // Halberd arcs downward from -1.5 rad to +1.3 rad
                halberdAngle = -1.5f + stateProgress * 2.8f;
            }
            case DEFENDING -> {
                // Halberd slammed into the stone floor, forming runic ward
                halberdAngle = 0.65f;
                offsetY += 6.0f;
            }
            case HURT -> {
                // Heavy titan shudder recoil
                float recoil = (float) Math.sin(stateProgress * Math.PI);
                offsetX = (facingRight ? -1 : 1) * recoil * 22.0f;
                offsetY -= recoil * 5.0f;
                halberdAngle = -recoil * 0.4f;
                hurtFlash = recoil;
            }
            case VICTORY -> {
                // Halberd raised skyward with lightning
                halberdAngle = -1.75f;
                offsetY -= 12.0f;
            }
            case DEFEAT -> {
                // Crumbling into petrified obsidian dust
                offsetY += 38.0f;
                halberdAngle = 0.9f;
            }
        }

        // Apply boss scale and position
        g.translate(centerX + offsetX, centerY + offsetY);
        if (facingRight) {
            g.scale(-1, 1);
        }

        float scale = (baseSize / 180.0f) * getScaleMultiplier();
        g.scale(scale, scale);

        BufferedImage malakorSprite = HeroSpriteLoader.getEnemySprite("malakor");
        if (malakorSprite != null) {
            // Soft ambient ground shadow firmly anchored on the ground plane
            RadialGradientPaint titanShadow = new RadialGradientPaint(
                    0, 0f, 90f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0, 0, 0, 160), new Color(0, 0, 0, 60), new Color(0, 0, 0, 0)}
            );
            g.setPaint(titanShadow);
            g.fill(new Ellipse2D.Float(-90, -12, 180, 24));

            // Colossal Phase Aura (behind boss, centered on chest)
            drawPhaseAura(g, idleTime);

            // Orbiting Runic Megaliths (behind plane)
            drawOrbitingRunes(g, idleTime, false);

            // Draw Malakor High-Res Artwork Cutout (feet aligned to ground y = 0)
            int mh = 190;
            int mw = (int) (mh * ((float) malakorSprite.getWidth() / malakorSprite.getHeight()));
            int mx = -mw / 2;
            int my = -mh;
            g.drawImage(malakorSprite, mx, my, mw, mh, null);

            // In Phase II / III (Cataclysm): Cataclysmic lightning arcs around the molten blade
            if (currentPhase != RiftWarden.Phase.PHASE_I_GUARDIAN) {
                g.setColor(new Color(255, 160, 40, 160));
                g.setStroke(new BasicStroke(1.5f));
                for (int l = 0; l < 3; l++) {
                    float lx = (float) Math.sin(idleTime * 8.0f + l * 2.0f) * 45f;
                    float ly = -120f + l * 40f;
                    g.drawLine((int) lx, (int) ly, (int) (lx + 8), (int) (ly + 14));
                }
            }

            // Attack Cleave Slash Arc
            if (state == AnimationState.ATTACKING) {
                float slashAngle = -1.5f + stateProgress * 2.8f;
                g.setColor(new Color(0xFF, 0x45, 0x00, 200));
                g.setStroke(new BasicStroke(4.0f));
                g.drawArc(-70, -160, 160, 180, (int) Math.toDegrees(slashAngle), 80);
            }

            // Defensive Runic Monolith Barrier
            if (state == AnimationState.DEFENDING) {
                drawMonolithBarrier(g, idleTime);
            }

            // Hurt flash
            if (hurtFlash > 0.05f) {
                g.setColor(new Color(255, 60, 60, (int) (hurtFlash * 145)));
                g.setClip(new Ellipse2D.Float(-75, -105, 150, 205));
                g.fillRect(-75, -105, 150, 205);
            }
        } else {
            // Procedural fallback
            g.setColor(new Color(0, 0, 0, 140));
            g.fill(new Ellipse2D.Float(-62, 70 - heave * 0.3f, 124, 26));
            drawPhaseAura(g, idleTime);
            drawOrbitingRunes(g, idleTime, false);
            drawExecutionerHalberd(g, -28, -20, halberdAngle, state == AnimationState.ATTACKING, stateProgress);
            drawColossalLegs(g);
            drawObsidianCuirassAndCore(g, idleTime);
            drawSpiredPauldrons(g);
            drawHornedHelm(g, idleTime);
            drawOrbitingRunes(g, idleTime, true);
            if (state == AnimationState.DEFENDING) {
                drawMonolithBarrier(g, idleTime);
            }
            if (hurtFlash > 0.05f) {
                g.setColor(new Color(255, 60, 60, (int) (hurtFlash * 145)));
                g.setClip(new Ellipse2D.Float(-75, -105, 150, 205));
                g.fillRect(-75, -105, 150, 205);
            }
        }

        g.setTransform(originalTransform);
        g.dispose();
    }

    private void drawPhaseAura(Graphics2D g, float idleTime) {
        float radius;
        Color coreColor;
        Color outerColor;

        switch (currentPhase) {
            case PHASE_I_GUARDIAN -> {
                radius = 70.0f + (float) Math.sin(idleTime * 2.5f) * 6.0f;
                coreColor = new Color(P1_VIOLET.getRed(), P1_VIOLET.getGreen(), P1_VIOLET.getBlue(), 60);
                outerColor = new Color(0, 0, 0, 0);
            }
            case PHASE_II_AWAKENING -> {
                radius = 90.0f + (float) Math.sin(idleTime * 4.0f) * 10.0f;
                coreColor = new Color(P2_GOLD.getRed(), P2_GOLD.getGreen(), P2_GOLD.getBlue(), 75);
                outerColor = new Color(P1_VIOLET.getRed(), P1_VIOLET.getGreen(), P1_VIOLET.getBlue(), 25);
            }
            case PHASE_III_CATACLYSM -> {
                radius = 110.0f + (float) Math.sin(idleTime * 6.0f) * 15.0f;
                coreColor = new Color(P3_DANGER.getRed(), P3_DANGER.getGreen(), P3_DANGER.getBlue(), 85);
                outerColor = new Color(P1_VIOLET.getRed(), P1_VIOLET.getGreen(), P1_VIOLET.getBlue(), 30);
            }
            default -> {
                radius = 70.0f;
                coreColor = P1_VIOLET;
                outerColor = new Color(0, 0, 0, 0);
            }
        }

        RadialGradientPaint aura = new RadialGradientPaint(0, -95, radius,
                new float[]{0f, 0.65f, 1f},
                new Color[]{coreColor, outerColor, new Color(0, 0, 0, 0)});
        g.setPaint(aura);
        g.fill(new Ellipse2D.Float(-radius, -10 - radius, radius * 2, radius * 2));
    }

    private void drawOrbitingRunes(Graphics2D g, float idleTime, boolean foreground) {
        float speed = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? 3.8f
                : (currentPhase == RiftWarden.Phase.PHASE_II_AWAKENING ? 2.6f : 1.6f);
        int runeCount = 3;

        Color runeColor = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? P3_DANGER
                : (currentPhase == RiftWarden.Phase.PHASE_II_AWAKENING ? P2_GOLD : P1_VIOLET);

        for (int i = 0; i < runeCount; i++) {
            float angle = idleTime * speed + (float) (i * Math.PI * 2 / runeCount);
            float sin = (float) Math.sin(angle);
            boolean isFront = sin > 0;

            if (isFront != foreground) {
                continue;
            }

            float rx = (float) Math.cos(angle) * 54.0f;
            float ry = -28 + sin * 18.0f;

            Graphics2D rg = (Graphics2D) g.create();
            rg.translate(rx, ry);
            rg.rotate(angle);

            // Floating faceted obsidian monolith
            Path2D.Float stone = new Path2D.Float();
            stone.moveTo(-7, -12);
            stone.lineTo(9, -9);
            stone.lineTo(7, 12);
            stone.lineTo(-9, 9);
            stone.closePath();

            rg.setPaint(new GradientPaint(-7, -12, STONE_EDGE, 7, 12, OBSIDIAN_DEEP));
            rg.fill(stone);
            rg.setColor(runeColor);
            rg.setStroke(new BasicStroke(1.8f));
            rg.draw(stone);

            // Carved glowing rune glyph
            rg.drawLine(0, -7, 0, 7);
            rg.drawLine(-4, 0, 4, 0);

            // Crackling lightning spark to titan's shoulder
            if (currentPhase != RiftWarden.Phase.PHASE_I_GUARDIAN) {
                rg.setColor(new Color(runeColor.getRed(), runeColor.getGreen(), runeColor.getBlue(), 120));
                rg.setStroke(new BasicStroke(1.2f));
                rg.drawLine(0, 0, (int) -rx / 2, (int) -ry / 2);
            }

            rg.dispose();
        }
    }

    private void drawColossalLegs(Graphics2D g) {
        // Colossal obsidian greaves
        g.setPaint(new GradientPaint(-32, 20, OBSIDIAN_LIGHT, -32, 72, OBSIDIAN_DEEP));
        g.fill(new RoundRectangle2D.Float(-36, 22, 26, 52, 7, 7));
        g.fill(new RoundRectangle2D.Float(12, 22, 26, 52, 7, 7));

        // Spiked knee guards with glowing runes
        g.setColor(OBSIDIAN_MID);
        g.fill(new RoundRectangle2D.Float(-38, 34, 30, 16, 5, 5));
        g.fill(new RoundRectangle2D.Float(10, 34, 30, 16, 5, 5));

        Color runeColor = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? P3_DANGER
                : (currentPhase == RiftWarden.Phase.PHASE_II_AWAKENING ? P2_GOLD : P1_VIOLET);
        g.setColor(runeColor);
        g.fillRect(-26, 40, 6, 4);
        g.fillRect(22, 40, 6, 4);

        // Sabatons (heavy armored boots)
        g.setPaint(new GradientPaint(-44, 68, STONE_EDGE, -44, 78, OBSIDIAN_DEEP));
        g.fill(new RoundRectangle2D.Float(-44, 68, 36, 13, 5, 5));
        g.fill(new RoundRectangle2D.Float(8, 68, 38, 13, 5, 5));
    }

    private void drawObsidianCuirassAndCore(Graphics2D g, float idleTime) {
        // Heavy obsidian breastplate
        Path2D.Float chest = new Path2D.Float();
        chest.moveTo(-32, 18);
        chest.lineTo(32, 18);
        chest.lineTo(38, -32);
        chest.lineTo(0, -42); // Raised gorget keel
        chest.lineTo(-38, -32);
        chest.closePath();

        g.setPaint(new GradientPaint(-38, -42, OBSIDIAN_LIGHT, 38, 18, OBSIDIAN_DEEP));
        g.fill(chest);

        Color coreGlowColor = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? P3_DANGER
                : (currentPhase == RiftWarden.Phase.PHASE_II_AWAKENING ? P2_GOLD : P1_VIOLET);

        // Fractured armor plates in Phase II & III
        if (currentPhase != RiftWarden.Phase.PHASE_I_GUARDIAN) {
            g.setColor(coreGlowColor);
            g.setStroke(new BasicStroke(2.6f));
            // Deep glowing fissure cracks
            g.drawLine(-22, -28, -8, -14);
            g.drawLine(22, -28, 8, -14);
            g.drawLine(-8, -14, 0, 8);
            g.drawLine(8, -14, 0, 8);
            g.drawLine(-24, 0, -8, -14);
            g.drawLine(24, 0, 8, -14);
        }

        // Central Abyssal Energy Core
        float coreY = -12;
        float corePulse = (float) Math.sin(idleTime * 4.5f) * 2.5f;
        float coreRadius = 9.0f + corePulse;
        if (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) {
            coreRadius += 5.0f; // Cataclysmic bursting core
        }

        RadialGradientPaint coreGlow = new RadialGradientPaint(0, coreY, coreRadius * 2.2f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{Color.WHITE, coreGlowColor, new Color(0, 0, 0, 0)});
        g.setPaint(coreGlow);
        g.fill(new Ellipse2D.Float(-coreRadius * 2.2f, coreY - coreRadius * 2.2f, coreRadius * 4.4f, coreRadius * 4.4f));

        // Border outline
        g.setColor(STONE_EDGE);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(chest);
    }

    private void drawSpiredPauldrons(Graphics2D g) {
        // Colossal Spired Pauldrons
        Path2D.Float leftP = new Path2D.Float();
        leftP.moveTo(-54, -22);
        leftP.lineTo(-28, -38);
        leftP.lineTo(-26, -10);
        leftP.lineTo(-62, -46); // High spired horn
        leftP.closePath();

        g.setPaint(new GradientPaint(-62, -46, STONE_EDGE, -26, -10, OBSIDIAN_DEEP));
        g.fill(leftP);

        Path2D.Float rightP = new Path2D.Float();
        rightP.moveTo(54, -22);
        rightP.lineTo(28, -38);
        rightP.lineTo(26, -10);
        rightP.lineTo(62, -46); // High spired horn
        rightP.closePath();

        g.setPaint(new GradientPaint(26, -46, STONE_EDGE, 62, -10, OBSIDIAN_DEEP));
        g.fill(rightP);

        g.setColor(OBSIDIAN_LIGHT.brighter());
        g.setStroke(new BasicStroke(1.8f));
        g.draw(leftP);
        g.draw(rightP);
    }

    private void drawHornedHelm(Graphics2D g, float idleTime) {
        // Demonic Horned Greathelm
        Path2D.Float helm = new Path2D.Float();
        helm.moveTo(-20, -38);
        helm.lineTo(20, -38);
        helm.lineTo(25, -62);
        helm.curveTo(20, -88, -20, -88, -25, -62);
        helm.closePath();

        g.setPaint(new GradientPaint(0, -88, STONE_EDGE, 0, -38, OBSIDIAN_DEEP));
        g.fill(helm);

        // Colossal Upcurving Demon Horns
        Path2D.Float leftHorn = new Path2D.Float();
        leftHorn.moveTo(-20, -68);
        leftHorn.curveTo(-44, -82, -48, -118, -34, -122);
        leftHorn.curveTo(-32, -106, -22, -86, -14, -78);
        leftHorn.closePath();

        Path2D.Float rightHorn = new Path2D.Float();
        rightHorn.moveTo(20, -68);
        rightHorn.curveTo(44, -82, 48, -118, 34, -122);
        rightHorn.curveTo(32, -106, 22, -86, 14, -78);
        rightHorn.closePath();

        g.setPaint(new GradientPaint(-34, -122, STONE_EDGE, -14, -68, OBSIDIAN_DEEP));
        g.fill(leftHorn);
        g.setPaint(new GradientPaint(14, -68, OBSIDIAN_DEEP, 34, -122, STONE_EDGE));
        g.fill(rightHorn);

        g.setColor(OBSIDIAN_LIGHT);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(leftHorn);
        g.draw(rightHorn);

        Color eyeColor = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? P3_DANGER
                : (currentPhase == RiftWarden.Phase.PHASE_II_AWAKENING ? P2_GOLD : P1_VIOLET);

        // Burning Horn Flames in Phase III
        if (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) {
            float flameFlicker = (float) Math.sin(idleTime * 8.0f) * 4.0f;
            g.setColor(new Color(P3_DANGER.getRed(), P3_DANGER.getGreen(), P3_DANGER.getBlue(), 180));
            g.fill(new Ellipse2D.Float(-38, -128 + flameFlicker, 12, 16));
            g.fill(new Ellipse2D.Float(26, -128 + flameFlicker, 12, 16));
        }

        // Burning Abyssal Eye Sockets
        g.setColor(eyeColor);
        g.fill(new Ellipse2D.Float(-11, -62, 7, 4.5f));
        g.fill(new Ellipse2D.Float(4, -62, 7, 4.5f));

        // Eye glare flares
        RadialGradientPaint eyeGlow = new RadialGradientPaint(0, -60, 18.0f,
                new float[]{0f, 0.45f, 1f},
                new Color[]{Color.WHITE, new Color(eyeColor.getRed(), eyeColor.getGreen(), eyeColor.getBlue(), 170),
                        new Color(0, 0, 0, 0)});
        g.setPaint(eyeGlow);
        g.fill(new Ellipse2D.Float(-18, -66, 36, 14));
    }

    private void drawExecutionerHalberd(Graphics2D g, float x, float y, float angle, boolean isAttacking, float progress) {
        Graphics2D hg = (Graphics2D) g.create();
        hg.translate(x, y);
        hg.rotate(angle);

        // Heavy Stone/Iron Shaft
        hg.setPaint(new GradientPaint(-5, -100, OBSIDIAN_MID, 5, 90, OBSIDIAN_DEEP));
        hg.fill(new RoundRectangle2D.Float(-5, -100, 10, 195, 4, 4));

        Color bladeColor = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? P3_DANGER
                : (currentPhase == RiftWarden.Phase.PHASE_II_AWAKENING ? P2_GOLD : P1_VIOLET);

        // Colossal Serrated Executioner Halberd Blade
        Path2D.Float blade = new Path2D.Float();
        blade.moveTo(5, -95);
        blade.lineTo(44, -120);
        blade.lineTo(50, -108);
        blade.lineTo(38, -96);
        blade.lineTo(54, -84); // Serrated pincer tooth
        blade.lineTo(36, -72);
        blade.lineTo(5, -66);
        blade.closePath();

        hg.setPaint(new GradientPaint(5, -120, Color.WHITE, 54, -66, bladeColor));
        hg.fill(blade);

        hg.setColor(bladeColor);
        hg.setStroke(new BasicStroke(2.2f));
        hg.draw(blade);

        // Opposing rear armor-piercing beak hook
        Path2D.Float hook = new Path2D.Float();
        hook.moveTo(-5, -90);
        hook.lineTo(-24, -82);
        hook.lineTo(-5, -74);
        hook.closePath();
        hg.setColor(OBSIDIAN_MID);
        hg.fill(hook);
        hg.setColor(bladeColor);
        hg.draw(hook);

        // Attack slash trail
        if (isAttacking && progress > 0.10f && progress < 0.90f) {
            hg.setStroke(new BasicStroke(6.0f));
            hg.setColor(new Color(bladeColor.getRed(), bladeColor.getGreen(), bladeColor.getBlue(), 220));
            hg.drawArc(-80, -155, 160, 160, 15, 110);

            hg.setStroke(new BasicStroke(2.5f));
            hg.setColor(Color.WHITE);
            hg.drawArc(-76, -151, 152, 152, 20, 100);
        }

        hg.dispose();
    }

    private void drawMonolithBarrier(Graphics2D g, float idleTime) {
        Graphics2D mg = (Graphics2D) g.create();
        mg.translate(40, -12);

        Path2D.Float monolith = new Path2D.Float();
        monolith.moveTo(-18, -75);
        monolith.lineTo(18, -75);
        monolith.lineTo(24, 75);
        monolith.lineTo(-24, 75);
        monolith.closePath();

        mg.setPaint(new GradientPaint(-18, -75, OBSIDIAN_MID, 18, 75, OBSIDIAN_DEEP));
        mg.fill(monolith);

        Color runeColor = (currentPhase == RiftWarden.Phase.PHASE_III_CATACLYSM) ? P3_DANGER : P1_VIOLET;
        mg.setColor(runeColor);
        mg.setStroke(new BasicStroke(2.8f));
        mg.draw(monolith);

        // Ancient glowing glyphs on monolith
        mg.drawLine(0, -60, 0, 60);
        mg.drawLine(-12, -30, 12, -30);
        mg.drawLine(-12, 20, 12, 20);

        mg.dispose();
    }
}
