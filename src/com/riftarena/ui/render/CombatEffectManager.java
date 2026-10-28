package com.riftarena.ui.render;

import com.riftarena.ui.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages transient visual combat effects, particle bursts, floating damage numbers,
 * and camera screen shakes.
 *
 * Keeps effects lightweight and ensures completed effects are automatically culled.
 *
 * Syllabus concepts demonstrated:
 *   - Object lifecycle management: collections, iterator removal, cleanups.
 *   - Mathematics of particle dynamics, gravity, and opacity decay.
 */
public class CombatEffectManager {

    private final List<CombatEffect> activeEffects = new ArrayList<>();
    private final Random rng = new Random();

    // Screen shake state
    private float shakeTimeRemaining = 0.0f;
    private float shakeMagnitude = 0.0f;
    private float shakeOffsetX = 0.0f;
    private float shakeOffsetY = 0.0f;

    public void clearAll() {
        activeEffects.clear();
        shakeTimeRemaining = 0.0f;
        shakeMagnitude = 0.0f;
        shakeOffsetX = 0.0f;
        shakeOffsetY = 0.0f;
    }

    public void addEffect(CombatEffect effect) {
        if (effect != null) {
            activeEffects.add(effect);
        }
    }

    public int getActiveEffectCount() {
        return activeEffects.size();
    }

    public List<CombatEffect> getEffects() {
        return Collections.unmodifiableList(activeEffects);
    }

    public float getShakeOffsetX() {
        return shakeOffsetX;
    }

    public float getShakeOffsetY() {
        return shakeOffsetY;
    }

    public boolean isScreenShakeActive() {
        return shakeTimeRemaining > 0.0f;
    }

    public void triggerScreenShake(float durationSeconds, float magnitude) {
        this.shakeTimeRemaining = Math.max(this.shakeTimeRemaining, durationSeconds);
        this.shakeMagnitude = Math.max(this.shakeMagnitude, magnitude);
    }

    public void update(float deltaSeconds) {
        // 1. Update screen shake
        if (shakeTimeRemaining > 0.0f) {
            shakeTimeRemaining -= deltaSeconds;
            if (shakeTimeRemaining <= 0.0f) {
                shakeOffsetX = 0.0f;
                shakeOffsetY = 0.0f;
                shakeMagnitude = 0.0f;
            } else {
                float damp = shakeTimeRemaining / 0.35f;
                shakeOffsetX = (rng.nextFloat() * 2.0f - 1.0f) * shakeMagnitude * damp;
                shakeOffsetY = (rng.nextFloat() * 2.0f - 1.0f) * shakeMagnitude * damp;
            }
        }

        // 2. Update active effects and cull expired ones
        activeEffects.removeIf(effect -> effect.update(deltaSeconds));
    }

    public void render(Graphics2D g2) {
        for (int i = 0; i < activeEffects.size(); i++) {
            activeEffects.get(i).render(g2);
        }
    }

    // ==================================================================
    // Factory Spawners for standard effects
    // ==================================================================

    public void spawnFloatingDamage(int x, int y, int amount, boolean isCritical) {
        Color color = isCritical ? Theme.GOLD_BRIGHT : Theme.DANGER;
        String text = isCritical ? "-" + amount + " CRIT!" : "-" + amount;
        addEffect(new FloatingNumberEffect(x, y, text, color, isCritical ? 24 : 20));
    }

    public void spawnFloatingHeal(int x, int y, int amount) {
        Color healGreen = new Color(0x57, 0xF2, 0x87);
        addEffect(new FloatingNumberEffect(x, y, "+" + amount, healGreen, 20));
    }

    public void spawnFloatingShield(int x, int y, String text) {
        Color shieldCyan = new Color(0x64, 0xDF, 0xDF);
        addEffect(new FloatingNumberEffect(x, y, text, shieldCyan, 18));
    }

    public void spawnSlash(int x, int y, Color color, float radius, boolean flip) {
        addEffect(new SlashEffect(x, y, color, radius, flip));
    }

    public void spawnSpellBurst(int x, int y, Color color) {
        addEffect(new SpellBurstEffect(x, y, color));
    }

    public void spawnShieldFlash(int x, int y, Color color) {
        addEffect(new ShieldFlashEffect(x, y, color));
    }

