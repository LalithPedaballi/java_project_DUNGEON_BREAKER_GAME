package com.riftarena.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Central styling for the whole game.
 *
 * Every colour, font and reusable painting routine lives here, so the entire
 * look of Dungeon Clash can be adjusted from a single file.
 *
 * Syllabus concepts demonstrated here:
 *   - Unit II:  classes, static methods, static final constants, access control
 *   - Unit III: java.awt / javax.swing utility classes
 *   - Unit IV:  inheritance (RiftButton extends JButton) and method overriding
 *              (paintComponent, getPreferredSize)
 */
public final class Theme {

    // ------------------------------------------------------------------
    // Palette — dark fantasy: near-black grounds, violet magic, old gold
    // ------------------------------------------------------------------
    public static final Color BACKGROUND      = new Color(0x0D, 0x0B, 0x12);
    public static final Color BACKGROUND_SOFT = new Color(0x1A, 0x14, 0x24);
    public static final Color PANEL           = new Color(0x1C, 0x17, 0x2A);
    public static final Color PANEL_HOVER     = new Color(0x2A, 0x22, 0x40);
    public static final Color PANEL_DEEP      = new Color(0x12, 0x0E, 0x1B);

    public static final Color VIOLET      = new Color(0x7B, 0x5F, 0xD6);
    public static final Color VIOLET_DIM  = new Color(0x4A, 0x3A, 0x7A);
    public static final Color GOLD        = new Color(0xC9, 0xA2, 0x27);
    public static final Color GOLD_BRIGHT = new Color(0xE8, 0xC5, 0x4A);
    public static final Color GOLD_DIM    = new Color(0x6E, 0x59, 0x1C);

    public static final Color TEXT     = new Color(0xE8, 0xDC, 0xC5);
    public static final Color TEXT_DIM = new Color(0x9A, 0x8F, 0xB0);
    public static final Color DANGER   = new Color(0xB0, 0x3A, 0x3A);
    public static final Color SHADOW   = new Color(0, 0, 0, 170);

    // Hero accent colours — one per selectable champion
    public static final Color HERO_WARRIOR  = new Color(0x4E, 0x7C, 0xB5);
    public static final Color HERO_MAGE     = new Color(0x8A, 0x63, 0xD2);
    public static final Color HERO_ASSASSIN = new Color(0x3F, 0xA0, 0x6B);

    // ------------------------------------------------------------------
    // Typography — only font families guaranteed to exist in every JVM
    // ------------------------------------------------------------------
    public static final Font TITLE_FONT    = new Font("Serif", Font.BOLD, 58);
    public static final Font SUBTITLE_FONT = new Font("Serif", Font.ITALIC, 21);
    public static final Font HEADING_FONT  = new Font("Serif", Font.BOLD, 32);
    public static final Font CARD_FONT     = new Font("Serif", Font.BOLD, 25);
    public static final Font BODY_FONT     = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font SMALL_FONT    = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BUTTON_FONT   = new Font("SansSerif", Font.BOLD, 17);

    /** Crest symbols painted on hero cards and in the arena. */
    public enum Crest { SWORD, STAFF, DAGGER, RIFT_SIGIL }

    /** Utility class: never instantiated, never subclassed. */
    private Theme() {
    }

    // ==================================================================
    // Construction helpers
    // ==================================================================

    /** Creates a standard opaque game panel using the theme background. */
    public static javax.swing.JPanel panel() {
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setOpaque(true);
        panel.setBackground(BACKGROUND);
        return panel;
    }

    /** Creates a themed, custom-painted button with an accent colour. */
    public static RiftButton button(String text, Color accent) {
        return new RiftButton(text, accent);
    }

    // ==================================================================
    // Java2D painting helpers (used by every screen)
    // ==================================================================

    /**
     * Paints the standard screen background: a vertical charcoal-to-violet
     * gradient softened by a radial vignette that darkens the edges.
     */
    public static void paintBackground(Graphics2D g2, int width, int height) {
        Paint oldPaint = g2.getPaint();
        GradientPaint ground = new GradientPaint(
                0, 0, BACKGROUND,
                0, height, BACKGROUND_SOFT);
        g2.setPaint(ground);
        g2.fillRect(0, 0, width, height);

        float radius = Math.max(width, height) * 0.72f;
        RadialGradientPaint vignette = new RadialGradientPaint(
                width / 2f, height / 2f, radius,
                new float[]{0.55f, 1.0f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 110)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, width, height);
        g2.setPaint(oldPaint);
    }

