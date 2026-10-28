package com.riftarena.ui.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws a cut-out character figure onto the arena ground plane and gives it a pose.
 *
 * Every figure is anchored by the bottom-center of its own visible artwork, not by the
 * bottom of the PNG, so a sprite never floats above the floor or sinks into it just
 * because its image has transparent padding. The pose also pivots around the feet, which
 * keeps a walking or attacking character standing on the same spot instead of sliding.
 *
 * Motion is produced from the single painted illustration each fighter owns: torso lean,
 * leg swing about the hips, breathing scale, stride bob and a soft contact shadow.
 *
 * Syllabus concepts demonstrated:
 *   - AffineTransform composition and pivoting (Unit IV: 2D graphics).
 *   - Caching derived data (bounds) for per-frame performance.
 */
public final class FigureRenderer {

    /** Visible artwork rectangle inside the PNG, in image pixels. */
    public record Bounds(int x, int y, int width, int height) {
        public boolean isEmpty() {
            return width <= 0 || height <= 0;
        }
    }

    /**
     * A single frame of figure motion. Offsets are fractions of the figure height so the
     * same pose reads correctly at any on-screen size.
     */
    public static final class Pose {
        public float sway;      // side-to-side shift
        public float lift;      // vertical offset, negative is upwards
        public float lean;      // degrees, rotation about the feet
        public float stride;    // degrees, legs swing about the hips
        public float breathe;   // -1..1 chest expansion
        public float squash = 1f; // 1 = neutral, < 1 compressed on landing
        public float alpha = 1f;

        public Pose copy() {
            Pose p = new Pose();
            p.sway = sway;
            p.lift = lift;
            p.lean = lean;
            p.stride = stride;
            p.breathe = breathe;
            p.squash = squash;
            p.alpha = alpha;
            return p;
        }
    }

    private static final Map<String, Bounds> BOUNDS_CACHE = new HashMap<>();

    private FigureRenderer() {
    }

    // ------------------------------------------------------------------
    // Artwork bounds
    // ------------------------------------------------------------------

