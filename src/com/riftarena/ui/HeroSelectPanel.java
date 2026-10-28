package com.riftarena.ui;

import com.riftarena.ui.render.HeroSpriteLoader;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cinematic Six-Champion Fighting Game Selection Screen for DUNGEON BREAKER.
 *
 * Implements arcade fighting-game presentation matching the main menu's visual standard:
 *   - Edge-to-edge cinematic background reusing assets/menu/main_menu_background.png with
 *     a subtle dark-glass vignette wash for high-contrast UI legibility.
 *   - Imposing Left Showcase Stage featuring large, high-definition character artwork,
 *     directional backlighting aura, ground contact shadow, segmented jewel stat meters,
 *     lore description, and signature special move card.
 *   - Polished 2x3 Roster Grid displaying all six champions (Aegis, Nyx, Zephyra, Kael,
 *     Vexara, Draven) with crisp portraits, role badges, and glowing golden selection frames.
 *   - Premium Dark-Glass Bottom Control Bar (Main Menu, Encounter Mode Toggle, Enter the Rift).
 *   - Living atmospheric Java2D animations: pulsing rift glow, drifting mist, floating embers,
 *     and weapon light glints.
 *   - 4-way keyboard navigation (WASD, Arrows, Enter, Space, Escape) and mouse support.
 */
public class HeroSelectPanel extends JPanel {

    public static final String[] HERO_INTERNAL_NAMES = {"Warrior", "Assassin", "Mage", "Kael", "Vexara", "Draven"};
    public static final String[] HERO_DISPLAY_NAMES  = {"Aegis", "Nyx", "Zephyra", "Kael", "Vexara", "Draven"};
    public static final String[] HERO_ASSET_KEYS     = {"aegis", "nyx", "zephyra", "kael", "vexara", "draven"};
    public static final String[] HERO_CLASSES        = {
            "HEAVY VANGUARD",
            "SHADOW DUELIST",
            "ARCANE WEAVER",
            "SPELLSWORD DUELIST",
            "NETHER REAPER",
            "VOLCANIC BRAWLER"
    };
    public static final String[] HERO_SUBTITLES      = {
            "BULWARK & SWORD",
            "DUAL VENOM DAGGERS",
            "ARCANE FOCUS & RUNES",
            "CRIMSON RAPIER",
            "NETHER SCYTHE",
            "MOLTEN GAUNTLETS"
    };
    public static final String[] HERO_LORE = {
            "Unbreakable frontline titan clad in black and gold plate armor, sunburst shield, and greatsword.",
            "Lethal shadow stalker striking with supernatural agility, shadow step, and twin venom daggers.",
            "Spatial spellweaver manipulating dimensional tears, mystic bolts, and orbiting focus crystals.",
            "Master fencing spellsword channeling crimson arcane energy through lightning-fast rapier thrusts.",
            "Spectral necromancer cloaked in shadows, wielding an abyssal scythe that siphons life from foes.",
            "Volcanic juggernaut encased in molten stone armor, delivering earth-shattering magma punches."
    };
    public static final String[] HERO_SPECIAL_NAMES = {
            "SHIELD SLAM",
            "SHADOW STRIKE",
            "ARCANE NOVA",
            "CRIMSON THRUST",
            "SOUL REAP",
            "MAGMA ERUPTION"
    };
    public static final String[] HERO_SPECIAL_DESCS = {
            "Invulnerable Guard & Heavy Stun",
            "Lethal Dash & Execute Strike",
            "360\u00b0 Explosive Spatial Rupture",
            "Piercing Triple Fencing Flurry",
            "Dark Cleave & Siphons 15 HP",
            "Ground Slam & Molten Shockwave"
    };
    public static final String[] HERO_SPECIAL_DETAILS = {
            "100 SP \u00b7 Cooldown: 3.5s \u00b7 Defensive Aegis",
            "100 SP \u00b7 Cooldown: 3.2s \u00b7 High Mobility",
            "100 SP \u00b7 Cooldown: 4.5s \u00b7 360\u00b0 AoE Burst",
            "100 SP \u00b7 Cooldown: 3.5s \u00b7 Armor Piercing",
            "100 SP \u00b7 Cooldown: 4.2s \u00b7 Life Steal Heal",
            "100 SP \u00b7 Cooldown: 4.0s \u00b7 Heavy Knockback"
    };