    public void spawnPhaseTransition(int x, int y, String banner, Color color) {
        addEffect(new PhaseShockwaveEffect(x, y, banner, color));
        triggerScreenShake(0.5f, 7.0f);
    }

    public void spawnHitSparks(int x, int y, Color color, float knockbackDir) {
        addEffect(new HitSparksEffect(x, y, color, knockbackDir, rng));
    }

    public void spawnDeathDissolution(int x, int y, Color color, boolean isBoss) {
        addEffect(new DeathDissolutionEffect(x, y, color, isBoss));
        if (isBoss) {
            triggerScreenShake(0.65f, 8.5f);
        } else {
            triggerScreenShake(0.30f, 4.0f);
        }
    }

    // ==================================================================
    // Concrete effect implementations
    // ==================================================================

    public static class FloatingNumberEffect implements CombatEffect {
        private float x;
        private float y;
        private final String text;
        private final Color color;
        private final int fontSize;
        private float age = 0.0f;
        private static final float LIFETIME = 0.85f;

        public FloatingNumberEffect(float x, float y, String text, Color color, int fontSize) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.fontSize = fontSize;
        }

        public String getText() {
            return text;
        }

        public Color getColor() {
            return color;
        }

        @Override
        public boolean update(float deltaSeconds) {
            age += deltaSeconds;
            // Float upward with slowing velocity
            y -= 45.0f * deltaSeconds * (1.0f - (age / LIFETIME) * 0.5f);
            return age >= LIFETIME;
        }

