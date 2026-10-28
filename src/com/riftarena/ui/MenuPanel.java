package com.riftarena.ui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cinematic Dark-Fantasy Main Menu for DUNGEON BREAKER.
 *
 * Recreates the dark-fantasy title screen based directly on the high-resolution artwork:
 *   - Six imposing foreground champions (Aegis, Nyx, Zephyra, Kael, Vexara, Draven)
 *   - Ruined gothic fortress towers, central eclipse portal, and wet cobblestone ground
 *   - Edge-to-edge canvas integration: fills the window seamlessly without visible borders,
 *     black letterbox bars, awkward cropping, or hero distortion
 *   - Interactive menu options (ENTER DUNGEON, BOSS CHALLENGE, HEROES, CONTROLS, QUIT GAME)
 *     with transparent dark glass surfaces, luminous gold/purple borders, and hover blooms
 *   - Subtle live Java2D effects: slowly pulsing rift glow, drifting ground mist ribbons,
 *     floating cosmic embers, staff crystal flares, and rapier blade glints
 *   - Keyboard (Arrow keys/WASD, Enter, Escape) and mouse navigation
 */
public class MenuPanel extends JPanel {

    public static final int BASE_WIDTH = 1024;
    public static final int BASE_HEIGHT = 575;

    public enum MenuOption {
        ENTER_DUNGEON("ENTER DUNGEON", 351, 372, 321, 44),
        BOSS_CHALLENGE("BOSS CHALLENGE", 357, 423, 309, 38),
        HEROES("HEROES", 361, 467, 144, 38),
        CONTROLS("CONTROLS", 519, 467, 144, 38),
        QUIT_GAME("QUIT GAME", 436, 514, 152, 33);

        public final String label;
        public final int baseLeft;
        public final int baseTop;
        public final int baseWidth;
        public final int baseHeight;

        MenuOption(String label, int left, int top, int width, int height) {
            this.label = label;
            this.baseLeft = left;
            this.baseTop = top;
            this.baseWidth = width;
            this.baseHeight = height;
        }
    }

    private static final class ButtonState {
        final MenuOption option;
        float hoverProgress = 0.0f;
        boolean hovered = false;
        boolean pressed = false;
        float clickFlash = 0.0f;

        ButtonState(MenuOption option) {
            this.option = option;
        }
    }

    private static final class EmberParticle {
        float normX;
        float normY;
        float size;
        float speedY;
        float driftFreq;
        float driftAmp;
        float phase;
        boolean isGold;
    }

    private static final class FogRibbon {
        float yNorm;
        float heightNorm;
        float speed;
        float waveFreq;
        float waveAmp;
        Color color;
        float phase;

        FogRibbon(float yNorm, float heightNorm, float speed, float waveFreq, float waveAmp, Color color, float phase) {
            this.yNorm = yNorm;
            this.heightNorm = heightNorm;
            this.speed = speed;
            this.waveFreq = waveFreq;
            this.waveAmp = waveAmp;
            this.color = color;
            this.phase = phase;
        }
    }

    private final MainFrame frame;
    private final BufferedImage backgroundImage;
    private final ButtonState[] buttons;
    private final List<EmberParticle> embers = new ArrayList<>();
    private final List<FogRibbon> fogRibbons = new ArrayList<>();

    private int focusedIndex = 0;
    private float idleTime = 0.0f;
    private final Timer animTimer;

    public MenuPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(null);
        setBackground(new Color(0x0E, 0x0A, 0x18));
        setFocusable(true);

        this.backgroundImage = loadMenuBackground();

        this.buttons = new ButtonState[MenuOption.values().length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new ButtonState(MenuOption.values()[i]);
        }

        initAtmosphericParticles();

        setupInputHandlers();

        animTimer = new Timer(16, e -> {
            idleTime += 0.016f;
            updateAnimationState();
            repaint();
        });