    // Attribute pips (1-5 scale) — Aegis, Nyx, Zephyra, Kael, Vexara, Draven
    public static final int[] ATTR_HP    = {5, 4, 3, 4, 3, 5}; // Vitality
    public static final int[] ATTR_POW   = {4, 4, 5, 4, 5, 5}; // Attack
    public static final int[] ATTR_AGI   = {3, 5, 3, 5, 3, 2}; // Mobility
    public static final int[] ATTR_RANGE = {2, 2, 5, 3, 4, 2}; // Range

    public static final Color[] HERO_ACCENTS = {
            Theme.HERO_WARRIOR,             // Aegis (Gold/Steel)
            Theme.HERO_ASSASSIN,            // Nyx (Emerald)
            Theme.HERO_MAGE,                // Zephyra (Violet)
            new Color(0xEF, 0x53, 0x50),    // Kael (Crimson)
            new Color(0xBA, 0x68, 0xC8),    // Vexara (Nether Violet)
            new Color(0xFF, 0x6D, 0x00)     // Draven (Molten Orange)
    };

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

    private final MainFrame frame;
    private final BufferedImage backgroundImage;
    private final JLabel subtitleLabel;
    private final Theme.RiftButton modeButton;
    private final Theme.RiftButton startButton;
    private final HeroRosterCard[] cards = new HeroRosterCard[HERO_INTERNAL_NAMES.length];
    private final HeroStagePreview stagePreview;
    private final List<EmberParticle> embers = new ArrayList<>();
    private int selectedHeroIndex = 0;

    private float idleTime = 0.0f;
    private final Timer idleTimer;