        @Override
        public void render(Graphics2D g2) {
            float progress = age / LIFETIME;
            float alpha = 1.0f;
            if (progress > 0.6f) {
                alpha = Math.max(0.0f, 1.0f - ((progress - 0.6f) / 0.4f));
            }

            int alphaInt = (int) (alpha * 255);
            if (alphaInt <= 0) return;

            Font font = new Font("SansSerif", Font.BOLD, fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            int textW = fm.stringWidth(text);
            int drawX = (int) (x - textW / 2);
            int drawY = (int) y;

            // Shadow outline
            g2.setColor(new Color(0, 0, 0, (int) (alpha * 220)));
            g2.drawString(text, drawX + 2, drawY + 2);
            g2.drawString(text, drawX - 1, drawY - 1);
            g2.drawString(text, drawX + 1, drawY - 1);
            g2.drawString(text, drawX - 1, drawY + 1);

            // Colored core text
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt));
            g2.drawString(text, drawX, drawY);
        }
    }

    public static class SlashEffect implements CombatEffect {
        private final float x;
        private final float y;
        private final Color color;
        private final float radius;
        private final boolean flip;
        private float age = 0.0f;
        private static final float LIFETIME = 0.28f;

        public SlashEffect(float x, float y, Color color, float radius, boolean flip) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.radius = radius;
            this.flip = flip;
        }

        @Override
        public boolean update(float deltaSeconds) {
            age += deltaSeconds;
            return age >= LIFETIME;
        }

        @Override
        public void render(Graphics2D g2) {
            float t = age / LIFETIME;
            float alpha = Math.max(0.0f, 1.0f - t);
            int alphaInt = (int) (alpha * 220);

            Graphics2D sg = (Graphics2D) g2.create();
            sg.translate(x, y);
            if (flip) {
                sg.scale(-1, 1);
            }

            float currentRadius = radius * (0.8f + t * 0.4f);
            sg.setStroke(new BasicStroke(4.5f * (1.0f - t * 0.7f)));
            sg.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt));
            sg.drawArc((int) -currentRadius, (int) -currentRadius, (int) (currentRadius * 2), (int) (currentRadius * 2), -45, 110);

            // Bright white interior blade gleam
            sg.setStroke(new BasicStroke(2.0f));
            sg.setColor(new Color(255, 255, 255, alphaInt));
            sg.drawArc((int) -currentRadius + 2, (int) -currentRadius + 2,
                    (int) (currentRadius * 2 - 4), (int) (currentRadius * 2 - 4), -40, 100);

            sg.dispose();
        }
    }

    public static class SpellBurstEffect implements CombatEffect {
        private final float x;
        private final float y;
        private final Color color;
        private float age = 0.0f;
        private static final float LIFETIME = 0.42f;
        private final List<MagicSpark> sparks = new ArrayList<>();

        private static class MagicSpark {
            float vx, vy, size;
            MagicSpark(float vx, float vy, float size) {
                this.vx = vx;
                this.vy = vy;
                this.size = size;
            }
        }

        public SpellBurstEffect(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            Random r = new Random((long) (x * 1000 + y));
            for (int i = 0; i < 12; i++) {
                double angle = i * (Math.PI * 2 / 12) + (r.nextFloat() - 0.5f) * 0.35f;
                float speed = 75.0f + r.nextFloat() * 120.0f;
                sparks.add(new MagicSpark((float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, 2.5f + r.nextFloat() * 2.5f));
            }
        }

        @Override
        public boolean update(float deltaSeconds) {
            age += deltaSeconds;
            return age >= LIFETIME;
        }

        @Override
        public void render(Graphics2D g2) {
            float t = age / LIFETIME;
            float alpha = Math.max(0.0f, 1.0f - t);
            float coreRadius = 16.0f + t * 45.0f;

            // Luminous arcane core glow
            RadialGradientPaint coreGlow = new RadialGradientPaint(
                    x, y, Math.max(1f, coreRadius),
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{
                            new Color(255, 255, 255, (int) (alpha * 220)),
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 160)),
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
                    }
            );
            g2.setPaint(coreGlow);
            g2.fill(new Ellipse2D.Float(x - coreRadius, y - coreRadius, coreRadius * 2, coreRadius * 2));

            // Organic radiating magic sparks
            for (MagicSpark spark : sparks) {
                float px = x + spark.vx * age;
                float py = y + spark.vy * age;
                float sparkSize = spark.size * (1.0f - t * 0.4f);
                g2.setColor(new Color(255, 255, 255, (int) (alpha * 230)));
                g2.fill(new Ellipse2D.Float(px - sparkSize / 2, py - sparkSize / 2, sparkSize, sparkSize));
            }
        }
    }

    public static class ShieldFlashEffect implements CombatEffect {
        private final float x;
        private final float y;
        private final Color color;
        private float age = 0.0f;
        private static final float LIFETIME = 0.35f;

        public ShieldFlashEffect(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        @Override
        public boolean update(float deltaSeconds) {
            age += deltaSeconds;
            return age >= LIFETIME;
        }

        @Override
        public void render(Graphics2D g2) {
            float t = age / LIFETIME;
            float alpha = Math.max(0.0f, 1.0f - t);
            float radius = 45.0f + t * 15.0f;

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 90)));
            g2.fill(new Ellipse2D.Float(x - radius * 0.5f, y - radius, radius, radius * 2));

            g2.setStroke(new BasicStroke(2.5f * (1.0f - t * 0.5f)));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 220)));
            g2.draw(new Ellipse2D.Float(x - radius * 0.5f, y - radius, radius, radius * 2));
        }
    }

    public static class PhaseShockwaveEffect implements CombatEffect {
        private final float x;
        private final float y;
        private final String banner;
        private final Color color;
        private float age = 0.0f;
        private static final float LIFETIME = 0.95f;

        public PhaseShockwaveEffect(float x, float y, String banner, Color color) {
            this.x = x;
            this.y = y;
            this.banner = banner;
            this.color = color;
        }

        @Override
        public boolean update(float deltaSeconds) {
            age += deltaSeconds;
            return age >= LIFETIME;
        }

        @Override
        public void render(Graphics2D g2) {
            float t = age / LIFETIME;
            float alpha = Math.max(0.0f, 1.0f - t);
            float radius = 15.0f + t * 140.0f;

            // Atmospheric horizon shockwave pulse (no wireframe circles)
            g2.setStroke(new BasicStroke(2.0f * (1.0f - t)));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 120)));
            g2.drawLine((int) (x - radius * 1.8f), (int) y, (int) (x + radius * 1.8f), (int) y);

            // Floating banner text
            Font bannerFont = Theme.HEADING_FONT;
            g2.setFont(bannerFont);
            FontMetrics fm = g2.getFontMetrics(bannerFont);
            int tw = fm.stringWidth(banner);
            float by = y - 60.0f - t * 30.0f;

            g2.setColor(new Color(0, 0, 0, (int) (alpha * 200)));
            g2.drawString(banner, (int) (x - tw / 2) + 2, (int) by + 2);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            g2.drawString(banner, (int) (x - tw / 2), (int) by);
        }
    }

    public static class HitSparksEffect implements CombatEffect {
        private final float x;
        private final float y;
        private final Color color;
        private final float knockbackDir;
        private final List<SparkParticle> sparks = new ArrayList<>();
        private float age = 0.0f;
        private static final float LIFETIME = 0.32f;

        private static class SparkParticle {
            float px, py, vx, vy, size;
            SparkParticle(float px, float py, float vx, float vy, float size) {
                this.px = px; this.py = py; this.vx = vx; this.vy = vy; this.size = size;
            }
        }

        public HitSparksEffect(float x, float y, Color color, float knockbackDir, Random rng) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.knockbackDir = knockbackDir;
            int count = 9;
            for (int i = 0; i < count; i++) {
                float baseAngle = (knockbackDir > 0) ? 0.0f : (float) Math.PI;
                float angle = baseAngle + (rng.nextFloat() - 0.5f) * 1.5f;
                float speed = 130.0f + rng.nextFloat() * 190.0f;
                float vx = (float) Math.cos(angle) * speed;
                float vy = (float) Math.sin(angle) * speed - 70.0f;
                sparks.add(new SparkParticle(x, y, vx, vy, 2.5f + rng.nextFloat() * 2.5f));
            }
        }

        @Override
        public boolean update(float dt) {
            age += dt;
            for (SparkParticle s : sparks) {
                s.px += s.vx * dt;
                s.py += s.vy * dt;
                s.vy += 420.0f * dt; // gravity
            }
            return age >= LIFETIME;
        }

        @Override
        public void render(Graphics2D g2) {
            float progress = age / LIFETIME;
            float alpha = Math.max(0.0f, 1.0f - progress);
            int alphaInt = (int) (alpha * 255);
            if (alphaInt <= 0) return;

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt));
            for (SparkParticle s : sparks) {
                g2.fill(new Ellipse2D.Float(s.px - s.size * 0.5f, s.py - s.size * 0.5f, s.size, s.size));
            }
            g2.setColor(new Color(255, 255, 255, (int) (alpha * 220)));
            for (SparkParticle s : sparks) {
                g2.fill(new Ellipse2D.Float(s.px - 1, s.py - 1, 2, 2));
            }
        }
    }

    public static class DeathDissolutionEffect implements CombatEffect {
        private final float x;
        private final float y;
        private final Color color;
        private final boolean isBoss;
        private float age = 0.0f;
        private final float lifetime;

        public DeathDissolutionEffect(float x, float y, Color color, boolean isBoss) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isBoss = isBoss;
            this.lifetime = isBoss ? 1.25f : 0.65f;
        }

        @Override
        public boolean update(float dt) {
            age += dt;
            return age >= lifetime;
        }

        @Override
        public void render(Graphics2D g2) {
            float progress = age / lifetime;
            float alpha = Math.max(0.0f, 1.0f - progress);
            int alphaInt = (int) (alpha * 240);
            if (alphaInt <= 0) return;

            float maxRadius = isBoss ? 165.0f : 85.0f;
            float currentRadius = 15.0f + progress * maxRadius;

            // Expanding void energy shockwave ring
            g2.setStroke(new BasicStroke((isBoss ? 5.0f : 3.0f) * (1.0f - progress * 0.7f)));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt));
            g2.draw(new Ellipse2D.Float(x - currentRadius, y - currentRadius, currentRadius * 2, currentRadius * 2));

            // Interior dissipation glow
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 65)));
            g2.fill(new Ellipse2D.Float(x - currentRadius * 0.6f, y - currentRadius * 0.6f, currentRadius * 1.2f, currentRadius * 1.2f));

            // Rising soul motes
            int motes = isBoss ? 16 : 8;
            for (int i = 0; i < motes; i++) {
                double angle = (i * Math.PI * 2 / motes) + progress * 1.5;
                float dist = currentRadius * 0.75f;
                float mx = (float) (x + Math.cos(angle) * dist);
                float my = (float) (y + Math.sin(angle) * dist - progress * 40.0f);
                float size = (isBoss ? 5.5f : 3.5f) * (1.0f - progress * 0.5f);
                g2.setColor(new Color(255, 255, 255, (int) (alpha * 230)));
                g2.fill(new Ellipse2D.Float(mx - size * 0.5f, my - size * 0.5f, size, size));
            }
        }
    }
}