    /** Draws a decorative arcane circle: rings, triangles, ticks and gems. */
    public static void drawRuneCircle(Graphics2D g2, int centerX, int centerY, int radius, Color color) {
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(color);
        g2.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0));

        int inner = (int) (radius * 0.78);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{9f, 9f}, 0f));
        g2.draw(new Ellipse2D.Double(centerX - inner, centerY - inner, inner * 2.0, inner * 2.0));
        g2.setStroke(oldStroke);

        drawTriangle(g2, centerX, centerY, (int) (radius * 0.74), -Math.PI / 2);
        drawTriangle(g2, centerX, centerY, (int) (radius * 0.74), Math.PI / 2);

        int tick = (int) (radius * 0.92);
        for (int i = 0; i < 12; i++) {
            double angle = i * (Math.PI * 2 / 12);
            int x1 = centerX + (int) (Math.cos(angle) * inner);
            int y1 = centerY + (int) (Math.sin(angle) * inner);
            int x2 = centerX + (int) (Math.cos(angle) * tick);
            int y2 = centerY + (int) (Math.sin(angle) * tick);
            g2.drawLine(x1, y1, x2, y2);
        }

        for (int i = 0; i < 4; i++) {
            double angle = i * (Math.PI / 2) + Math.PI / 4;
            int gx = centerX + (int) (Math.cos(angle) * radius);
            int gy = centerY + (int) (Math.sin(angle) * radius);
            int s = Math.max(3, radius / 14);
            java.awt.geom.Path2D.Double gem = new java.awt.geom.Path2D.Double();
            gem.moveTo(gx, gy - s);
            gem.lineTo(gx + s, gy);
            gem.lineTo(gx, gy + s);
            gem.lineTo(gx - s, gy);
            gem.closePath();
            g2.fill(gem);
        }
        g2.setPaint(oldPaint);
    }

    /** Paints the thin double golden frame drawn around every screen. */
    public static void drawFrame(Graphics2D g2, int width, int height) {
        Paint oldPaint = g2.getPaint();
        g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 120));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Double(10, 10, width - 20, height - 20, 8, 8));
        g2.setColor(new Color(GOLD_DIM.getRed(), GOLD_DIM.getGreen(), GOLD_DIM.getBlue(), 170));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(17, 17, width - 34, height - 34, 6, 6));
        g2.setStroke(new BasicStroke(1f));
        g2.setPaint(oldPaint);
    }

    /** Draws horizontally centred text with a soft dark drop shadow. */
    public static void drawCenteredText(Graphics2D g2, String text, Font font, Color color,
                                        int centerX, int y) {
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics(font);
        int x = centerX - metrics.stringWidth(text) / 2;
        g2.setColor(SHADOW);
        g2.drawString(text, x + 2, y + 2);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    /** Draws horizontally centred text filled with a vertical colour gradient. */
    public static void drawCenteredGradientText(Graphics2D g2, String text, Font font,
                                                int centerX, int y, Color top, Color bottom) {
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics(font);
        int x = centerX - metrics.stringWidth(text) / 2;
        g2.setColor(SHADOW);
        g2.drawString(text, x + 3, y + 3);
        Paint oldPaint = g2.getPaint();
        g2.setPaint(new GradientPaint(x, y - metrics.getAscent(), top,
                x, y + metrics.getDescent(), bottom));
        g2.drawString(text, x, y);
        g2.setPaint(oldPaint);
    }

    /**
     * Paints a hero crest inside a circular arcane medallion.
     * Used on the hero cards and by both fighters in the arena.
     */
    public static void paintCrest(Graphics2D g2, Crest crest, int centerX, int centerY,
                                  int size, Color accent) {
        int radius = size / 2;

        // Medallion backdrop: filled disc, glowing ring, notch marks
        g2.setColor(new Color(PANEL_DEEP.getRed(), PANEL_DEEP.getGreen(), PANEL_DEEP.getBlue(), 230));
        g2.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, size, size));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, size, size));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
        g2.setStroke(new BasicStroke(1f));
        int inset = Math.max(5, size / 12);
        g2.draw(new Ellipse2D.Double(centerX - radius + inset, centerY - radius + inset,
                size - inset * 2.0, size - inset * 2.0));

        switch (crest) {
            case SWORD -> paintSwordCrest(g2, centerX, centerY, size, accent);
            case STAFF -> paintStaffCrest(g2, centerX, centerY, size, accent);
            case DAGGER -> paintDaggerCrest(g2, centerX, centerY, size, accent);
            case RIFT_SIGIL -> paintRiftSigil(g2, centerX, centerY, size, accent);
        }
    }

    // ==================================================================
    // Crest symbols
    // ==================================================================

    private static void paintSwordCrest(Graphics2D g2, int cx, int cy, int size, Color accent) {
        float s = size / 100f;
        // Shield behind the blade
        java.awt.geom.Path2D.Double shield = new java.awt.geom.Path2D.Double();
        shield.moveTo(cx - 26 * s, cy - 30 * s);
        shield.lineTo(cx + 26 * s, cy - 30 * s);
        shield.lineTo(cx + 26 * s, cy + 8 * s);
        shield.curveTo(cx + 26 * s, cy + 30 * s, cx, cy + 42 * s, cx, cy + 42 * s);
        shield.curveTo(cx, cy + 42 * s, cx - 26 * s, cy + 30 * s, cx - 26 * s, cy + 8 * s);
        shield.closePath();
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
        g2.fill(shield);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(shield);
        // Blade
        java.awt.Polygon blade = new java.awt.Polygon();
        blade.addPoint(cx, cy - 38 * (int) s);
        blade.addPoint(cx + 7 * (int) s, cy - 22 * (int) s);
        blade.addPoint(cx + 7 * (int) s, cy + 8 * (int) s);
        blade.addPoint(cx - 7 * (int) s, cy + 8 * (int) s);
        blade.addPoint(cx - 7 * (int) s, cy - 22 * (int) s);
        g2.setColor(new Color(0xD8, 0xD8, 0xE0));
        g2.fill(blade);
        g2.setColor(new Color(0x60, 0x60, 0x70));
        g2.draw(blade);
        // Crossguard, grip, pommel
        g2.setColor(GOLD);
        g2.fillRect(cx - 20 * (int) s, cy + 8 * (int) s, 40 * (int) s, 6 * (int) s);
        g2.setColor(new Color(0x4A, 0x3A, 0x2A));
        g2.fillRect(cx - 3 * (int) s, cy + 14 * (int) s, 6 * (int) s, 14 * (int) s);
        g2.setColor(GOLD_BRIGHT);
        g2.fill(new Ellipse2D.Double(cx - 6 * s, cy + 27 * s, 12 * s, 12 * s));
    }

    private static void paintStaffCrest(Graphics2D g2, int cx, int cy, int size, Color accent) {
        float s = size / 100f;
        g2.setStroke(new BasicStroke(5f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0x6A, 0x53, 0x3A));
        g2.drawLine((int) (cx - 20 * s), (int) (cy + 38 * s), (int) (cx + 8 * s), (int) (cy - 18 * s));
        // Glowing orb
        int orbX = (int) (cx + 10 * s), orbY = (int) (cy - 26 * s);
        int orbR = (int) (16 * s);
        RadialGradientPaint glow = new RadialGradientPaint(orbX, orbY, orbR * 2.2f,
                new float[]{0.1f, 1.0f},
                new Color[]{accent, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)});
        Paint old = g2.getPaint();
        g2.setPaint(glow);
        g2.fill(new Ellipse2D.Double(orbX - orbR * 2.2f, orbY - orbR * 2.2f, orbR * 4.4f, orbR * 4.4f));
        g2.setPaint(new GradientPaint(orbX - orbR, orbY - orbR, Color.WHITE, orbX + orbR, orbY + orbR, accent));
        g2.fill(new Ellipse2D.Double(orbX - orbR * 0.72f, orbY - orbR * 0.72f, orbR * 1.44f, orbR * 1.44f));
        g2.setPaint(old);
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
        g2.draw(new Ellipse2D.Double(orbX - orbR, orbY - orbR, orbR * 2.0, orbR * 2.0));
        // Small runic rings on the shaft
        g2.setColor(GOLD_DIM);
        g2.setStroke(new BasicStroke(2.4f * s));
        g2.drawLine((int) (cx - 8 * s), (int) (cy + 12 * s), (int) (cx - 2 * s), (int) (cy + 2 * s));
    }

    private static void paintDaggerCrest(Graphics2D g2, int cx, int cy, int size, Color accent) {
        float s = size / 100f;
        // Crescent slash behind the blade
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
        g2.setStroke(new BasicStroke(6f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - 34 * (int) s, cy - 34 * (int) s, 68 * (int) s, 68 * (int) s, -60, 120);
        // Downward blade
        java.awt.Polygon blade = new java.awt.Polygon();
        blade.addPoint(cx, cy + 40 * (int) s);
        blade.addPoint(cx + 8 * (int) s, cy - 6 * (int) s);
        blade.addPoint(cx + 5 * (int) s, cy - 20 * (int) s);
        blade.addPoint(cx - 5 * (int) s, cy - 20 * (int) s);
        blade.addPoint(cx - 8 * (int) s, cy - 6 * (int) s);
        g2.setColor(new Color(0xCF, 0xD4, 0xDC));
        g2.fill(blade);
        g2.setColor(new Color(0x4E, 0x55, 0x60));
        g2.draw(blade);
        // Guard and grip above
        g2.setColor(GOLD);
        g2.fillRect(cx - 16 * (int) s, cy - 24 * (int) s, 32 * (int) s, 5 * (int) s);
        g2.setColor(new Color(0x33, 0x2A, 0x22));
        g2.fillRect(cx - 3 * (int) s, cy - 38 * (int) s, 6 * (int) s, 14 * (int) s);
    }

    private static void paintRiftSigil(Graphics2D g2, int cx, int cy, int size, Color accent) {
        float s = size / 100f;
        // Broken outer ring
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 170));
        g2.setStroke(new BasicStroke(3f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - 36 * (int) s, cy - 36 * (int) s, 72 * (int) s, 72 * (int) s, 15, 130);
        g2.drawArc(cx - 36 * (int) s, cy - 36 * (int) s, 72 * (int) s, 72 * (int) s, 195, 130);
        // Jagged vertical rift
        java.awt.geom.Path2D.Double crack = new java.awt.geom.Path2D.Double();
        crack.moveTo(cx - 4 * s, cy - 34 * s);
        crack.lineTo(cx + 9 * s, cy - 10 * s);
        crack.lineTo(cx - 7 * s, cy + 4 * s);
        crack.lineTo(cx + 6 * s, cy + 18 * s);
        crack.lineTo(cx - 2 * s, cy + 36 * s);
        g2.setColor(new Color(0xE4, 0xD8, 0xFF));
        g2.setStroke(new BasicStroke(4f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(crack);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.setStroke(new BasicStroke(9f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(crack);
        // Floating shards
        g2.setColor(accent);
        int shard = Math.max(3, (int) (5 * s));
        g2.fill(new Ellipse2D.Double(cx - 26 * s, cy - 20 * s, shard, shard));
        g2.fill(new Ellipse2D.Double(cx + 22 * s, cy + 12 * s, shard, shard));
        g2.fill(new Ellipse2D.Double(cx + 14 * s, cy - 30 * s, shard, shard));
    }

    private static void drawTriangle(Graphics2D g2, int cx, int cy, int radius, double startAngle) {
        java.awt.Polygon triangle = new java.awt.Polygon();
        for (int i = 0; i < 3; i++) {
            double angle = startAngle + i * (Math.PI * 2 / 3);
            triangle.addPoint(cx + (int) (Math.cos(angle) * radius),
                    cy + (int) (Math.sin(angle) * radius));
        }
        g2.draw(triangle);
    }

    // ==================================================================
    // RiftButton — a custom-painted button with hover feedback
    // ==================================================================

    /**
     * A JButton drawn entirely with Java2D: rounded dark body, coloured
     * accent border, drop-shadow label, and a brighter glow on hover.
     */
    public static final class RiftButton extends JButton {

        private final Color accent;
        private boolean hovered;

        public RiftButton(String text, Color accent) {
            super(text);
            this.accent = accent;
            setFont(BUTTON_FONT);
            setForeground(TEXT);
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setMargin(new Insets(0, 0, 0, 0));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            MouseAdapter hoverWatcher = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            };
            addMouseListener(hoverWatcher);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = 16;
            boolean active = isEnabled();

            // Body: vertical gradient, warmer when hovered
            Color bodyTop = !active ? PANEL_DEEP : (hovered ? PANEL_HOVER : PANEL);
            Color bodyBottom = !active ? BACKGROUND : BACKGROUND_SOFT;
            g2.setPaint(new GradientPaint(0, 0, bodyTop, 0, height, bodyBottom));
            g2.fill(new RoundRectangle2D.Double(2, 2, width - 4, height - 4, arc, arc));

            // Accent border (brighter with a faint outer glow on hover)
            if (hovered && active) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.setStroke(new BasicStroke(5f));
                g2.draw(new RoundRectangle2D.Double(3, 3, width - 6, height - 6, arc, arc));
            }
            g2.setColor(!active ? new Color(0x3A, 0x34, 0x4A)
                    : (hovered ? accent.brighter() : accent));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Double(2.5, 2.5, width - 5, height - 5, arc, arc));

            // Inner top highlight for a subtle metallic edge
            g2.setColor(new Color(255, 255, 255, hovered ? 34 : 18));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(arc / 2 + 4, 5, width - arc / 2 - 4, 5);

            // Label with drop shadow
            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics(getFont());
            String text = getText();
            int textX = (width - metrics.stringWidth(text)) / 2;
            int textY = (height + metrics.getAscent() - metrics.getDescent()) / 2;
            g2.setColor(SHADOW);
            g2.drawString(text, textX + 1, textY + 1);
            g2.setColor(!active ? TEXT_DIM : (hovered ? GOLD_BRIGHT : TEXT));
            g2.drawString(text, textX, textY);

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.FontMetrics metrics = getFontMetrics(getFont());
            int width = metrics.stringWidth(getText()) + 64;
            return new Dimension(Math.max(width, 160), 52);
        }
    }
}