    public HeroSelectPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(0x0E, 0x0A, 0x18));

        this.backgroundImage = loadMenuBackground();

        initAtmosphericParticles();

        idleTimer = new Timer(16, e -> {
            idleTime += 0.016f;
            updateEmbers();
            repaint();
        });
        if (!GraphicsEnvironment.isHeadless()) {
            idleTimer.start();
        }

        // 4-Way Keyboard Navigation (WASD, Arrows, Enter, Space, Escape)
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
                    int prev = (selectedHeroIndex - 1 + HERO_INTERNAL_NAMES.length) % HERO_INTERNAL_NAMES.length;
                    selectHero(prev, false);
                } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
                    int next = (selectedHeroIndex + 1) % HERO_INTERNAL_NAMES.length;
                    selectHero(next, false);
                } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                    int next = (selectedHeroIndex - 3 + HERO_INTERNAL_NAMES.length) % HERO_INTERNAL_NAMES.length;
                    selectHero(next, false);
                } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                    int next = (selectedHeroIndex + 3) % HERO_INTERNAL_NAMES.length;
                    selectHero(next, false);
                } else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
                    launchSelectedHero();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    if (frame != null) {
                        frame.showScreen(MainFrame.Screen.MENU);
                    }
                }
            }
        });

        // --- Header Section --------------------------------------------
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));

        JLabel titleLabel = new JLabel("CHOOSE YOUR CHAMPION", JLabel.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 28));
        titleLabel.setForeground(Theme.GOLD_BRIGHT);

        subtitleLabel = new JLabel("TARGET: RIFT SPAWN (ARENA SKIRMISH)  \u00b7  CHOOSE YOUR CHAMPION", JLabel.CENTER);
        subtitleLabel.setFont(Theme.SUBTITLE_FONT);
        subtitleLabel.setForeground(Theme.TEXT_DIM);

        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // --- Center Content: Stage Showcase (Left) + 2x3 Roster Grid (Right) ---
        JPanel centerContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 4));
        centerContent.setOpaque(false);

        stagePreview = new HeroStagePreview();
        centerContent.add(stagePreview);

        JPanel rosterGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        rosterGrid.setOpaque(false);
        rosterGrid.setPreferredSize(new Dimension(720, 590));

        for (int i = 0; i < HERO_INTERNAL_NAMES.length; i++) {
            int index = i;
            HeroRosterCard card = new HeroRosterCard(index);
            card.addActionListener(e -> {
                requestFocusInWindow();
                selectHero(index, false);
            });
            cards[i] = card;
            rosterGrid.add(card);
        }
        centerContent.add(rosterGrid);
        add(centerContent, BorderLayout.CENTER);

        // --- Footer Navigation Bar -------------------------------------
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 36, 16, 36));

        Theme.RiftButton backButton = Theme.button("\u25c0  MAIN MENU [ESC]", Theme.VIOLET_DIM);
        backButton.setPreferredSize(new Dimension(200, 42));
        backButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        backButton.setFocusable(false);
        backButton.addActionListener(e -> {
            if (frame != null) {
                frame.showScreen(MainFrame.Screen.MENU);
            }
        });
        footer.add(backButton, BorderLayout.WEST);

        // Center-Right cluster: Mode toggle & Start action
        JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightCluster.setOpaque(false);

        modeButton = Theme.button("\u2694  SWITCH TO BOSS FIGHT", Theme.GOLD_DIM);
        modeButton.setPreferredSize(new Dimension(230, 42));
        modeButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        modeButton.setFocusable(false);
        modeButton.addActionListener(e -> {
            if (frame != null) {
                MainFrame.EncounterType current = frame.getEncounterType();
                frame.setEncounterType(current == MainFrame.EncounterType.BOSS
                        ? MainFrame.EncounterType.SKIRMISH
                        : MainFrame.EncounterType.BOSS);
                updateEncounterHeader();
            }
        });
        rightCluster.add(modeButton);

        startButton = Theme.button("ENTER THE RIFT [ENTER]  \u25b6", Theme.GOLD_BRIGHT);
        startButton.setFont(new Font("Serif", Font.BOLD, 15));
        startButton.setPreferredSize(new Dimension(250, 42));
        startButton.setFocusable(false);
        startButton.addActionListener(e -> launchSelectedHero());
        rightCluster.add(startButton);

        footer.add(rightCluster, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        // Initial default selection
        selectHero(0, false);
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

        try (InputStream is = HeroSelectPanel.class.getResourceAsStream("/assets/menu/main_menu_background.png")) {
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
        Random rand = new Random(4242);
        for (int i = 0; i < 32; i++) {
            EmberParticle p = new EmberParticle();
            p.normX = rand.nextFloat();
            p.normY = rand.nextFloat();
            p.size = 1.2f + rand.nextFloat() * 2.2f;
            p.speedY = 0.025f + rand.nextFloat() * 0.065f;
            p.driftFreq = 0.7f + rand.nextFloat() * 1.3f;
            p.driftAmp = 0.010f + rand.nextFloat() * 0.020f;
            p.phase = rand.nextFloat() * (float) (Math.PI * 2);
            p.isGold = rand.nextFloat() > 0.45f;
            embers.add(p);
        }
    }

    private void updateEmbers() {
        for (EmberParticle p : embers) {
            p.normY -= p.speedY * 0.016f;
            if (p.normY < -0.04f) {
                p.normY = 1.04f;
                p.normX = (float) Math.random();
            }
        }
    }

    public void selectHero(int index, boolean startImmediately) {
        if (index < 0 || index >= HERO_INTERNAL_NAMES.length) {
            return;
        }
        this.selectedHeroIndex = index;
        for (HeroRosterCard card : cards) {
            if (card != null) {
                card.repaint();
            }
        }
        if (stagePreview != null) {
            stagePreview.repaint();
        }
        if (startImmediately) {
            launchSelectedHero();
        }
    }

    public void launchSelectedHero() {
        if (frame != null) {
            frame.startBattle(HERO_INTERNAL_NAMES[selectedHeroIndex]);
        }
    }

    public void updateEncounterHeader() {
        if (frame == null) {
            return;
        }
        MainFrame.EncounterType type = frame.getEncounterType();
        if (type == MainFrame.EncounterType.BOSS) {
            subtitleLabel.setText("TARGET: THE RIFT WARDEN (3-PHASE BOSS)  \u00b7  CHOOSE YOUR CHAMPION");
            subtitleLabel.setForeground(Theme.GOLD_BRIGHT);
            modeButton.setText("\u25c0  SWITCH TO SKIRMISH");
        } else {
            subtitleLabel.setText("TARGET: RIFT SPAWN (ARENA SKIRMISH)  \u00b7  CHOOSE YOUR CHAMPION");
            subtitleLabel.setForeground(Theme.TEXT_DIM);
            modeButton.setText("\u2694  SWITCH TO BOSS FIGHT");
        }
    }

    public Theme.RiftButton getModeButton() {
        return modeButton;
    }

    public String getSubtitleText() {
        return subtitleLabel.getText();
    }

    public int getSelectedHeroIndex() {
        return selectedHeroIndex;
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

        // 1. Edge-to-Edge Cinematic Background (Reusing Main Menu Artwork)
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, width, height, null);
        } else {
            GradientPaint bg = new GradientPaint(
                    0, 0, new Color(0x1A, 0x12, 0x2C),
                    0, height, new Color(0x10, 0x08, 0x14)
            );
            g2.setPaint(bg);
            g2.fillRect(0, 0, width, height);
        }

        // 2. Translucent Dark-Glass Vignette Overlay (Gives high contrast for cards and text)
        GradientPaint overlay = new GradientPaint(
                0, 0, new Color(0x0A, 0x06, 0x14, 190),
                0, height, new Color(0x06, 0x03, 0x0C, 220)
        );
        g2.setPaint(overlay);
        g2.fillRect(0, 0, width, height);

        // 3. Subtle Pulsing Rift Glow in Top-Center Background
        float portalPulse = 0.5f + 0.5f * (float) Math.sin(idleTime * 1.5f);
        int portalX = width / 2;
        int portalY = (int) (height * 0.16f);
        int portalRadius = (int) (Math.min(width, height) * 0.28f);

        RadialGradientPaint portalGlow = new RadialGradientPaint(
                portalX, portalY, portalRadius,
                new float[]{0.0f, 0.50f, 1.0f},
                new Color[]{
                        new Color(175, 60, 255, (int) (38 + 24 * portalPulse)),
                        new Color(110, 25, 185, (int) (18 + 14 * portalPulse)),
                        new Color(0, 0, 0, 0)
                }
        );
        g2.setPaint(portalGlow);
        g2.fillOval(portalX - portalRadius, portalY - portalRadius, portalRadius * 2, portalRadius * 2);

        // 4. Floating Cosmic Embers
        for (EmberParticle p : embers) {
            float px = ((p.normX + (float) Math.sin(idleTime * p.driftFreq + p.phase) * p.driftAmp) % 1.0f) * width;
            float py = p.normY * height;

            if (px < 0 || px > width || py < 0 || py > height) {
                continue;
            }

            float flicker = 0.6f + 0.4f * (float) Math.sin(idleTime * 4.0f + p.phase);
            int alpha = (int) (160 * flicker);

            Color color = p.isGold
                    ? new Color(255, 215, 80, alpha)
                    : new Color(195, 125, 255, alpha);

            g2.setColor(color);
            int s = Math.max(1, Math.round(p.size));
            g2.fillRect(Math.round(px), Math.round(py), s, s);
        }

        g2.dispose();
    }

    // ==================================================================
    // HeroStagePreview — Prominent Left Stage Showcase for Selected Hero
    // ==================================================================
    private final class HeroStagePreview extends JPanel {

        private static final int STAGE_WIDTH = 460;
        private static final int STAGE_HEIGHT = 590;

        HeroStagePreview() {
            setPreferredSize(new Dimension(STAGE_WIDTH, STAGE_HEIGHT));
            setOpaque(false);
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
            int centreX = width / 2;
            int arc = 16;

            int idx = selectedHeroIndex;
            String displayName = HERO_DISPLAY_NAMES[idx];
            String assetKey = HERO_ASSET_KEYS[idx];
            String roleTitle = HERO_CLASSES[idx];
            String lore = HERO_LORE[idx];
            String specialName = HERO_SPECIAL_NAMES[idx];
            String specialDesc = HERO_SPECIAL_DESCS[idx];
            String specialDetail = HERO_SPECIAL_DETAILS[idx];
            Color accent = HERO_ACCENTS[idx];

            // 1. Translucent Dark-Glass Stage Card Backing
            g2.setPaint(new GradientPaint(
                    0, 0, new Color(0x18, 0x0E, 0x28, 220),
                    0, height, new Color(0x0C, 0x06, 0x16, 240)
            ));
            g2.fill(new RoundRectangle2D.Double(2, 2, width - 4, height - 4, arc, arc));

            // Outer radiant accent rim
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 75));
            g2.setStroke(new BasicStroke(4.0f));
            g2.draw(new RoundRectangle2D.Double(3.5, 3.5, width - 7, height - 7, arc, arc));

            // Inner luminous gold/accent border
            g2.setColor(Theme.GOLD_BRIGHT);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Double(2.5, 2.5, width - 5, height - 5, arc, arc));

            // 2. Archetype Status Badge
            g2.setColor(accent);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String badge = "\u2605  " + roleTitle + "  \u2605";
            g2.drawString(badge, centreX - fm.stringWidth(badge) / 2, 24);

            // 3. Directional Backlighting Aura behind Champion
            int auraY = 160;
            int auraRadius = 110;
            RadialGradientPaint aura = new RadialGradientPaint(
                    centreX, auraY, auraRadius,
                    new float[]{0.0f, 0.50f, 1.0f},
                    new Color[]{
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 95),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30),
                            new Color(0, 0, 0, 0)
                    }
            );
            g2.setPaint(aura);
            g2.fill(new Ellipse2D.Double(centreX - auraRadius, auraY - auraRadius * 0.75f, auraRadius * 2, auraRadius * 1.5f));

            // 4. Ambient Ground Contact Shadow
            int footY = 275;
            RadialGradientPaint shadow = new RadialGradientPaint(
                    centreX, footY, 70f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(0, 0, 0, 130), new Color(0, 0, 0, 40), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(shadow);
            g2.fill(new Ellipse2D.Double(centreX - 70, footY - 10, 140, 20));

            // 5. Large Champion Artwork Portrait (Crisp and imposing)
            BufferedImage portrait = HeroSpriteLoader.getHeroPortrait(assetKey);
            if (portrait != null) {
                int targetH = 245;
                float imgAspect = (float) portrait.getWidth() / Math.max(1, portrait.getHeight());
                int targetW = Math.round(targetH * imgAspect);
                int px = centreX - targetW / 2;
                int py = footY - targetH + 4;

                // Subtle floating breathing offset
                float floatOffset = (float) Math.sin(idleTime * 3.5f) * 2.5f;
                py += (int) floatOffset;

                g2.drawImage(portrait, px, py, targetW, targetH, null);
            }

            // 6. Champion Name Banner
            int nameY = 308;
            Font nameFont = new Font("Serif", Font.BOLD, 26);
            g2.setFont(nameFont);
            FontMetrics nfm = g2.getFontMetrics(nameFont);
            String nameStr = displayName.toUpperCase();

            // Shadow
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(nameStr, centreX - nfm.stringWidth(nameStr) / 2 + 1, nameY + 2);

            // Gilded gradient title
            g2.setColor(Theme.GOLD_BRIGHT);
            g2.drawString(nameStr, centreX - nfm.stringWidth(nameStr) / 2, nameY);

            // Gilded ornamental divider line
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(centreX - 120, nameY + 8, centreX + 120, nameY + 8);
            drawDiamond(g2, centreX, nameY + 8, 4);

            // 7. Fitted Lore Description
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(Theme.TEXT);
            drawWrappedText(g2, lore, 28, nameY + 26, width - 56, 16);

            // 8. Segmented Jewel Attribute Meters
            int attrY = nameY + 68;
            paintAttributePips(g2, "VITALITY", ATTR_HP[idx], Theme.GOLD_BRIGHT, 28, attrY, width - 56);
            attrY += 21;
            paintAttributePips(g2, "ATTACK", ATTR_POW[idx], Theme.HERO_WARRIOR, 28, attrY, width - 56);
            attrY += 21;
            paintAttributePips(g2, "MOBILITY", ATTR_AGI[idx], new Color(0x52, 0xE5, 0xFF), 28, attrY, width - 56);
            attrY += 21;
            paintAttributePips(g2, "RANGE", ATTR_RANGE[idx], new Color(0xBA, 0x68, 0xC8), 28, attrY, width - 56);

            // 9. Signature Special Move Showcase Box
            int skillBoxY = attrY + 24;
            int skillBoxW = width - 56;
            int skillBoxH = 62;

            g2.setColor(new Color(15, 10, 24, 220));
            g2.fillRoundRect(28, skillBoxY, skillBoxW, skillBoxH, 10, 10);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(28, skillBoxY, skillBoxW, skillBoxH, 10, 10);

            // Move Name & Key prompt
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(accent);
            g2.drawString("[K / RMB]  SPECIAL: " + specialName, 38, skillBoxY + 18);

            // Description
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(Theme.TEXT);
            g2.drawString(specialDesc, 38, skillBoxY + 36);

            // Details
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString(specialDetail, 38, skillBoxY + 52);

            // 10. Bottom Ready Prompt
            Font ctaFont = new Font("SansSerif", Font.BOLD, 12);
            g2.setFont(ctaFont);
            FontMetrics cfm = g2.getFontMetrics(ctaFont);
            String cta = "\u25b6  CHAMPION READY  \u25b6";
            float ctaPulse = 0.5f + 0.5f * (float) Math.sin(idleTime * 4.0f);
            g2.setColor(new Color(Theme.GOLD_BRIGHT.getRed(), Theme.GOLD_BRIGHT.getGreen(), Theme.GOLD_BRIGHT.getBlue(), (int) (180 + 75 * ctaPulse)));
            g2.drawString(cta, centreX - cfm.stringWidth(cta) / 2, height - 12);

            g2.dispose();
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

        private void paintAttributePips(Graphics2D g2, String label, int value, Color pipColor, int x, int y, int width) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString(label, x, y + 10);

            int pipsStartX = x + 85;
            int pipW = 20;
            int pipH = 8;
            int pipGap = 5;

            for (int i = 0; i < 5; i++) {
                int px = pipsStartX + i * (pipW + pipGap);
                if (i < value) {
                    g2.setPaint(new GradientPaint(px, y, pipColor.brighter(), px, y + pipH, pipColor.darker()));
                    g2.fillRoundRect(px, y + 2, pipW, pipH, 3, 3);
                    g2.setColor(pipColor.brighter());
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.drawRoundRect(px, y + 2, pipW, pipH, 3, 3);
                } else {
                    g2.setColor(new Color(28, 20, 42));
                    g2.fillRoundRect(px, y + 2, pipW, pipH, 3, 3);
                    g2.setColor(new Color(55, 42, 75));
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.drawRoundRect(px, y + 2, pipW, pipH, 3, 3);
                }
            }
        }

        private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
            FontMetrics fm = g2.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int currentY = y;

            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    g2.drawString(line.toString(), x, currentY);
                    currentY += lineHeight;
                    line = new StringBuilder(word);
                }
            }
            if (!line.isEmpty()) {
                g2.drawString(line.toString(), x, currentY);
            }
        }
    }

    // ==================================================================
    // HeroRosterCard — High-definition 2x3 grid selection card
    // ==================================================================
    private final class HeroRosterCard extends JButton {

        private final int heroIndex;
        private final String displayName;
        private final String assetKey;
        private final String roleTitle;
        private final String subTitle;
        private final Color accent;
        private boolean hovered;

        HeroRosterCard(int heroIndex) {
            this.heroIndex = heroIndex;
            this.displayName = HERO_DISPLAY_NAMES[heroIndex];
            this.assetKey = HERO_ASSET_KEYS[heroIndex];
            this.roleTitle = HERO_CLASSES[heroIndex];
            this.subTitle = HERO_SUBTITLES[heroIndex];
            this.accent = HERO_ACCENTS[heroIndex];

            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText("Select " + displayName + " (" + roleTitle + ")");

            addMouseListener(new MouseAdapter() {
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

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        launchSelectedHero();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int width = getWidth();
            int height = getHeight();
            int centreX = width / 2;
            int arc = 14;
            boolean isSelected = (selectedHeroIndex == heroIndex);

            // 1. Card Body with deep dark-glass gradient
            Color topBg = isSelected
                    ? new Color(0x2C, 0x1A, 0x42, 235)
                    : (hovered ? new Color(0x22, 0x14, 0x36, 225) : new Color(0x16, 0x0D, 0x24, 215));
            g2.setPaint(new GradientPaint(0, 0, topBg, 0, height, new Color(0x0C, 0x06, 0x16, 240)));
            g2.fill(new RoundRectangle2D.Double(2, 2, width - 4, height - 4, arc, arc));

            // 2. Beveled Metallic Border & Selection Aura
            if (isSelected) {
                // Outer glowing gold aura
                g2.setColor(new Color(Theme.GOLD_BRIGHT.getRed(), Theme.GOLD_BRIGHT.getGreen(), Theme.GOLD_BRIGHT.getBlue(), 90));
                g2.setStroke(new BasicStroke(4.5f));
                g2.draw(new RoundRectangle2D.Double(3.5, 3.5, width - 7, height - 7, arc, arc));

                // Bright golden rim
                g2.setColor(Theme.GOLD_BRIGHT);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(new RoundRectangle2D.Double(2.5, 2.5, width - 5, height - 5, arc, arc));

                // Corner ornamental diamond brackets
                drawDiamond(g2, 10, 10, 3);
                drawDiamond(g2, width - 10, 10, 3);
                drawDiamond(g2, 10, height - 10, 3);
                drawDiamond(g2, width - 10, height - 10, 3);
            } else if (hovered) {
                g2.setColor(accent.brighter());
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(new RoundRectangle2D.Double(2.5, 2.5, width - 5, height - 5, arc, arc));
            } else {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(2.5, 2.5, width - 5, height - 5, arc, arc));
            }

            // 3. Top Role Badge
            g2.setColor(isSelected ? Theme.GOLD_BRIGHT : accent);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            String badge = isSelected ? "\u2605  SELECTED  \u2605" : ("\u25c6 " + roleTitle + " \u25c6");
            g2.drawString(badge, centreX - fm.stringWidth(badge) / 2, 20);

            // 4. Portrait Area
            int footY = height - 44;
            // Soft ambient ground shadow
            RadialGradientPaint cardShadow = new RadialGradientPaint(
                    centreX, footY, 48f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(0, 0, 0, 110), new Color(0, 0, 0, 30), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(cardShadow);
            g2.fill(new Ellipse2D.Double(centreX - 48, footY - 6, 96, 12));

            BufferedImage portrait = HeroSpriteLoader.getHeroPortrait(assetKey);
            if (portrait != null) {
                int targetH = isSelected ? 175 : 160;
                float imgAspect = (float) portrait.getWidth() / Math.max(1, portrait.getHeight());
                int targetW = Math.round(targetH * imgAspect);
                int px = centreX - targetW / 2;
                int py = footY - targetH + 4;

                g2.drawImage(portrait, px, py, targetW, targetH, null);
            }

            // 5. Champion Name Banner & Weapon Subtitle
            g2.setFont(new Font("Serif", Font.BOLD, 16));
            FontMetrics nfm = g2.getFontMetrics();
            g2.setColor(isSelected ? Theme.GOLD_BRIGHT : Theme.TEXT);
            g2.drawString(displayName.toUpperCase(), centreX - nfm.stringWidth(displayName.toUpperCase()) / 2, height - 22);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            FontMetrics sfm = g2.getFontMetrics();
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString(subTitle, centreX - sfm.stringWidth(subTitle) / 2, height - 9);

            g2.dispose();
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
    }
}