    /** Returns the rectangle actually covered by the figure, ignoring transparent padding. */
    public static Bounds bounds(BufferedImage image) {
        if (image == null) {
            return new Bounds(0, 0, 0, 0);
        }
        String key = System.identityHashCode(image) + ":" + image.getWidth() + "x" + image.getHeight();
        Bounds cached = BOUNDS_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 12) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        Bounds found = (maxX < 0)
                ? new Bounds(0, 0, 0, 0)
                : new Bounds(minX, minY, maxX - minX + 1, maxY - minY + 1);
        BOUNDS_CACHE.put(key, found);
        return found;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * Soft contact shadow under a fighter. A radial falloff reads as light being blocked
     * by the body, rather than as a solid platform drawn underneath it.
     */
    public static void shadow(Graphics2D g2, float x, float groundY, float width, float strength) {
        if (width <= 1f || strength <= 0f) {
            return;
        }
        Graphics2D g = (Graphics2D) g2.create();
        float rx = Math.max(10f, width * 0.5f);
        float ry = Math.max(4f, rx * 0.24f);
        g.translate(x, groundY);
        g.scale(1.0, ry / rx);
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(0, 0), rx,
                new float[]{0f, 0.55f, 1f},
                new Color[]{
                        new Color(0, 0, 0, (int) (168 * strength)),
                        new Color(0, 0, 0, (int) (86 * strength)),
                        new Color(0, 0, 0, 0)});
        g.setPaint(paint);
        g.fill(new Ellipse2D.Float(-rx, -rx, rx * 2, rx * 2));
        g.dispose();
    }

    /** Draws a figure standing on {@code groundY}, scaled so its artwork is {@code height} tall. */
    public static void draw(Graphics2D g2, BufferedImage image, float x, float groundY,
                            float height, boolean facingRight, Pose pose) {
        Bounds b = bounds(image);
        if (b.isEmpty() || height <= 1f) {
            return;
        }
        Graphics2D g = (Graphics2D) g2.create();
        if (pose.alpha < 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, pose.alpha)));
        }

        float scale = height / b.height();
        AffineTransform body = new AffineTransform();
        body.translate(x + pose.sway * height, groundY + pose.lift * height);
        body.scale(facingRight ? 1 : -1, 1);
        body.rotate(Math.toRadians(pose.lean));
        body.scale(scale, scale * pose.squash);
        // Bottom-center of the visible artwork becomes the origin: feet on the ground.
        body.translate(-(b.x() + b.width() / 2.0), -(b.y() + b.height()));

        boolean splitLegs = Math.abs(pose.stride) > 0.05f;
        if (!splitLegs) {
            g.drawImage(image, body, null);
            g.dispose();
            return;
        }

        float hipY = groundY + pose.lift * height - height * 0.46f;
        Shape legs = new Rectangle2D.Float(x + pose.sway * height - height * 3f, hipY, height * 6f, height * 2f);
        Shape torso = new Rectangle2D.Float(x - height * 3f, hipY - height * 3f, height * 6f, height * 3f);

        Graphics2D lower = (Graphics2D) g.create();
        lower.clip(legs);
        lower.rotate(Math.toRadians(pose.stride), x + pose.sway * height, hipY);
        lower.drawImage(image, body, null);
        lower.dispose();

        Graphics2D upper = (Graphics2D) g.create();
        upper.clip(torso);
        if (Math.abs(pose.breathe) > 0.01f) {
            upper.translate(0, hipY);
            upper.scale(1f, 1f + pose.breathe * 0.012f);
            upper.translate(0, -hipY);
        }
        upper.drawImage(image, body, null);
        upper.dispose();
        g.dispose();
    }

    /**
     * Draws the artwork with its feet at the origin of the current transform. Used by
     * renderers that paint their own local frame (bosses with orbiting effects).
     */
    public static void drawAtOrigin(Graphics2D g2, BufferedImage image, float height) {
        Bounds b = bounds(image);
        if (b.isEmpty()) {
            return;
        }
        float scale = height / b.height();
        Graphics2D g = (Graphics2D) g2.create();
        g.scale(scale, scale);
        g.drawImage(image, -(b.x() + b.width() / 2), -(b.y() + b.height()), null);
        g.dispose();
    }

    /** Red impact flare over a fighter that just took a hit. */
    public static void hurtFlash(Graphics2D g2, float x, float groundY, float height, float alpha) {
        if (alpha <= 0.02f) {
            return;
        }
        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
        float rx = height * 0.24f;
        float ry = height * 0.40f;
        RadialGradientPaint flare = new RadialGradientPaint(
                new Point2D.Float(x, groundY - ry), Math.max(rx, ry),
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(255, 90, 90, 170), new Color(255, 60, 60, 90), new Color(255, 40, 40, 0)});
        g.setPaint(flare);
        g.fill(new Ellipse2D.Float(x - ry, groundY - ry * 2f, ry * 2f, ry * 2f));
        g.dispose();
    }

    /** Motion blur streak behind a dashing fighter. */
    public static void dashTrail(Graphics2D g2, BufferedImage image, float x, float groundY,
                                 float height, boolean facingRight, Pose pose) {
        int direction = facingRight ? -1 : 1;
        for (int i = 1; i <= 2; i++) {
            Pose ghost = pose.copy();
            ghost.alpha = 0.16f - i * 0.05f;
            draw(g2, image, x + direction * height * 0.06f * i, groundY, height, facingRight, ghost);
        }
    }

    /** Radiant rim light or arcane flare following the figure silhouette. */
    public static void rimLight(Graphics2D g2, BufferedImage image, float x, float groundY,
                                float height, boolean facingRight, Pose pose, Color color, float strength) {
        Bounds b = bounds(image);
        if (b.isEmpty()) {
            return;
        }
        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, strength)));
        float rx = height * (b.width() / (float) b.height()) * 0.55f;
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(x + pose.sway * height, groundY - height * 0.55f), rx,
                new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(color.getRed(), color.getGreen(), color.getBlue(), 90),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 30),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)});
        g.setPaint(glow);
        g.fill(new Ellipse2D.Float(x - rx, groundY - height * 0.55f - rx * 0.9f, rx * 2, rx * 1.8f));
        g.dispose();
    }

    // ------------------------------------------------------------------
    // Pose library: one description of how each state moves
    // ------------------------------------------------------------------

    public static Pose poseFor(AnimationState state, float progress, float idleTime) {
        Pose pose = new Pose();
        switch (state) {
            case RUNNING -> {
                double phase = idleTime * 9.0;
                pose.stride = (float) Math.sin(phase) * 12.0f;
                pose.lift = -(float) Math.abs(Math.sin(phase)) * 0.022f;
                pose.sway = (float) Math.sin(phase) * 0.008f;
                pose.lean = 7.0f;
                pose.breathe = (float) Math.sin(phase) * 0.5f;
            }
            case JUMPING -> {
                pose.lift = -0.16f + (float) Math.sin(progress * Math.PI) * -0.02f;
                pose.stride = 16.0f;
                pose.lean = 6.0f;
                pose.squash = 1.02f;
            }
            case DASHING -> {
                pose.lift = -0.035f;
                pose.lean = 15.0f;
                pose.stride = 18.0f;
                pose.sway = 0.012f;
            }
            case ATTACKING, CASTING -> {
                float strike = (float) Math.sin(progress * Math.PI);
                float windup = (float) Math.sin(progress * Math.PI * 2.0);
                pose.sway = (state == AnimationState.CASTING ? 0.035f : 0.075f) * strike;
                pose.lean = -7.0f * windup + 10.0f * strike;
                pose.stride = -6.0f * strike;
                pose.lift = -0.012f * strike;
                pose.breathe = strike * 0.8f;
            }
            case DEFENDING -> {
                pose.lean = -4.0f;
                pose.stride = 7.0f;
                pose.squash = 0.985f;
                pose.lift = 0.004f;
            }
            case HURT -> {
                float recoil = (float) Math.sin(progress * Math.PI);
                pose.sway = -0.05f * recoil;
                pose.lean = -11.0f * recoil;
                pose.lift = -0.012f * recoil;
                pose.stride = 5.0f * recoil;
            }
            case VICTORY -> {
                pose.lift = -0.012f - (float) Math.abs(Math.sin(idleTime * 2.4)) * 0.012f;
                pose.lean = -3.0f;
                pose.breathe = (float) Math.sin(idleTime * 2.4) * 0.6f;
            }
            case DEFEAT -> {
                pose.lean = -62.0f;
                pose.lift = 0.05f;
                pose.squash = 0.94f;
            }
            default -> {
                pose.lift = (float) Math.sin(idleTime * 2.0) * 0.006f;
                pose.lean = (float) Math.sin(idleTime * 1.3) * 0.7f;
                pose.sway = (float) Math.sin(idleTime * 1.1) * 0.004f;
                pose.breathe = (float) Math.sin(idleTime * 2.0);
            }
        }
        return pose;
    }
}