        if (!GraphicsEnvironment.isHeadless()) {
            animTimer.start();
        }
    }

    private static BufferedImage loadMenuBackground() {
        File[] searchPaths = new File[] {
                new File("assets/menu/main_menu_background.png"),
                new File("assets/menu/main_menu_background.jpg"),
                new File("../assets/menu/main_menu_background.png"),
                new File("main_menu_background.png")
        };

        for (File f : searchPaths) {
            if (f.exists() && f.isFile()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        return img;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        try (InputStream is = MenuPanel.class.getResourceAsStream("/assets/menu/main_menu_background.png")) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    return img;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void initAtmosphericParticles() {
        Random rand = new Random(1337);
        // 42 atmospheric embers (gold sparks & abyssal amethyst spores)
        for (int i = 0; i < 42; i++) {
            EmberParticle p = new EmberParticle();
            p.normX = rand.nextFloat();
            p.normY = rand.nextFloat();
            p.size = 1.2f + rand.nextFloat() * 2.4f;
            p.speedY = 0.030f + rand.nextFloat() * 0.075f;
            p.driftFreq = 0.8f + rand.nextFloat() * 1.5f;
            p.driftAmp = 0.010f + rand.nextFloat() * 0.024f;
            p.phase = rand.nextFloat() * (float) (Math.PI * 2);
            p.isGold = rand.nextFloat() > 0.42f;
            embers.add(p);
        }

        // Drifting low ground fog ribbons across wet cobblestone
        fogRibbons.add(new FogRibbon(0.83f, 0.10f, 0.028f, 0.0035f, 6.0f, new Color(0x38, 0x1C, 0x4E, 32), 0.0f));
        fogRibbons.add(new FogRibbon(0.89f, 0.09f, -0.020f, 0.0045f, 5.5f, new Color(0x26, 0x16, 0x3C, 38), 1.8f));
        fogRibbons.add(new FogRibbon(0.94f, 0.08f, 0.034f, 0.0030f, 7.0f, new Color(0x45, 0x22, 0x62, 28), 3.6f));
    }

    private void setupInputHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int idx = getButtonAt(e.getX(), e.getY());
                if (idx != -1) {
                    buttons[idx].pressed = true;
                    focusedIndex = idx;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int idx = getButtonAt(e.getX(), e.getY());
                for (int i = 0; i < buttons.length; i++) {
                    if (buttons[i].pressed) {
                        buttons[i].pressed = false;
                        if (i == idx) {
                            buttons[i].clickFlash = 1.0f;
                            activateOption(buttons[i].option);
                        }
                    }
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                for (ButtonState b : buttons) {
                    b.hovered = false;
                    b.pressed = false;
                }
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = getButtonAt(e.getX(), e.getY());
                boolean anyHover = false;
                for (int i = 0; i < buttons.length; i++) {
                    boolean isHover = (i == idx);
                    buttons[i].hovered = isHover;
                    if (isHover) {
                        focusedIndex = i;
                        anyHover = true;
                    }
                }
                setCursor(anyHover ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                switch (code) {
                    case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                        navigateUp();
                        repaint();
                    }
                    case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                        navigateDown();
                        repaint();
                    }
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
                        navigateLeftRight(false);
                        repaint();
                    }
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                        navigateLeftRight(true);
                        repaint();
                    }
                    case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                        if (focusedIndex >= 0 && focusedIndex < buttons.length) {
                            buttons[focusedIndex].clickFlash = 1.0f;
                            activateOption(buttons[focusedIndex].option);
                            repaint();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        activateOption(MenuOption.QUIT_GAME);
                    }
                }
            }
        });
    }

    private void navigateDown() {
        if (focusedIndex == 0) {
            focusedIndex = 1;
        } else if (focusedIndex == 1) {
            focusedIndex = 2;
        } else if (focusedIndex == 2 || focusedIndex == 3) {
            focusedIndex = 4;
        } else {
            focusedIndex = 0;
        }
    }

    private void navigateUp() {
        if (focusedIndex == 0) {
            focusedIndex = 4;
        } else if (focusedIndex == 1) {
            focusedIndex = 0;
        } else if (focusedIndex == 2 || focusedIndex == 3) {
            focusedIndex = 1;
        } else {
            focusedIndex = 2;
        }
    }

    private void navigateLeftRight(boolean right) {
        if (focusedIndex == 2) {
            focusedIndex = 3;
        } else if (focusedIndex == 3) {
            focusedIndex = 2;
        }
    }

    private void updateAnimationState() {
        // Smooth button hover transitions
        for (int i = 0; i < buttons.length; i++) {
            ButtonState b = buttons[i];
            boolean active = b.hovered || (i == focusedIndex);
            float target = active ? 1.0f : 0.0f;
            b.hoverProgress += (target - b.hoverProgress) * 0.22f;
            if (b.clickFlash > 0.0f) {
                b.clickFlash = Math.max(0.0f, b.clickFlash - 0.08f);
            }
        }

        // Particle updates
        for (EmberParticle p : embers) {
            p.normY -= p.speedY * 0.016f;
            if (p.normY < -0.04f) {
                p.normY = 1.04f;
                p.normX = (float) Math.random();
            }
        }
    }

    public Rectangle getButtonBounds(MenuOption option) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            width = 1280;
            height = 800;
        }

        float aspect = (float) BASE_WIDTH / BASE_HEIGHT;
        float viewAspect = (float) width / height;

        float scale;
        int ox;
        int oy;

        if (viewAspect <= aspect) {
            // Screen is taller or equal (e.g. 1280x800, 1024x680): anchor horizontally
            scale = (float) width / BASE_WIDTH;
            int drawH = Math.round(BASE_HEIGHT * scale);
            ox = 0;
            oy = (height - drawH) / 2;
        } else {
            // Screen is wider (e.g. 21:9 ultrawide): anchor vertically
            scale = (float) height / BASE_HEIGHT;
            int drawW = Math.round(BASE_WIDTH * scale);
            ox = (width - drawW) / 2;
            oy = 0;
        }

        int bx = ox + Math.round(option.baseLeft * scale);
        int by = oy + Math.round(option.baseTop * scale);
        int bw = Math.round(option.baseWidth * scale);
        int bh = Math.round(option.baseHeight * scale);

        return new Rectangle(bx, by, bw, bh);
    }

    private int getButtonAt(int x, int y) {
        for (int i = 0; i < buttons.length; i++) {
            Rectangle rect = getButtonBounds(buttons[i].option);
            Rectangle hitBox = new Rectangle(rect.x - 4, rect.y - 4, rect.width + 8, rect.height + 8);
            if (hitBox.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public void activateOption(MenuOption option) {
        switch (option) {
            case ENTER_DUNGEON -> {
                if (frame != null) {
                    frame.setEncounterType(MainFrame.EncounterType.SKIRMISH);
                    frame.showScreen(MainFrame.Screen.HERO_SELECT);
                }
            }
            case BOSS_CHALLENGE -> {
                if (frame != null) {
                    frame.setEncounterType(MainFrame.EncounterType.BOSS);
                    frame.showScreen(MainFrame.Screen.HERO_SELECT);
                }
            }
            case HEROES -> {
                if (frame != null) {
                    frame.showScreen(MainFrame.Screen.HERO_SELECT);
                }
            }
            case CONTROLS -> {
                showControlsDialog(frame);
            }
            case QUIT_GAME -> {
                if (frame != null) {
                    frame.quitGame();
                } else {
                    System.exit(0);
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int width = getWidth();
        int height = getHeight();

        if (backgroundImage != null) {
            // Render edge-to-edge integrated artwork without black bars or hero distortion
            paintEdgeToEdgeBackground(g2, width, height);
            // Live atmospheric Java2D dynamic overlay layers
            paintCinematicOverlays(g2, width, height);
            // Interactive button overlays (translucent dark glass, glowing borders, hover blooms)
            paintInteractiveButtons(g2, width, height, false);
        } else {
            // Procedural fallback if image asset is unavailable
            paintProceduralFallback(g2, width, height);
            paintInteractiveButtons(g2, width, height, true);
        }

        g2.dispose();
    }

    private void paintEdgeToEdgeBackground(Graphics2D g2, int width, int height) {
        float aspect = (float) BASE_WIDTH / BASE_HEIGHT;
        float viewAspect = (float) width / height;

        float scale;
        int drawW;
        int drawH;
        int ox;
        int oy;

        if (viewAspect <= aspect) {
            // Screen is taller or equal (e.g. 1280x800, 1024x680): anchor horizontally
            scale = (float) width / BASE_WIDTH;
            drawW = width;
            drawH = Math.round(BASE_HEIGHT * scale);
            ox = 0;
            oy = (height - drawH) / 2;
        } else {
            // Screen is wider (e.g. 21:9 ultrawide): anchor vertically
            scale = (float) height / BASE_HEIGHT;
            drawH = height;
            drawW = Math.round(BASE_WIDTH * scale);
            ox = (width - drawW) / 2;
            oy = 0;
        }

        // 1. Draw atmospheric backdrop filling 100% of the canvas edge-to-edge
        GradientPaint backdrop = new GradientPaint(
                0, 0, new Color(0x1A, 0x12, 0x2C),
                0, height, new Color(0x12, 0x08, 0x14)
        );
        g2.setPaint(backdrop);
        g2.fillRect(0, 0, width, height);

        // 2. Seamless Sky Extension above main image (0 .. oy)
        if (oy > 0) {
            GradientPaint skyGrad = new GradientPaint(
                    0, 0, new Color(0x1E, 0x16, 0x32),
                    0, oy + 28, new Color(0x2C, 0x28, 0x48)
            );
            g2.setPaint(skyGrad);
            g2.fillRect(0, 0, width, oy + 28);

            // Distant twinkling stars in zenith sky
            g2.setColor(new Color(255, 255, 255, 60));
            for (int i = 0; i < 28; i++) {
                int sx = (int) ((i * 187.3f) % width);
                int sy = (int) ((i * 39.7f) % (oy + 12));
                int sz = (i % 6 == 0) ? 2 : 1;
                g2.fillRect(sx, sy, sz, sz);
            }
        }

        // 3. Draw high-resolution master artwork with bilinear filtering
        g2.drawImage(backgroundImage, ox, oy, drawW, drawH, null);

        // 4. Feathered Edge Blending: completely dissolves any boundary between artwork and extended canvas
        if (oy > 0) {
            // Top blend: smooth gradient dissolving top edge of artwork into the sky
            GradientPaint topBlend = new GradientPaint(
                    0, oy - 4, new Color(0x2C, 0x28, 0x48, 230),
                    0, oy + 24, new Color(0x2C, 0x28, 0x48, 0)
            );
            g2.setPaint(topBlend);
            g2.fillRect(0, oy - 4, width, 28);

            // Bottom extension & blend: smooth gradient continuing wet cobblestones to bottom of window
            int botY = oy + drawH;
            GradientPaint botBlend = new GradientPaint(
                    0, botY - 24, new Color(0x26, 0x14, 0x26, 0),
                    0, botY + 8, new Color(0x26, 0x14, 0x26, 235)
            );
            g2.setPaint(botBlend);
            g2.fillRect(0, botY - 24, width, 32);

            // Ground floor continuation to bottom window edge
            GradientPaint floorGrad = new GradientPaint(
                    0, botY, new Color(0x26, 0x14, 0x26),
                    0, height, new Color(0x10, 0x08, 0x12)
            );
            g2.setPaint(floorGrad);
            g2.fillRect(0, botY, width, height - botY);
        }

        if (ox > 0) {
            // Left & right pillar extension for ultrawide viewports
            GradientPaint leftBlend = new GradientPaint(
                    ox - 4, 0, new Color(0x1A, 0x12, 0x2C, 230),
                    ox + 24, 0, new Color(0x1A, 0x12, 0x2C, 0)
            );
            g2.setPaint(leftBlend);
            g2.fillRect(0, 0, ox + 24, height);

            GradientPaint rightBlend = new GradientPaint(
                    ox + drawW - 24, 0, new Color(0x1A, 0x12, 0x2C, 0),
                    ox + drawW + 4, 0, new Color(0x1A, 0x12, 0x2C, 230)
            );
            g2.setPaint(rightBlend);
            g2.fillRect(ox + drawW - 24, 0, width - (ox + drawW - 24), height);
        }
    }

    private void paintCinematicOverlays(Graphics2D g2, int width, int height) {
        float aspect = (float) BASE_WIDTH / BASE_HEIGHT;
        float viewAspect = (float) width / height;

        float scale;
        int drawW;
        int drawH;
        int ox;
        int oy;

        if (viewAspect <= aspect) {
            scale = (float) width / BASE_WIDTH;
            drawW = width;
            drawH = Math.round(BASE_HEIGHT * scale);
            ox = 0;
            oy = (height - drawH) / 2;
        } else {
            scale = (float) height / BASE_HEIGHT;
            drawH = height;
            drawW = Math.round(BASE_WIDTH * scale);
            ox = (width - drawW) / 2;
            oy = 0;
        }

        // 1. Slowly Pulsing Rift & Eclipse Glow
        float portalPulse = 0.5f + 0.5f * (float) Math.sin(idleTime * 1.5f);
        int portalX = ox + Math.round(535 * scale);
        int portalY = oy + Math.round(155 * scale);
        int portalRadius = Math.round(155 * scale);

        RadialGradientPaint portalGlow = new RadialGradientPaint(
                portalX, portalY, portalRadius,
                new float[]{0.0f, 0.45f, 0.85f, 1.0f},
                new Color[]{
                        new Color(185, 60, 255, (int) (48 + 34 * portalPulse)),
                        new Color(130, 30, 205, (int) (26 + 20 * portalPulse)),
                        new Color(80, 15, 145, (int) (10 + 10 * portalPulse)),
                        new Color(0, 0, 0, 0)
                }
        );
        g2.setPaint(portalGlow);
        g2.fillOval(portalX - portalRadius, portalY - portalRadius, portalRadius * 2, portalRadius * 2);

        // 2. Drifting Ground Mist Ribbons across wet cobblestones
        paintGroundFog(g2, width, height, ox, oy, drawW, drawH, scale);

        // 3. Floating Cosmic & Ash Embers drifting upward across the viewport
        paintFloatingEmbers(g2, width, height, ox, oy, drawW, drawH);

        // 4. Subtle Weapon & Crystal Light Glints
        paintGlints(g2, ox, oy, scale);
    }

    private void paintGroundFog(Graphics2D g2, int width, int height, int ox, int oy, int drawW, int drawH, float scale) {
        for (FogRibbon ribbon : fogRibbons) {
            float yPos = oy + ribbon.yNorm * drawH;
            float ribbonH = ribbon.heightNorm * drawH;
            float shift = (idleTime * ribbon.speed * width + ribbon.phase * 50) % width;
            if (shift < 0) shift += width;

            Path2D.Float path = new Path2D.Float();
            path.moveTo(0, yPos);

            int segments = 14;
            for (int s = 0; s <= segments; s++) {
                float sx = (s / (float) segments) * width;
                float wave = (float) Math.sin((sx + shift) * ribbon.waveFreq) * ribbon.waveAmp * scale;
                path.lineTo(sx, yPos + wave);
            }
            path.lineTo(width, yPos + ribbonH);
            path.lineTo(0, yPos + ribbonH);
            path.closePath();

            g2.setColor(ribbon.color);
            g2.fill(path);
        }
    }

    private void paintFloatingEmbers(Graphics2D g2, int width, int height, int ox, int oy, int drawW, int drawH) {
        for (EmberParticle p : embers) {
            float px = ((p.normX + (float) Math.sin(idleTime * p.driftFreq + p.phase) * p.driftAmp) % 1.0f) * width;
            float py = p.normY * height;

            if (px < 0 || px > width || py < 0 || py > height) {
                continue;
            }

            float flicker = 0.55f + 0.45f * (float) Math.sin(idleTime * 4.2f + p.phase);
            int alpha = (int) (185 * flicker);

            Color color = p.isGold
                    ? new Color(255, 215, 80, alpha)
                    : new Color(195, 125, 255, alpha);

            g2.setColor(color);
            int s = Math.max(1, Math.round(p.size));
            g2.fillRect(Math.round(px), Math.round(py), s, s);
        }
    }

    private void paintGlints(Graphics2D g2, int ox, int oy, float scale) {
        // Zephyra Staff Crystal Sparkle at base coordinates (385, 180)
        float staffPulse = (float) Math.max(0.0, Math.sin(idleTime * 1.85f) - 0.72) * 3.57f;
        if (staffPulse > 0.05f) {
            int sx = ox + Math.round(385 * scale);
            int sy = oy + Math.round(180 * scale);
            drawStarGlint(g2, sx, sy, 8.5f * scale * staffPulse, new Color(230, 185, 255, (int) (225 * staffPulse)));
        }

        // Kael Blood Blade Reflection at base coordinates (650, 515)
        float bladePulse = (float) Math.max(0.0, Math.sin(idleTime * 1.35f + 1.4f) - 0.78) * 4.54f;
        if (bladePulse > 0.05f) {
            int kx = ox + Math.round(650 * scale);
            int ky = oy + Math.round(515 * scale);
            drawStarGlint(g2, kx, ky, 7.5f * scale * bladePulse, new Color(255, 95, 95, (int) (215 * bladePulse)));
        }
    }

    private void drawStarGlint(Graphics2D g2, int x, int y, float radius, Color color) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(color);
        g.setStroke(new BasicStroke(1.2f));

        g.drawLine(x - (int) radius, y, x + (int) radius, y);
        g.drawLine(x, y - (int) radius, x, y + (int) radius);

        g.setColor(Color.WHITE);
        g.fillOval(x - 1, y - 1, 3, 3);
        g.dispose();
    }

    private void paintInteractiveButtons(Graphics2D g2, int width, int height, boolean fallbackMode) {
        for (int i = 0; i < buttons.length; i++) {
            ButtonState b = buttons[i];
            Rectangle r = getButtonBounds(b.option);
            boolean isFocused = (i == focusedIndex);
            float progress = b.hoverProgress;

            // Idle breathing aura for Primary Action (ENTER DUNGEON) when unhovered
            if (i == 0 && progress < 0.05f) {
                float idlePulse = 0.5f + 0.5f * (float) Math.sin(idleTime * 2.4f);
                g2.setColor(new Color(255, 215, 60, (int) (18 + 15 * idlePulse)));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g2.setColor(new Color(255, 220, 110, (int) (38 + 32 * idlePulse)));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            }

            if (fallbackMode) {
                // In fallback mode, render full gothic dark button containers with sharp text
                paintFallbackButtonContainer(g2, b.option, r, progress, isFocused);
            } else if (progress > 0.01f || isFocused) {
                // Over reference artwork: render transparent dark glass surface, glowing borders, and radiant bloom
                paintArtworkButtonOverlay(g2, b.option, r, progress, isFocused, b.pressed);
            }

            // Click ripple / flash feedback
            if (b.clickFlash > 0.01f) {
                g2.setColor(new Color(255, 255, 255, (int) (115 * b.clickFlash)));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            }
        }
    }

    private void paintArtworkButtonOverlay(Graphics2D g2, MenuOption option, Rectangle r, float progress, boolean isFocused, boolean pressed) {
        Graphics2D g = (Graphics2D) g2.create();

        Color baseGlow;
        Color borderHighlight;
        switch (option) {
            case ENTER_DUNGEON -> {
                baseGlow = new Color(255, 205, 50);
                borderHighlight = new Color(255, 235, 140);
            }
            case BOSS_CHALLENGE -> {
                baseGlow = new Color(175, 75, 255);
                borderHighlight = new Color(225, 180, 255);
            }
            case HEROES -> {
                baseGlow = new Color(145, 90, 250);
                borderHighlight = new Color(205, 180, 255);
            }
            case CONTROLS -> {
                baseGlow = new Color(120, 140, 255);
                borderHighlight = new Color(195, 210, 255);
            }
            case QUIT_GAME -> {
                baseGlow = new Color(245, 60, 60);
                borderHighlight = new Color(255, 160, 160);
            }
            default -> {
                baseGlow = new Color(200, 150, 255);
                borderHighlight = Color.WHITE;
            }
        }

        int arc = 8;
        if (pressed) {
            g.translate(0, 1);
        }

        // 1. Transparent Dark Glass Surface: deepens contrast under the button text
        int surfaceAlpha = (int) (95 * progress);
        g.setColor(new Color(0x0C, 0x06, 0x14, surfaceAlpha));
        g.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);

        // 2. Radial Energy Bloom
        int bloomAlpha = (int) (68 * progress);
        RadialGradientPaint bloom = new RadialGradientPaint(
                r.x + r.width / 2.0f, r.y + r.height / 2.0f, r.width * 0.55f,
                new float[]{0.0f, 0.65f, 1.0f},
                new Color[]{
                        new Color(baseGlow.getRed(), baseGlow.getGreen(), baseGlow.getBlue(), bloomAlpha),
                        new Color(baseGlow.getRed(), baseGlow.getGreen(), baseGlow.getBlue(), (int) (bloomAlpha * 0.38f)),
                        new Color(0, 0, 0, 0)
                }
        );
        g.setPaint(bloom);
        g.fillRoundRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8, arc + 4, arc + 4);

        // 3. Shimmering Gilded / Amethyst Border
        int borderAlpha = (int) (225 * progress);
        g.setColor(new Color(borderHighlight.getRed(), borderHighlight.getGreen(), borderHighlight.getBlue(), borderAlpha));
        g.setStroke(new BasicStroke(isFocused ? 2.0f : 1.5f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);

        // 4. Animated Traveling Light Gleam across the button
        float gleamPos = ((idleTime * 1.8f) % 2.5f) - 0.5f;
        if (gleamPos >= 0.0f && gleamPos <= 1.2f) {
            float gx = r.x + gleamPos * r.width;
            float startX = Math.max(r.x, gx - 20);
            float endX = Math.min(r.x + r.width, gx + 20);
            if (endX > startX) {
                LinearGradientPaint gleam = new LinearGradientPaint(
                        startX, r.y, endX, r.y,
                        new float[]{0.0f, 0.5f, 1.0f},
                        new Color[]{
                                new Color(255, 255, 255, 0),
                                new Color(255, 255, 255, (int) (65 * progress)),
                                new Color(255, 255, 255, 0)
                        }
                );
                g.setPaint(gleam);
                g.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
            }
        }

        // 5. Gothic Focus Diamonds when navigated via keyboard
        if (isFocused) {
            float pipPulse = 0.5f + 0.5f * (float) Math.sin(idleTime * 5.0f);
            g.setColor(new Color(borderHighlight.getRed(), borderHighlight.getGreen(), borderHighlight.getBlue(), (int) (200 + 55 * pipPulse)));
            drawDiamond(g, r.x - 7, r.y + r.height / 2, 4);
            drawDiamond(g, r.x + r.width + 7, r.y + r.height / 2, 4);
        }

        g.dispose();
    }

    private void drawDiamond(Graphics2D g, int cx, int cy, int size) {
        Path2D.Float d = new Path2D.Float();
        d.moveTo(cx, cy - size);
        d.lineTo(cx + size, cy);
        d.lineTo(cx, cy + size);
        d.lineTo(cx - size, cy);
        d.closePath();
        g.fill(d);
    }

    private void paintFallbackButtonContainer(Graphics2D g2, MenuOption option, Rectangle r, float progress, boolean isFocused) {
        Graphics2D g = (Graphics2D) g2.create();

        g.setColor(new Color(0x14, 0x0E, 0x1E, 230));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);

        Color borderCol = option == MenuOption.ENTER_DUNGEON
                ? Theme.GOLD_BRIGHT
                : (option == MenuOption.QUIT_GAME ? Theme.DANGER : Theme.HERO_MAGE);

        g.setColor(new Color(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), isFocused ? 240 : 160));
        g.setStroke(new BasicStroke(isFocused ? 2.0f : 1.2f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);

        g.setFont(new Font("Serif", Font.BOLD, Math.max(12, Math.round(r.height * 0.42f))));
        FontMetrics fm = g.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(option.label)) / 2;
        int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();

        g.setColor(Color.BLACK);
        g.drawString(option.label, tx + 1, ty + 1);

        g.setColor(isFocused ? Color.WHITE : borderCol);
        g.drawString(option.label, tx, ty);

        g.dispose();
    }

    private void paintProceduralFallback(Graphics2D g2, int width, int height) {
        int centreX = width / 2;

        g2.setColor(new Color(255, 255, 255, 75));
        for (int i = 0; i < 40; i++) {
            int sx = (int) ((i * 137.5f) % width);
            int sy = (int) ((i * 79.3f) % (height * 0.65f));
            int size = (i % 7 == 0) ? 2 : 1;
            g2.fillRect(sx, sy, size, size);
        }

        RadialGradientPaint nebula = new RadialGradientPaint(
                centreX, height * 0.38f, width * 0.55f,
                new float[]{0.0f, 0.40f, 0.80f, 1.0f},
                new Color[]{
                        new Color(0x5A, 0x1E, 0x82, 70),
                        new Color(0x32, 0x0F, 0x4D, 40),
                        new Color(0x15, 0x07, 0x24, 15),
                        new Color(0, 0, 0, 0)
                }
        );
        g2.setPaint(nebula);
        g2.fillRect(0, 0, width, height);

        int riftY = (int) (height * 0.38);
        int riftSize = (int) (Math.min(width, height) * 0.38f);
        RadialGradientPaint rift = new RadialGradientPaint(
                centreX, riftY, riftSize * 0.5f,
                new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                new Color[]{
                        new Color(240, 180, 255, 220),
                        new Color(170, 60, 240, 180),
                        new Color(90, 15, 150, 90),
                        new Color(0, 0, 0, 0)
                }
        );
        g2.setPaint(rift);
        g2.fillOval(centreX - riftSize / 2, riftY - riftSize / 2, riftSize, riftSize);

        g2.setFont(new Font("Serif", Font.BOLD, 46));
        g2.setColor(Theme.GOLD_BRIGHT);
        String title = "DUNGEON BREAKER";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, centreX - fm.stringWidth(title) / 2, (int) (height * 0.18f));

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(new Color(200, 180, 220));
        String sub = "BREAK THE SEAL. CONQUER THE ABYSS.";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(sub, centreX - fm2.stringWidth(sub) / 2, (int) (height * 0.23f));
    }

    private void showControlsDialog(MainFrame parent) {
        JDialog dialog = new JDialog(parent, "COMBAT CONTROLS & HOW TO PLAY", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Theme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("DUNGEON BREAKER \u2014 COMBAT GUIDE");
        title.setFont(Theme.HEADING_FONT);
        title.setForeground(Theme.GOLD_BRIGHT);
        title.setAlignmentX(0.0f);
        content.add(title);
        content.add(javax.swing.Box.createRigidArea(new Dimension(0, 16)));

        String guideText = """
                === DIRECT REAL-TIME COMBAT CONTROLS ===
                \u2022 [A / D] or [Left / Right Arrows] : Move Left / Right
                \u2022 [W / Space] or [Up Arrow]         : Jump (Evade ground shockwaves!)
                \u2022 [J] or [Left Mouse Click]         : Primary Attack (Cleave / Volley / Dual Daggers)
                \u2022 [K] or [Right Mouse Click]        : Class Special Ability (With cooldown timer)
                \u2022 [Shift]                           : Evasive Dash (Invulnerable i-frames through projectiles!)
                \u2022 [ESC]                             : Pause / Resume simulation
                \u2022 [R]                               : Instant Dungeon / Encounter Restart
                \u2022 [H] or [LOG button]               : Toggle In-Arena Combat Archive Drawer

                === SIX-CHAMPION ROSTER ===
                \u2022 Aegis   (Warrior - 120 HP)       : Bulwark shield & broadsword cleave. Special grants invulnerable guard.
                \u2022 Zephyra (Mage - 80 HP)           : Arcane volley. Special unleashes an explosive 360\u00b0 Arcane Nova.
                \u2022 Nyx     (Assassin - 95 HP)       : Swift shadow daggers. Special dashes forward dealing lethal execute damage.
                \u2022 Kael    (Crimson Blade - 105 HP) : Agile rapier combatant. Special deals high-velocity armor-piercing thrust.
                \u2022 Vexara  (Soul Reaper - 90 HP)    : Abyssal scythe sweeps. Special siphons health from all nearby targets.
                \u2022 Draven  (Magma Juggernaut - 140 HP): Colossal molten gauntlets. Special: Magma Eruption shockwave slam.

                === DUNGEON & BOSS PROGRESSION ===
                \u2022 Encounter 1: Void Patrol (Rift Thrall & agile Void Skirmisher). Defeat all to unlock next room.
                \u2022 Encounter 2: Shadow Guard with ranged Rift Cultist & Iron Clad Grunt. Dash through void bolts!
                \u2022 Boss Gate  : Ancient seal shatters before The Rift Warden (240 HP).
                \u2022 Cataclysm  : At 50% HP (120 HP), Boss triggers Phase II, unleashing ground shockwave slams.
                                Jump ([W] / [Space]) to evade the shockwave!
                """;

        JTextArea area = new JTextArea(guideText);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setForeground(Theme.TEXT);
        area.setBackground(Theme.PANEL_DEEP);
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(660, 360));
        scroll.setBorder(BorderFactory.createLineBorder(Theme.GOLD_DIM, 1));
        scroll.getViewport().setBackground(Theme.PANEL_DEEP);
        content.add(scroll);
        content.add(javax.swing.Box.createRigidArea(new Dimension(0, 16)));

        Theme.RiftButton closeBtn = Theme.button("CLOSE GUIDE", Theme.GOLD);
        closeBtn.setFocusable(false);
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        btnRow.add(closeBtn);
        content.add(btnRow);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
