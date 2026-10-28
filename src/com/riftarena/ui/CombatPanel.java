package com.riftarena.ui;

import com.riftarena.game.Action;
import com.riftarena.game.CombatEngine;
import com.riftarena.game.CombatEvent;
import com.riftarena.game.Enemy;
import com.riftarena.game.Hero;
import com.riftarena.game.RiftWarden;
import com.riftarena.realtime.DungeonRun;
import com.riftarena.realtime.DungeonStage;
import com.riftarena.realtime.EnemyType;
import com.riftarena.realtime.EntityState;
import com.riftarena.realtime.Hitbox;
import com.riftarena.realtime.Projectile;
import com.riftarena.realtime.RealTimeArena;
import com.riftarena.realtime.RealTimeBoss;
import com.riftarena.realtime.RealTimeCombatEngine;
import com.riftarena.realtime.RealTimeEnemy;
import com.riftarena.realtime.RealTimePlayer;
import com.riftarena.ui.render.AnimationState;
import com.riftarena.ui.render.CharacterRenderer;
import com.riftarena.ui.render.CharacterRendererFactory;
import com.riftarena.ui.render.CombatEffectManager;
import com.riftarena.ui.render.PresentationQueue;
import com.riftarena.ui.render.RiftWardenRenderer;
import com.riftarena.ui.render.HeroSpriteLoader;
import com.riftarena.ui.render.FigureRenderer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

/**
 * Live combat screen connecting the Java Swing user interface to the
 * headless {@link CombatEngine}.
 *
 * All combat rules, HP mutations, enemy AI actions, and turn progressions are
 * computed exclusively by {@link CombatEngine}. The UI observes engine state,
 * renders distinct 2D character figures, plays animation states, spawns floating
 * combat effects, and enforces action availability.
 *
 * Syllabus concepts demonstrated:
 *   - Object-Oriented MVC / Model-View separation: Engine owns data, UI renders it.
 *   - Event handling: ActionListeners routing user input to engine commands.
 *   - Polymorphism: Hero and enemy specifics rendered dynamically via CharacterRenderer contracts.
 *   - Swing EDT compliance: 60 FPS animation timer executes synchronously on the dispatch thread.
 */
public class CombatPanel extends JPanel {

    private static final String DEFAULT_ENEMY_NAME = "RIFT SPAWN";

    private final MainFrame frame;
    private CombatEngine engine;

    private String heroName = "Warrior";
    private Color heroAccent = Theme.HERO_WARRIOR;

    private final ArenaView arena = new ArenaView();
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("", JLabel.RIGHT);

    private Theme.RiftButton attackButton;
    private Theme.RiftButton defendButton;
    private Theme.RiftButton healButton;
    private Theme.RiftButton specialButton;
    private Theme.RiftButton viewResultsButton;
    private Theme.RiftButton surrenderButton;
    private Theme.RiftButton pauseButton;
    private Theme.RiftButton restartButton;
    private RealTimeCombatEngine rtEngine;
    private DungeonRun dungeonRun;

    public CombatPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(Theme.BACKGROUND);

        // Keep status label initialized for internal synchronization
        statusLabel.setFont(Theme.SMALL_FONT);
        statusLabel.setForeground(Theme.TEXT_DIM);

        // --- Centre: the full-width painted arena ----------------------
        add(arena, BorderLayout.CENTER);

        // --- South: sleek dark-glass action & navigation bar -----------
        add(buildActionBar(), BorderLayout.SOUTH);

        // Install comprehensive, window-wide key bindings so keys always work
        setupKeyBindings();
    }

    /**
     * Installs Swing Key Bindings on the WHEN_IN_FOCUSED_WINDOW input map.
     * This guarantees that controls (A, D, W, Space, J, K, Shift, ESC, R)
     * work reliably regardless of which button or component currently has focus.
     */
    private void setupKeyBindings() {
        javax.swing.InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getActionMap();

        int[] continuousKeys = {
            KeyEvent.VK_A, KeyEvent.VK_LEFT,
            KeyEvent.VK_D, KeyEvent.VK_RIGHT,
            KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_SPACE,
            KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_SHIFT
        };

        for (int keyCode : continuousKeys) {
            String pressKey = "PRESS_" + keyCode;
            String releaseKey = "RELEASE_" + keyCode;

            im.put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, false), pressKey);
            am.put(pressKey, new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (rtEngine != null) {
                        rtEngine.getInputState().onKeyPressed(keyCode);
                    }
                }
            });

            im.put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, true), releaseKey);
            am.put(releaseKey, new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (rtEngine != null) {
                        rtEngine.getInputState().onKeyReleased(keyCode);
                    }
                }
            });
        }

        // ESC = Pause / Resume
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "TOGGLE_PAUSE");
        am.put("TOGGLE_PAUSE", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (pauseButton != null && pauseButton.isEnabled()) {
                    pauseButton.doClick();
                }
            }
        });

        // R = Restart
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "RESTART");
        am.put("RESTART", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (restartButton != null && restartButton.isEnabled()) {
                    restartButton.doClick();
                }
            }
        });

        // H or L = Toggle in-arena combat log overlay
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "TOGGLE_LOG");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "TOGGLE_LOG");
        am.put("TOGGLE_LOG", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (arena != null) {
                    arena.toggleLogDrawer();
                }
            }
        });
    }

    /**
     * Starts a full dungeon run with the specified Hero.
     */
    public void startDungeon(Hero hero) {
        startBattle(new CombatEngine(hero, new Enemy("Rift Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL)));
    }

    /**
     * Binds a fresh combat engine to the screen and initializes live display state.
     * Guaranteed to wipe previous log and reset UI controls.
     *
     * @param engine the authoritative, headless combat engine
     */
    public void startBattle(CombatEngine engine) {
        this.engine = Objects.requireNonNull(engine, "Combat engine cannot be null");
        Hero hero = engine.getHero();
        this.heroName = hero.getName();
        this.heroAccent = accentFor(heroName);

        arena.initFighters(engine.getHero(), engine.getEnemy());
        this.rtEngine = arena.getRealTimeEngine();

        logArea.setText("");
        if (engine.getEnemy() instanceof RiftWarden) {
            this.dungeonRun = new DungeonRun(hero, rtEngine);
            this.dungeonRun.startBossDirectly();
            arena.setDungeonRun(dungeonRun);
            setupDungeonListeners();
            log("==================================================");
            log("*** BOSS ENCOUNTER: THE RIFT WARDEN ***");
            log("An ancient guardian awakens to defend the dimensional rift!");
            log("Phase I: Guardian active. Watch for runic defenses.");
            log("Direct Real-Time Control: [A/D] Move  [W/Space] Jump  [J] Attack  [K] Special  [Shift] Dash");
            log("==================================================");
        } else {
            this.dungeonRun = new DungeonRun(hero, rtEngine);
            arena.setDungeonRun(dungeonRun);
            setupDungeonListeners();
            log("The rift stirs... " + heroName + " steps into the arena to face "
                    + engine.getEnemy().getName() + ".");
            log("Round 1 begins. Choose your tactical action below.");
            log("Direct Real-Time Control: [A/D] Move  [W/Space] Jump  [J] Attack  [K] Special  [Shift] Dash");
        }

        setupRealTimeListeners();
        updateControlsAndStatus();
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void setupDungeonListeners() {
        if (dungeonRun == null) {
            return;
        }
        dungeonRun.addListener(new DungeonRun.DungeonEventListener() {
            @Override
            public void onStageChanged(DungeonStage previousStage, DungeonStage newStage) {
                log(">>> [STAGE] " + newStage.getTitle() + " - " + newStage.getDescription());
                arena.getEffectManager().triggerScreenShake(0.25f, 3.5f);
            }

            @Override
            public void onAnnouncement(String title, String subtext, float duration) {
                log("[ANNOUNCEMENT] " + title + (subtext != null && !subtext.isEmpty() ? " \u00b7 " + subtext : ""));
            }

            @Override
            public void onEnemyKilled(RealTimeEnemy enemy, int totalKills) {
                log(heroName + " vanquished " + enemy.getEnemyName() + "! (" + totalKills + " enemies cleared)");
                boolean isBoss = enemy instanceof RealTimeBoss;
                Color deathColor = isBoss ? Theme.GOLD_BRIGHT : ((enemy.getType() == EnemyType.RIFT_CASTER) ? new Color(0x9B, 0x59, 0xB6) : Theme.VIOLET);
                arena.getEffectManager().spawnDeathDissolution((int) enemy.getX(), (int) enemy.getY() - 60, deathColor, isBoss);
            }

            @Override
            public void onDungeonConcluded(boolean victory, float elapsedTime, int totalKills) {
                if (victory) {
                    log("==================================================");
                    log("*** DUNGEON CLEARED! Total Kills: " + totalKills + " | Elapsed Time: " + String.format("%.1fs", elapsedTime) + " ***");
                    log("Performance Rank: " + dungeonRun.getPerformanceRank());
                    log("==================================================");
                } else {
                    log("==================================================");
                    log("*** HERO FELL IN THE DEPTHS! Defeated: " + totalKills + " enemies ***");
                    log("==================================================");
                }
                if (viewResultsButton != null) {
                    viewResultsButton.setVisible(true);
                    viewResultsButton.setEnabled(true);
                }
            }
        });
    }

    private void setupRealTimeListeners() {
        if (rtEngine == null) {
            return;
        }
        rtEngine.addFeedbackListener(new RealTimeCombatEngine.CombatFeedbackListener() {
            @Override
            public void onDamageDealt(String attackerName, String targetName, float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
                arena.getEffectManager().spawnFloatingDamage((int) hitX, (int) hitY - 35, damage, isCrit);
                float knockbackDir = isPlayerTarget ? -1.0f : 1.0f;
                arena.getEffectManager().spawnHitSparks((int) hitX, (int) hitY, isPlayerTarget ? Theme.DANGER : heroAccent, knockbackDir);
                if (isPlayerTarget) {
                    arena.getEffectManager().spawnSlash((int) hitX, (int) hitY, Theme.VIOLET, 50, true);
                    log(attackerName + " strikes " + targetName + " for " + damage + " damage!");
                } else {
                    if ("Mage".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSpellBurst((int) hitX, (int) hitY, Theme.HERO_MAGE);
                    } else if ("Assassin".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSlash((int) hitX, (int) hitY, Theme.HERO_ASSASSIN, 55, false);
                    } else {
                        arena.getEffectManager().spawnSlash((int) hitX, (int) hitY, Theme.GOLD_BRIGHT, 60, false);
                    }
                    String prefix = isCrit ? "[CRITICAL HIT] " : "";
                    log(prefix + attackerName + " hits " + targetName + " for " + damage + " damage!");
                }
                if (isCrit) {
                    arena.getEffectManager().triggerScreenShake(0.35f, 5.5f);
                } else {
                    arena.getEffectManager().triggerScreenShake(0.18f, 2.5f);
                }
            }

            @Override
            public void onDamageDealt(float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
                onDamageDealt("Attacker", "Target", hitX, hitY, damage, isCrit, isPlayerTarget);
            }

            @Override
            public void onAttackPerformed(String attacker, boolean isSpecial) {
                if (rtEngine == null) {
                    return;
                }
                RealTimePlayer p = rtEngine.getPlayer();
                if (p == null) {
                    return;
                }
                int swingX = (int) (p.getX() + (p.isFacingRight() ? 50 : -50));
                int swingY = (int) (p.getY() - 45);

                if (isSpecial) {
                    if ("Warrior".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnShieldFlash((int) p.getX(), (int) p.getY() - 40, Theme.GOLD_BRIGHT);
                        arena.getEffectManager().spawnFloatingShield((int) p.getX(), (int) p.getY() - 75, "SHIELD SLAM");
                        arena.getEffectManager().triggerScreenShake(0.30f, 4.5f);
                    } else if ("Mage".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSpellBurst((int) p.getX(), (int) p.getY() - 50, Theme.HERO_MAGE);
                        arena.getEffectManager().triggerScreenShake(0.40f, 6.0f);
                    } else if ("Assassin".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSlash((int) p.getX(), (int) p.getY() - 45, Theme.HERO_ASSASSIN, 65, !p.isFacingRight());
                        arena.getEffectManager().triggerScreenShake(0.25f, 4.0f);
                    }
                } else {
                    if ("Warrior".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSlash(swingX, swingY, Theme.GOLD_BRIGHT, 60, !p.isFacingRight());
                    } else if ("Mage".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSpellBurst(swingX, swingY, Theme.HERO_MAGE);
                    } else if ("Assassin".equalsIgnoreCase(heroName)) {
                        arena.getEffectManager().spawnSlash(swingX, swingY, Theme.HERO_ASSASSIN, 50, !p.isFacingRight());
                    }
                }
            }

            @Override
            public void onGameOver(boolean victory) {
                if (dungeonRun != null) {
                    return; // In dungeon mode, dungeonRun handles overall outcome & results
                }
                if (victory) {
                    log("*** VICTORY! The enemy has fallen in battle! ***");
                } else {
                    log("*** DEFEAT! " + heroName + " has been defeated! ***");
                }
                if (viewResultsButton != null) {
                    viewResultsButton.setVisible(true);
                    viewResultsButton.setEnabled(true);
                }
            }

            @Override
            public void onBossPhaseTransition(RealTimeBoss boss, int newPhase) {
                log(">>> [BOSS] The Rift Warden enters Phase " + newPhase + " (Cataclysm Awakens)!");
                int cx = arena.getWidth() > 0 ? arena.getWidth() / 2 : 512;
                int cy = arena.getHeight() > 0 ? (int) (arena.getHeight() * 0.42) : 250;
                arena.getEffectManager().spawnPhaseTransition(cx, cy, "CATACLYSM PHASE II", Theme.GOLD_BRIGHT);
                arena.getEffectManager().triggerScreenShake(0.5f, 7.0f);
            }
        });
    }

    /**
     * Backward-compatible delegation to start a battle with the given hero name.
     */
    public void showHero(String name) {
        if (frame != null) {
            frame.startBattle(name);
        }
    }

    // ------------------------------------------------------------------
    // Construction helpers
    // ------------------------------------------------------------------

    private JPanel buildLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout(0, 8));
        logPanel.setOpaque(false);

        JLabel logTitle = new JLabel("COMBAT LOG");
        logTitle.setFont(Theme.BODY_FONT);
        logTitle.setForeground(Theme.GOLD);
        logPanel.add(logTitle, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(Theme.SMALL_FONT);
        logArea.setForeground(Theme.TEXT);
        logArea.setBackground(Theme.PANEL_DEEP);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(280, 360));
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.PANEL_HOVER, 1));
        scrollPane.getViewport().setBackground(Theme.PANEL_DEEP);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        return logPanel;
    }

    private JPanel buildActionBar() {
        JPanel actionBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();

                // Dark obsidian glass gradient
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x0C, 0x08, 0x14, 250), 0, h, new Color(0x05, 0x03, 0x0A, 255));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Top gilded divider line
                g2.setColor(new Color(0x9E, 0x7D, 0x3B));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, 1, w, 1);

                // Gilded subtle highlight
                g2.setColor(new Color(0xDF, 0xC4, 0x7E, 80));
                g2.drawLine(w / 4, 1, 3 * w / 4, 1);

                // Central diamond ornament on the divider line
                int cx = w / 2;
                Polygon diamond = new Polygon();
                diamond.addPoint(cx, -3);
                diamond.addPoint(cx + 6, 2);
                diamond.addPoint(cx, 7);
                diamond.addPoint(cx - 6, 2);
                g2.setColor(new Color(0xD4, 0xAF, 0x37));
                g2.fill(diamond);
                g2.setColor(new Color(0xFF, 0xF2, 0xC2));
                g2.draw(diamond);

                g2.dispose();
            }
        };
        actionBar.setOpaque(false);
        actionBar.setPreferredSize(new Dimension(1280, 68));
        actionBar.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));

        // Tactical combat actions (kept instantiated for programmatic test compatibility)
        JPanel hiddenTactical = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        hiddenTactical.setVisible(false);
        hiddenTactical.setPreferredSize(new Dimension(0, 0));

        attackButton = Theme.button("ATTACK", Theme.HERO_WARRIOR);
        attackButton.setFocusable(false);
        attackButton.addActionListener(e -> performAction(Action.ATTACK));
        hiddenTactical.add(attackButton);

        defendButton = Theme.button("DEFEND", Theme.GOLD);
        defendButton.setFocusable(false);
        defendButton.addActionListener(e -> performAction(Action.DEFEND));
        hiddenTactical.add(defendButton);

        healButton = Theme.button("HEAL", Theme.HERO_ASSASSIN);
        healButton.setFocusable(false);
        healButton.addActionListener(e -> performAction(Action.HEAL));
        hiddenTactical.add(healButton);

        specialButton = Theme.button("SPECIAL", Theme.HERO_MAGE);
        specialButton.setFocusable(false);
        specialButton.addActionListener(e -> performAction(Action.SPECIAL));
        hiddenTactical.add(specialButton);

        actionBar.add(hiddenTactical, BorderLayout.NORTH);

        // Left Panel: Sleek CONTROLS key prompt badges
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 11));
        controlsPanel.setOpaque(false);

        JLabel controlsLabel = new JLabel("CONTROLS");
        controlsLabel.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.5f));
        controlsLabel.setForeground(new Color(0xD4, 0xAF, 0x37));
        controlsPanel.add(controlsLabel);

        controlsPanel.add(createControlPill("A/D", null, "Move"));
        controlsPanel.add(createControlPill("Space", null, "Jump"));
        controlsPanel.add(createControlPill("J", null, "Attack"));
        controlsPanel.add(createControlPill("K", null, "Special"));
        controlsPanel.add(createControlPill("Shift", null, "Dash"));

        actionBar.add(controlsPanel, BorderLayout.WEST);

        // Right Panel: Primary Game Actions & Navigation
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 11));
        buttonsPanel.setOpaque(false);

        pauseButton = Theme.button("PAUSE [ESC]", Theme.VIOLET_DIM);
        pauseButton.setFocusable(false);
        pauseButton.setPreferredSize(new Dimension(126, 48));
        pauseButton.addActionListener(e -> {
            RealTimeCombatEngine rt = getRealTimeEngine();
            if (rt != null && !rt.isGameOver()) {
                rt.getInputState().consumePause();
                rt.setPaused(!rt.isPaused());
                pauseButton.setText(rt.isPaused() ? "RESUME [ESC]" : "PAUSE [ESC]");
                arena.requestFocusInWindow();
                arena.repaint();
            }
        });
        buttonsPanel.add(pauseButton);

        restartButton = Theme.button("RESTART [R]", Theme.GOLD_DIM);
        restartButton.setFocusable(false);
        restartButton.setPreferredSize(new Dimension(136, 48));
        restartButton.addActionListener(e -> {
            if (dungeonRun != null) {
                dungeonRun.restart();
            } else {
                RealTimeCombatEngine rt = getRealTimeEngine();
                if (rt != null) {
                    rt.restart();
                }
            }
            if (pauseButton != null) {
                pauseButton.setText("PAUSE [ESC]");
            }
            if (viewResultsButton != null) {
                viewResultsButton.setVisible(false);
                viewResultsButton.setEnabled(false);
            }
            updateControlsAndStatus();
            arena.requestFocusInWindow();
            arena.repaint();
        });
        buttonsPanel.add(restartButton);

        surrenderButton = Theme.button("SURRENDER", Theme.DANGER);
        surrenderButton.setFocusable(false);
        surrenderButton.setPreferredSize(new Dimension(140, 48));
        surrenderButton.addActionListener(e -> {
            if (dungeonRun != null && !dungeonRun.isGameOver()) {
                log("[Surrender] " + heroName + " retreated from the dungeon.");
                if (frame != null) {
                    int seconds = (int) Math.ceil(dungeonRun.getTotalElapsedTime());
                    int kills = dungeonRun.getTotalEnemiesDefeated();
                    frame.showResult(false, seconds, kills, "D-RANK (SURRENDERED)");
                }
            } else {
                RealTimeCombatEngine rt = getRealTimeEngine();
                if (rt != null && !rt.isGameOver()) {
                    log("[Surrender] " + heroName + " conceded the contest.");
                    if (frame != null) {
                        frame.showResult(false, (int) Math.ceil(rt.getMatchTime()));
                    }
                } else if (engine != null && !engine.isGameOver()) {
                    log("[Surrender] " + heroName + " conceded the contest.");
                    if (frame != null) {
                        frame.showResult(false, engine.getTurnCount());
                    }
                }
            }
        });
        buttonsPanel.add(surrenderButton);

        Theme.RiftButton changeHeroButton = Theme.button("CHANGE HERO", Theme.VIOLET_DIM);
        changeHeroButton.setFocusable(false);
        changeHeroButton.setPreferredSize(new Dimension(154, 48));
        changeHeroButton.addActionListener(e -> {
            if (frame != null) {
                frame.showScreen(MainFrame.Screen.HERO_SELECT);
            }
        });
        buttonsPanel.add(changeHeroButton);

        viewResultsButton = Theme.button("VIEW RESULTS", Theme.GOLD_BRIGHT);
        viewResultsButton.setFocusable(false);
        viewResultsButton.setPreferredSize(new Dimension(140, 48));
        viewResultsButton.addActionListener(e -> {
            if (frame != null) {
                if (dungeonRun != null) {
                    boolean victory = dungeonRun.isVictory();
                    int seconds = (int) Math.ceil(dungeonRun.getTotalElapsedTime());
                    int kills = dungeonRun.getTotalEnemiesDefeated();
                    String rank = dungeonRun.getPerformanceRank();
                    frame.showResult(victory, seconds, kills, rank);
                } else {
                    RealTimeCombatEngine rt = getRealTimeEngine();
                    if (rt != null && rt.isGameOver()) {
                        frame.showResult(rt.isVictory(), (int) Math.ceil(rt.getMatchTime()));
                    } else if (engine != null && engine.isGameOver()) {
                        frame.showResult(engine.isVictory(), engine.getTurnCount());
                    }
                }
            }
        });
        viewResultsButton.setVisible(false);
        buttonsPanel.add(viewResultsButton);

        actionBar.add(buttonsPanel, BorderLayout.EAST);
        return actionBar;
    }

    private static JComponent createControlPill(String key1, String key2, String label) {
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0x16, 0x0E, 0x22, 220));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(new Color(0x44, 0x32, 0x5C, 200));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 8));

        JLabel keyLabel = new JLabel(key2 != null ? ("[" + key1 + "] [" + key2 + "]") : ("[" + key1 + "]"));
        keyLabel.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.5f));
        keyLabel.setForeground(Theme.GOLD_BRIGHT);
        pill.add(keyLabel);

        JLabel descLabel = new JLabel(label);
        descLabel.setFont(Theme.SMALL_FONT.deriveFont(Font.PLAIN, 10.5f));
        descLabel.setForeground(Theme.TEXT_DIM);
        pill.add(descLabel);

        return pill;
    }

    // ------------------------------------------------------------------
    // Combat interaction & UI synchronization
    // ------------------------------------------------------------------

    /**
     * Executes the requested action via the authoritative {@link CombatEngine},
     * logs all resulting events, updates controls, and plays visual animations.
     */
    private void performAction(Action action) {
        if (engine == null || engine.isGameOver()) {
            return;
        }

        // Fast-forward any in-flight animation before starting next turn
        arena.getPresentationQueue().finishImmediate();

        try {
            List<CombatEvent> events = engine.executePlayerAction(action);
            for (CombatEvent event : events) {
                log(event.message());
            }

            boolean isBoss = engine.getEnemy() instanceof RiftWarden;
            arena.getPresentationQueue().playEvents(events, heroName, isBoss, () -> {
                updateControlsAndStatus();
                arena.repaint();
            });
        } catch (Exception ex) {
            log("[Action Rejected] " + ex.getMessage());
        }

        updateControlsAndStatus();
        arena.repaint();
    }

    /**
     * Synchronizes button states, labels, tooltips, and header status with engine state.
     */
    private void updateControlsAndStatus() {
        if (engine == null) {
            statusLabel.setText("No active battle");
            return;
        }

        boolean active = !engine.isGameOver()
                && (dungeonRun == null || !dungeonRun.isGameOver())
                && (rtEngine == null || !rtEngine.isGameOver());
        attackButton.setEnabled(active);
        defendButton.setEnabled(active);
        healButton.setEnabled(active);

        Hero hero = engine.getHero();
        String specialName = hero.getSpecialName().toUpperCase();
        if (active && hero.isSpecialReady()) {
            specialButton.setText(specialName);
            specialButton.setEnabled(true);
        } else {
            int cd = hero.getSpecialCooldown();
            String cdLabel = cd > 0 ? " (" + cd + ")" : "";
            specialButton.setText(specialName + cdLabel);
            specialButton.setEnabled(false);
        }
        specialButton.setToolTipText(hero.getSpecialDescription());

        surrenderButton.setEnabled(active);
        viewResultsButton.setVisible(!active);
        viewResultsButton.setEnabled(!active);

        if (engine.isGameOver()) {
            if (engine.isVictory()) {
                String enemyTitle = (engine.getEnemy() instanceof RiftWarden)
                        ? "THE RIFT WARDEN VANQUISHED"
                        : engine.getEnemy().getName().toUpperCase() + " DEFEATED";
                statusLabel.setText("VICTORY  \u00b7  " + enemyTitle + " IN " + engine.getTurnCount() + " TURNS");
            } else {
                statusLabel.setText("DEFEAT  \u00b7  HERO FELL ON TURN " + engine.getTurnCount());
            }
        } else if (dungeonRun != null && dungeonRun.isGameOver()) {
            if (dungeonRun.isVictory()) {
                statusLabel.setText("DUNGEON CLEARED  \u00b7  " + dungeonRun.getTotalEnemiesDefeated() + " FOES SLAIN  \u00b7  RANK: " + dungeonRun.getPerformanceRank());
            } else {
                statusLabel.setText("HERO FELL  \u00b7  " + dungeonRun.getCurrentStage().getTitle().toUpperCase());
            }
        } else if (rtEngine != null && rtEngine.isGameOver()) {
            if (rtEngine.isVictory()) {
                statusLabel.setText("VICTORY  \u00b7  MATCH COMPLETED IN " + String.format("%.1fs", rtEngine.getMatchTime()));
            } else {
                statusLabel.setText("DEFEAT  \u00b7  HERO WAS SLAIN");
            }
        } else {
            if (engine.getEnemy() instanceof RiftWarden warden) {
                statusLabel.setText("ROUND " + engine.getTurnCount() + "  \u00b7  "
                        + warden.getCurrentPhase().getDisplayName().toUpperCase());
            } else if (dungeonRun != null) {
                statusLabel.setText(dungeonRun.getCurrentStage().getTitle().toUpperCase() + "  \u00b7  KILLS: " + dungeonRun.getTotalEnemiesDefeated());
            } else if (rtEngine != null) {
                statusLabel.setText("REAL-TIME ARENA  \u00b7  TIME: " + String.format("%.1fs", rtEngine.getMatchTime()));
            } else {
                statusLabel.setText("ROUND " + engine.getTurnCount() + "  \u00b7  " + engine.getCurrentPhase());
            }
        }
    }

    private void log(String message) {
        logArea.append("\u00bb " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        if (arena != null && message != null && (message.contains("STAGE") || message.contains("CRITICAL") || message.contains("vanquished"))) {
            arena.addToast(message);
        }
    }

    private static Color accentFor(String heroName) {
        if (heroName == null) {
            return Theme.HERO_WARRIOR;
        }
        return switch (heroName.toLowerCase()) {
            case "mage", "zephyra" -> Theme.HERO_MAGE;
            case "assassin", "nyx" -> Theme.HERO_ASSASSIN;
            case "kael" -> new Color(0xEF, 0x53, 0x50);
            case "vexara" -> new Color(0xBA, 0x68, 0xC8);
            case "draven" -> new Color(0xFF, 0x6D, 0x00);
            default -> Theme.HERO_WARRIOR;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme.drawFrame(g2, getWidth(), getHeight());
        g2.dispose();
    }

    // ------------------------------------------------------------------
    // Testing / Inspection accessors
    // ------------------------------------------------------------------

    public CombatEngine getEngine() {
        return engine;
    }

    public Theme.RiftButton getAttackButton() {
        return attackButton;
    }

    public Theme.RiftButton getDefendButton() {
        return defendButton;
    }

    public Theme.RiftButton getHealButton() {
        return healButton;
    }

    public Theme.RiftButton getSpecialButton() {
        return specialButton;
    }

    public Theme.RiftButton getViewResultsButton() {
        return viewResultsButton;
    }

    public Theme.RiftButton getSurrenderButton() {
        return surrenderButton;
    }

    public Theme.RiftButton getPauseButton() {
        return pauseButton;
    }

    public Theme.RiftButton getRestartButton() {
        return restartButton;
    }

    public String getLogText() {
        return logArea.getText();
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public String getStatusText() {
        return statusLabel.getText();
    }

    public CharacterRenderer getHeroRenderer() {
        return arena.heroRenderer;
    }

    public CharacterRenderer getEnemyRenderer() {
        return arena.enemyRenderer;
    }

    public AnimationState getHeroState() {
        return arena.heroState;
    }

    public AnimationState getEnemyState() {
        return arena.enemyState;
    }

    public CombatEffectManager getEffectManager() {
        return arena.effectManager;
    }

    public PresentationQueue getPresentationQueue() {
        return arena.presentationQueue;
    }

    public ArenaView getArenaView() {
        return arena;
    }

    public RealTimeCombatEngine getRealTimeEngine() {
        return arena.getRealTimeEngine();
    }

    public DungeonRun getDungeonRun() {
        return dungeonRun;
    }

    public void updateAnimation(float deltaSeconds) {
        arena.stepAnimation(deltaSeconds);
    }

    public void finishAllAnimations() {
        arena.presentationQueue.finishImmediate();
    }

    // ==================================================================
    // ArenaView — the painted battlefield displaying 2D character visuals
    // ==================================================================

    public final class ArenaView extends JComponent implements PresentationQueue.ArenaDirector {

        // Displayed fighter heights. Sprites are scaled up by 1.5x (heroes at ~310px,
        // boss at ~410px), making them the prominent visual centerpiece of the arena.
        private static final int HERO_DRAW_SIZE = 270;
        private static final int BOSS_DRAW_SIZE = 360;

        private CharacterRenderer heroRenderer = CharacterRendererFactory.forHeroName("Warrior");
        private CharacterRenderer enemyRenderer = CharacterRendererFactory.forCombatant(
                new Enemy("Rift Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL));
        private final CombatEffectManager effectManager = new CombatEffectManager();
        private final PresentationQueue presentationQueue;

        private RealTimeCombatEngine rtEngine;
        private DungeonRun dungeonRun;

        private AnimationState heroState = AnimationState.IDLE;
        private AnimationState enemyState = AnimationState.IDLE;
        private float heroProgress = 0.0f;
        private float enemyProgress = 0.0f;
        private float idleTime = 0.0f;

        private java.awt.image.BufferedImage cachedBg = null;
        private int cachedW = -1;
        private int cachedH = -1;
        private boolean cachedBoss = false;

        private boolean showLogDrawer = false;
        private final java.util.List<String> toastMessages = new java.util.ArrayList<>();
        private final java.util.List<Float> toastTimers = new java.util.ArrayList<>();

        private final Timer animationTimer;

        public void addToast(String msg) {
            if (msg == null || msg.trim().isEmpty()) {
                return;
            }
            if (toastMessages.size() >= 2) {
                toastMessages.remove(0);
                toastTimers.remove(0);
            }
            toastMessages.add(msg);
            toastTimers.add(3.2f);
        }

        public void clearToasts() {
            toastMessages.clear();
            toastTimers.clear();
        }

        public void toggleLogDrawer() {
            showLogDrawer = !showLogDrawer;
            repaint();
        }

        public boolean isLogDrawerVisible() {
            return showLogDrawer;
        }

        ArenaView() {
            setPreferredSize(new Dimension(1280, 732));
            presentationQueue = new PresentationQueue(this);

            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (rtEngine != null) {
                        rtEngine.getInputState().onKeyPressed(e.getKeyCode());
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (rtEngine != null) {
                        rtEngine.getInputState().onKeyReleased(e.getKeyCode());
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    if (rtEngine == null || rtEngine.isGameOver() || rtEngine.isPaused()) {
                        return;
                    }
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        rtEngine.getInputState().onKeyPressed(KeyEvent.VK_J);
                    } else if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                        rtEngine.getInputState().onKeyPressed(KeyEvent.VK_K);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (rtEngine == null) {
                        return;
                    }
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        rtEngine.getInputState().onKeyReleased(KeyEvent.VK_J);
                    } else if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                        rtEngine.getInputState().onKeyReleased(KeyEvent.VK_K);
                    }
                }
            });

            animationTimer = new Timer(16, e -> {
                stepAnimation(0.016f);
                repaint();
            });
            if (!GraphicsEnvironment.isHeadless()) {
                animationTimer.start();
            }
        }

        public void stepAnimation(float deltaSeconds) {
            idleTime += deltaSeconds;

            for (int i = toastTimers.size() - 1; i >= 0; i--) {
                float t = toastTimers.get(i) - deltaSeconds;
                if (t <= 0) {
                    toastTimers.remove(i);
                    toastMessages.remove(i);
                } else {
                    toastTimers.set(i, t);
                }
            }

            if (dungeonRun != null) {
                dungeonRun.update(deltaSeconds);
                if (dungeonRun.isGameOver()) {
                    if (viewResultsButton != null) {
                        viewResultsButton.setVisible(true);
                        viewResultsButton.setEnabled(true);
                    }
                }
                updateControlsAndStatus();
            } else if (rtEngine != null) {
                rtEngine.update(deltaSeconds);
                if (rtEngine.isGameOver()) {
                    if (viewResultsButton != null) {
                        viewResultsButton.setVisible(true);
                        viewResultsButton.setEnabled(true);
                    }
                }
                updateControlsAndStatus();
            }

            effectManager.update(deltaSeconds);
            presentationQueue.update(deltaSeconds);
        }

        public void setDungeonRun(DungeonRun run) {
            this.dungeonRun = run;
        }

        public DungeonRun getDungeonRun() {
            return dungeonRun;
        }

        public void initFighters(Hero hero, Enemy enemy) {
            int w = getWidth() > 0 ? getWidth() : (getPreferredSize() != null ? getPreferredSize().width : 980);
            int h = getHeight() > 0 ? getHeight() : (getPreferredSize() != null ? getPreferredSize().height : 500);
            int groundY = (int) (h * 0.765f);
            RealTimeArena rtArena = new RealTimeArena(w, h, groundY, 110, w - 110);
            initFighters(hero, enemy, new RealTimeCombatEngine(hero, enemy, rtArena));
        }

        public void initFighters(Hero hero, Enemy enemy, RealTimeCombatEngine rtEngine) {
            this.heroRenderer = CharacterRendererFactory.forCombatant(hero);
            this.enemyRenderer = CharacterRendererFactory.forCombatant(enemy);
            this.rtEngine = rtEngine;
            this.heroState = AnimationState.IDLE;
            this.enemyState = AnimationState.IDLE;
            this.heroProgress = 0.0f;
            this.enemyProgress = 0.0f;
            this.effectManager.clearAll();
            this.presentationQueue.finishImmediate();
        }

        public RealTimeCombatEngine getRealTimeEngine() {
            return rtEngine;
        }

        public PresentationQueue getPresentationQueue() {
            return presentationQueue;
        }

        public CombatEffectManager getEffectManager() {
            return effectManager;
        }

        // --------------------------------------------------------------
        // ArenaDirector Implementation
        // --------------------------------------------------------------

        @Override
        public void setHeroState(AnimationState state) {
            this.heroState = state;
            repaint();
        }

        @Override
        public void setEnemyState(AnimationState state) {
            this.enemyState = state;
            repaint();
        }

        @Override
        public void setHeroProgress(float progress) {
            this.heroProgress = progress;
        }

        @Override
        public void setEnemyProgress(float progress) {
            this.enemyProgress = progress;
        }

        @Override
        public void spawnDamageOnHero(int amount, boolean isCrit) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int heroX = (int) (width * 0.28);
            int heroY = (int) (height * 0.44);
            effectManager.spawnFloatingDamage(heroX, heroY - 45, amount, isCrit);
            effectManager.spawnSlash(heroX, heroY, Theme.VIOLET, 55, false);
            if (isCrit) {
                effectManager.triggerScreenShake(0.35f, 5.0f);
            }
        }

        @Override
        public void spawnDamageOnEnemy(int amount, boolean isCrit) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int enemyX = (int) (width * 0.72);
            int enemyY = (int) (height * 0.44);
            effectManager.spawnFloatingDamage(enemyX, enemyY - 45, amount, isCrit);
            if (isCrit) {
                effectManager.triggerScreenShake(0.35f, 5.0f);
            }
        }

        @Override
        public void spawnHealOnHero(int amount) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int heroX = (int) (width * 0.28);
            int heroY = (int) (height * 0.44);
            effectManager.spawnFloatingHeal(heroX, heroY - 45, amount);
            effectManager.spawnSpellBurst(heroX, heroY, new Color(0x57, 0xF2, 0x87));
        }

        @Override
        public void spawnHealOnEnemy(int amount) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int enemyX = (int) (width * 0.72);
            int enemyY = (int) (height * 0.44);
            effectManager.spawnFloatingHeal(enemyX, enemyY - 45, amount);
            effectManager.spawnSpellBurst(enemyX, enemyY, new Color(0x57, 0xF2, 0x87));
        }

        @Override
        public void spawnShieldOnHero() {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int heroX = (int) (width * 0.28);
            int heroY = (int) (height * 0.44);
            effectManager.spawnShieldFlash(heroX, heroY, Theme.GOLD_BRIGHT);
            effectManager.spawnFloatingShield(heroX, heroY - 45, "GUARD 50%");
        }

        @Override
        public void spawnShieldOnEnemy() {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int enemyX = (int) (width * 0.72);
            int enemyY = (int) (height * 0.44);
            Color shieldColor = (engine != null && engine.getEnemy() instanceof RiftWarden)
                    ? Theme.GOLD_BRIGHT : Theme.VIOLET;
            effectManager.spawnShieldFlash(enemyX, enemyY, shieldColor);
            effectManager.spawnFloatingShield(enemyX, enemyY - 45, "GUARD 50%");
        }

        @Override
        public void spawnHeroAttackEffect(String heroName, boolean isSpecial) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int enemyX = (int) (width * 0.72);
            int enemyY = (int) (height * 0.44);

            if ("Mage".equalsIgnoreCase(heroName)) {
                effectManager.spawnSpellBurst(enemyX, enemyY, Theme.HERO_MAGE);
                if (isSpecial) {
                    effectManager.triggerScreenShake(0.40f, 6.0f);
                }
            } else if ("Assassin".equalsIgnoreCase(heroName)) {
                effectManager.spawnSlash(enemyX, enemyY, Theme.HERO_ASSASSIN, 55, false);
                effectManager.spawnSlash(enemyX, enemyY, new Color(0x57, 0xF2, 0x87), 50, true);
                if (isSpecial) {
                    effectManager.triggerScreenShake(0.35f, 5.0f);
                }
            } else { // Warrior
                effectManager.spawnSlash(enemyX - 10, enemyY, Theme.GOLD_BRIGHT, 60, false);
                if (isSpecial) {
                    effectManager.triggerScreenShake(0.35f, 5.5f);
                }
            }
        }

        @Override
        public void spawnEnemyAttackEffect(boolean isBoss, int phaseNumber, boolean isSpecial) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int heroX = (int) (width * 0.28);
            int heroY = (int) (height * 0.44);

            if (isBoss) {
                Color cleaveColor = (phaseNumber >= 3) ? Theme.DANGER
                        : ((phaseNumber == 2) ? Theme.GOLD_BRIGHT : Theme.VIOLET);
                effectManager.spawnSlash(heroX + 15, heroY, cleaveColor, 75, true);
                effectManager.triggerScreenShake(isSpecial ? 0.45f : 0.25f, isSpecial ? 7.0f : 4.0f);
            } else {
                effectManager.spawnSlash(heroX + 10, heroY, Theme.VIOLET, 50, true);
            }
        }

        @Override
        public void spawnPhaseTransition(int phaseNumber) {
            int width = getWidth() > 0 ? getWidth() : 700;
            int height = getHeight() > 0 ? getHeight() : 420;
            int enemyX = (int) (width * 0.72);
            int enemyY = (int) (height * 0.44);

            Color phaseColor = (phaseNumber >= 3) ? Theme.DANGER : Theme.GOLD_BRIGHT;
            String banner = (phaseNumber >= 3) ? "PHASE III: CATACLYSM" : "PHASE II: AWAKENING";
            effectManager.spawnPhaseTransition(enemyX, enemyY, banner, phaseColor);

            if (enemyRenderer instanceof RiftWardenRenderer wr) {
                wr.setPhase(phaseNumber >= 3 ? RiftWarden.Phase.PHASE_III_CATACLYSM : RiftWarden.Phase.PHASE_II_AWAKENING);
            }
        }

        @Override
        public void onBattleConcluded(boolean heroVictory) {
            if (heroVictory) {
                heroState = AnimationState.VICTORY;
                enemyState = AnimationState.DEFEAT;
            } else {
                heroState = AnimationState.DEFEAT;
                enemyState = AnimationState.VICTORY;
            }
            repaint();
        }

        // --------------------------------------------------------------
        // Painting
        // --------------------------------------------------------------

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth() > 0 ? getWidth() : (rtEngine != null ? (int) rtEngine.getArena().getWidth() : 980);
            int height = getHeight() > 0 ? getHeight() : (getPreferredSize() != null ? getPreferredSize().height : 500);
            int centreX = width / 2;
            int floorY = (rtEngine != null) ? (int) rtEngine.getArena().getGroundY() : (int) (height * 0.765f);

            boolean isBossAtmosphere = (dungeonRun != null && (dungeonRun.getCurrentStage() == DungeonStage.BOSS_GATE || dungeonRun.getCurrentStage() == DungeonStage.BOSS_ENCOUNTER))
                    || (engine != null && engine.getEnemy() instanceof RiftWarden);

            // 1. Camera screen shake
            float shakeX = effectManager.getShakeOffsetX();
            float shakeY = effectManager.getShakeOffsetY();
            g2.translate(shakeX, shakeY);

            // 2. Clean, environment-only gothic arena (cached for 60 FPS)
            paintAtmosphericArenaBackground(g2, width, height, floorY, centreX, isBossAtmosphere);

            // 3. Dynamic creeping horizon fog & floating rift embers
            paintDriftingHorizonMist(g2, width, floorY);
            paintFloatingEmbers(g2, width, height);

            if (rtEngine != null) {
                // ==============================================================
                // Real-Time Dynamic Simulation Rendering
                // ==============================================================
                RealTimePlayer player = rtEngine.getPlayer();
                List<RealTimeEnemy> activeEnemies = rtEngine.getEnemies();

                // 1. Boss Ground Aura (soft abyssal radiance, no artificial geometry)
                for (int i = 0; i < activeEnemies.size(); i++) {
                    RealTimeEnemy curEnemy = activeEnemies.get(i);
                    if (!curEnemy.isAlive() && curEnemy.getStateTime() > 0.8f) continue;
                    if (curEnemy instanceof RealTimeBoss boss) {
                        paintBossGroundAura(g2, (int) boss.getX(), (int) boss.getY(), boss.getBossPhase(), idleTime);
                    }
                }

                // 2. Projectiles
                for (Projectile proj : rtEngine.getProjectiles()) {
                    int px = (int) proj.getX();
                    int py = (int) proj.getY();
                    int rad = (int) proj.getRadius();

                    if (proj.isFromPlayer()) {
                        RadialGradientPaint glow = new RadialGradientPaint(px, py, rad * 2.2f,
                                new float[]{0f, 0.45f, 1f},
                                new Color[]{Color.WHITE, Theme.HERO_MAGE, new Color(0, 0, 0, 0)});
                        g2.setPaint(glow);
                        g2.fillOval(px - rad * 2, py - rad * 2, rad * 4, rad * 4);
                        g2.setColor(Color.WHITE);
                        g2.fillOval(px - rad / 2, py - rad / 2, rad, rad);
                    } else if (rad >= 18) {
                        paintGroundShockwave(g2, px, py, rad);
                    } else {
                        paintVoidProjectile(g2, px, py, rad);
                    }
                }

                // 3. Player Figure - anchored on the arena ground line, not on the panel centre
                AnimationState pState = mapEntityStateToAnimation(player.getState(), heroName, true);
                heroRenderer.render(g2, (int) player.getX(), (int) player.getY(), HERO_DRAW_SIZE, pState,
                        player.getStateProgress(), idleTime, player.isFacingRight());

                // Player Hurt Hit Flash
                if (player.isHurt()) {
                    float hurtProg = player.getHurtTimer() / 0.25f;
                    int alpha = (int) (Math.max(0.0f, Math.min(1.0f, hurtProg)) * 135);
                    FigureRenderer.hurtFlash(g2, player.getX(), player.getY(), HERO_DRAW_SIZE * 1.15f, alpha / 135f);
                }

                // Player Invulnerability / Guard Aura
                if (player.isInvulnerable() && ((int) (idleTime * 30) % 2 == 0)) {
                    g2.setColor(new Color(heroAccent.getRed(), heroAccent.getGreen(), heroAccent.getBlue(), 60));
                    int auraH = (int) (HERO_DRAW_SIZE * 1.12f);
                    g2.fillOval((int) player.getX() - 58, (int) player.getY() - auraH, 116, auraH);
                }

                // 4. All Active Enemies in Arena
                for (int i = 0; i < activeEnemies.size(); i++) {
                    RealTimeEnemy curEnemy = activeEnemies.get(i);
                    if (!curEnemy.isAlive() && curEnemy.getStateTime() > 0.8f) {
                        continue;
                    }

                    AnimationState eState = mapEntityStateToAnimation(curEnemy.getState(), curEnemy.getEnemyName(), false);

                    if (curEnemy instanceof RealTimeBoss boss) {
                        RiftWardenRenderer bossR = new RiftWardenRenderer();
                        bossR.setPhase(boss.getBossPhase() >= 2 ? RiftWarden.Phase.PHASE_II_AWAKENING : RiftWarden.Phase.PHASE_I_GUARDIAN);
                        bossR.render(g2, (int) boss.getX(), (int) boss.getY(), BOSS_DRAW_SIZE, eState,
                                boss.getStateProgress(), idleTime, boss.isFacingRight());
                    } else {
                        paintEnemyFigure(g2, curEnemy, eState, idleTime);
                    }

                    // Telegraph visual warning indicator
                    if (curEnemy.isTelegraphing() && curEnemy.isAlive()) {
                        paintTelegraphWarning(g2, curEnemy);
                    }

                    // Overhead floating health bar (for non-boss enemies)
                    if (curEnemy.isAlive() && !(curEnemy instanceof RealTimeBoss)) {
                        paintOverheadEnemyHp(g2, curEnemy);
                    }
                }

                // 5. Active Combat Effects (slashes, floating numbers)
                effectManager.render(g2);

                // 6. Ground mist and colour grade unify fighters with the environment
                paintFighterGroundBlend(g2, width, height, floorY);

                // 7. Foreground Architectural Vignette & Columns (Cinematic Depth)
                paintForegroundFraming(g2, width, height);

                // 7. Top HUD
                paintDungeonHUD(g2, width, player);

                // Boss Entrance Cinematic Title Banner
                if (dungeonRun != null && dungeonRun.getCurrentStage() == DungeonStage.BOSS_ENCOUNTER && dungeonRun.getStageTimer() < 3.2f) {
                    paintBossRevealCinematic(g2, width, height, dungeonRun.getStageTimer());
                }

                // 6. On-screen announcement banner
                if (dungeonRun != null && dungeonRun.isAnnouncementActive() &&
                        !(dungeonRun.getCurrentStage() == DungeonStage.BOSS_ENCOUNTER && dungeonRun.getStageTimer() < 3.2f)) {
                    paintAnnouncementBanner(g2, width, height, dungeonRun.getAnnouncementTitle(), dungeonRun.getAnnouncementSubtext());
                }

                // 7. Pause and Game Over Overlays
                if (rtEngine.isPaused()) {
                    paintPauseOverlay(g2, width, height);
                } else if (dungeonRun != null && dungeonRun.isGameOver()) {
                    paintDungeonGameOverOverlay(g2, width, height, dungeonRun);
                } else if (dungeonRun == null && rtEngine.isGameOver()) {
                    paintGameOverOverlay(g2, width, height, rtEngine.isVictory(), rtEngine.getMatchTime());
                }

                // 8. Floating in-game notification toasts
                paintToastNotifications(g2, width, height);

                // 9. Interactive in-arena combat log drawer
                if (showLogDrawer) {
                    paintLogDrawer(g2, width, height);
                }

            } else {
                // Fallback static presentation (milestone 5 backward-compatibility)
                paintFallbackFighters(g2, width, height, centreX, floorY, isBossAtmosphere);
            }

            g2.dispose();
        }

        private void paintToastNotifications(Graphics2D g2, int width, int height) {
            if (toastMessages.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) g2.create();
            g.setFont(Theme.SMALL_FONT);
            FontMetrics fm = g.getFontMetrics(Theme.SMALL_FONT);

            int startX = 36;
            int startY = height - 26;

            for (int i = toastMessages.size() - 1; i >= 0; i--) {
                if (i >= toastTimers.size()) {
                    continue;
                }
                String msg = toastMessages.get(i);
                float timer = toastTimers.get(i);
                float alpha = Math.min(1.0f, Math.max(0.0f, timer / 0.8f));
                int bgAlpha = (int) (185 * alpha);
                int borderAlpha = (int) (130 * alpha);
                int textAlpha = (int) (240 * alpha);

                int textW = fm.stringWidth(msg);
                int pillW = textW + 20;
                int pillH = 22;

                g.setColor(new Color(15, 12, 24, bgAlpha));
                g.fillRoundRect(startX, startY - pillH + 4, pillW, pillH, 8, 8);

                Color borderColor = msg.contains("CRITICAL") ? Theme.GOLD_BRIGHT :
                        (msg.contains("STAGE") ? Theme.GOLD :
                        (msg.contains("vanquished") ? new Color(0x57, 0xF2, 0x87) :
                        (msg.contains("strikes") || msg.contains("hits") ? Theme.TEXT_DIM : Theme.VIOLET)));
                g.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), borderAlpha));
                g.setStroke(new BasicStroke(1.0f));
                g.drawRoundRect(startX, startY - pillH + 4, pillW, pillH, 8, 8);

                g.setColor(new Color(Theme.TEXT.getRed(), Theme.TEXT.getGreen(), Theme.TEXT.getBlue(), textAlpha));
                g.drawString(msg, startX + 10, startY);

                startY -= (pillH + 6);
            }
            g.dispose();
        }

        private void paintLogDrawer(Graphics2D g2, int width, int height) {
            Graphics2D g = (Graphics2D) g2.create();
            int drawerW = Math.min(380, width - 80);
            int drawerH = height - 40;
            int dx = width - drawerW - 20;
            int dy = 20;

            // Translucent glass drawer body
            g.setColor(new Color(12, 10, 20, 235));
            g.fillRoundRect(dx, dy, drawerW, drawerH, 12, 12);

            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(dx, dy, drawerW, drawerH, 12, 12);

            // Title
            g.setFont(Theme.CARD_FONT);
            g.setColor(Theme.GOLD_BRIGHT);
            g.drawString("COMBAT ARCHIVE  [H / LOG]", dx + 18, dy + 28);

            g.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), 90));
            g.drawLine(dx + 18, dy + 38, dx + drawerW - 18, dy + 38);

            // Lines from logArea
            g.setFont(Theme.SMALL_FONT);
            g.setColor(Theme.TEXT);
            FontMetrics fm = g.getFontMetrics(Theme.SMALL_FONT);
            String fullText = logArea.getText();
            String[] lines = fullText.split("\n");

            int maxVisibleLines = (drawerH - 85) / 18;
            int startLine = Math.max(0, lines.length - maxVisibleLines);
            int lineY = dy + 58;

            for (int i = startLine; i < lines.length; i++) {
                String l = lines[i];
                if (fm.stringWidth(l) > drawerW - 36) {
                    l = l.substring(0, Math.min(l.length(), 42)) + "...";
                }
                if (l.contains("VICTORY") || l.contains("CLEARED")) {
                    g.setColor(Theme.GOLD_BRIGHT);
                } else if (l.contains("DEFEAT") || l.contains("PERISHED")) {
                    g.setColor(Theme.DANGER);
                } else if (l.contains("vanquished")) {
                    g.setColor(new Color(0x57, 0xF2, 0x87));
                } else if (l.contains("STAGE")) {
                    g.setColor(Theme.GOLD);
                } else {
                    g.setColor(Theme.TEXT);
                }
                g.drawString(l, dx + 18, lineY);
                lineY += 18;
            }

            g.setColor(Theme.TEXT_DIM);
            g.drawString("Press [H] or click [LOG] button to close drawer", dx + 18, dy + drawerH - 12);
            g.dispose();
        }

        private void paintGroundShockwave(Graphics2D g2, int px, int py, int rad) {
            Graphics2D g = (Graphics2D) g2.create();
            int waveW = 44;
            int waveH = 34;

            RadialGradientPaint waveGlow = new RadialGradientPaint(px, py, 45.0f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0xF0, 0x2B, 0x42), new Color(0x8A, 0x15, 0x38), new Color(0, 0, 0, 0)});
            g.setPaint(waveGlow);
            g.fillOval(px - waveW, py - waveH, waveW * 2, waveH * 2);

            g.setColor(Theme.DANGER);
            g.setStroke(new BasicStroke(3.5f));
            g.drawLine(px - 15, py + 12, px, py - waveH / 2);
            g.drawLine(px, py - waveH / 2, px + 15, py + 12);

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.8f));
            g.drawLine(px - 8, py + 8, px, py - waveH / 3);
            g.drawLine(px, py - waveH / 3, px + 8, py + 8);

            g.setColor(Theme.GOLD_BRIGHT);
            g.fillRect(px - 22, py + 14, 44, 3);
            g.dispose();
        }

        private void paintVoidProjectile(Graphics2D g2, int px, int py, int rad) {
            RadialGradientPaint glow = new RadialGradientPaint(px, py, rad * 2.4f,
                    new float[]{0f, 0.4f, 1f},
                    new Color[]{Theme.GOLD_BRIGHT, Theme.DANGER, new Color(0, 0, 0, 0)});
            g2.setPaint(glow);
            g2.fillOval(px - rad * 2, py - rad * 2, rad * 4, rad * 4);
            g2.setColor(new Color(0x8A, 0x15, 0x38));
            g2.fillOval(px - rad, py - rad, rad * 2, rad * 2);
            g2.setColor(Color.WHITE);
            g2.fillOval(px - rad / 3, py - rad / 3, Math.max(2, (rad * 2) / 3), Math.max(2, (rad * 2) / 3));
        }

        private void paintSpecialPowerBar(Graphics2D g2, int x, int y, int width, int height,
                                          float meter, boolean ready, float idleTime) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background channel
            g.setColor(new Color(15, 12, 25, 230));
            g.fillRoundRect(x, y, width, height, 4, 4);
            g.setColor(new Color(45, 38, 65));
            g.setStroke(new BasicStroke(1.0f));
            g.drawRoundRect(x, y, width, height, 4, 4);

            int fillW = (int) (width * Math.min(1.0f, Math.max(0.0f, meter / 100.0f)));
            if (fillW > 0) {
                if (ready) {
                    float pulse = (float) (Math.sin(idleTime * 6.0f) * 0.5 + 0.5);
                    Color col1 = new Color(0x00, 0xF5, 0xD4);
                    Color col2 = new Color(0xFF, 0xD7, 0x00);
                    GradientPaint grad = new GradientPaint(x, y, col1, x + fillW, y, col2);
                    g.setPaint(grad);
                    g.fillRoundRect(x + 1, y + 1, fillW - 2, height - 2, 3, 3);

                    int scanX = x + (int) ((idleTime * 120.0f) % (width + 40)) - 20;
                    if (scanX >= x && scanX < x + fillW - 8) {
                        g.setColor(new Color(255, 255, 255, 180));
                        g.fillRect(scanX, y + 1, 6, height - 2);
                    }

                    g.setColor(new Color(0x00, 0xF5, 0xD4, (int) (120 + pulse * 100)));
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRoundRect(x, y, width, height, 4, 4);
                } else {
                    GradientPaint grad = new GradientPaint(x, y, new Color(0x00, 0x80, 0x80),
                            x + fillW, y, new Color(0x00, 0xF5, 0xD4));
                    g.setPaint(grad);
                    g.fillRoundRect(x + 1, y + 1, fillW - 2, height - 2, 3, 3);
                }
            }

            g.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 9.5f));
            FontMetrics fm = g.getFontMetrics();
            String label = ready ? "SPECIAL POWER  ·  100% READY [K]" : String.format("SPECIAL POWER  ·  %d%%", (int) meter);
            int textX = x + (width - fm.stringWidth(label)) / 2;
            int textY = y + height - 2;

            g.setColor(new Color(0, 0, 0, 180));
            g.drawString(label, textX + 1, textY + 1);
            g.setColor(ready ? Color.WHITE : new Color(0xDF, 0xF0, 0xF8));
            g.drawString(label, textX, textY);

            g.dispose();
        }

        private void paintBossRevealCinematic(Graphics2D g2, int width, int height, float stageTimer) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float alpha;
            if (stageTimer < 0.5f) {
                alpha = stageTimer / 0.5f;
            } else if (stageTimer > 2.5f) {
                alpha = Math.max(0.0f, (3.2f - stageTimer) / 0.7f);
            } else {
                alpha = 1.0f;
            }

            int barH = (int) (height * 0.12f);
            // 1. Cinematic letterbox bars
            g.setColor(new Color(0, 0, 0, (int) (230 * alpha)));
            g.fillRect(0, 0, width, barH);
            g.fillRect(0, height - barH, width, barH);

            g.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), (int) (180 * alpha)));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(0, barH, width, barH);
            g.drawLine(0, height - barH, width, height - barH);

            // Center Dark Vignette Banner
            int bannerH = 92;
            int bannerY = (int) (height * 0.42f) - bannerH / 2;
            g.setColor(new Color(10, 6, 18, (int) (225 * alpha)));
            g.fillRect(0, bannerY, width, bannerH);

            g.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), (int) (180 * alpha)));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(0, bannerY, width, bannerY);
            g.drawLine(0, bannerY + bannerH, width, bannerY + bannerH);

            int cx = width / 2;
            int cy = bannerY + 36;

            g.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 12f));
            g.setColor(new Color(0xF0, 0x2B, 0x42, (int) (240 * alpha)));
            Theme.drawCenteredText(g, "— ANCIENT TITAN AWAKENED —", g.getFont(), g.getColor(), cx, cy - 14);

            Font titleFont = Theme.TITLE_FONT.deriveFont(Font.BOLD, 30f);
            g.setFont(titleFont);
            FontMetrics tfm = g.getFontMetrics(titleFont);
            int tw = tfm.stringWidth("MALAKOR, THE RIFT GOD");
            g.setColor(new Color(0, 0, 0, (int) (200 * alpha)));
            g.drawString("MALAKOR, THE RIFT GOD", cx - tw / 2 + 2, cy + 24);

            GradientPaint titleGrad = new GradientPaint(
                    cx - 180, cy, new Color(0xFF, 0xE5, 0x7F, (int) (255 * alpha)),
                    cx + 180, cy, new Color(0xF0, 0x2B, 0x42, (int) (255 * alpha)));
            g.setPaint(titleGrad);
            g.drawString("MALAKOR, THE RIFT GOD", cx - tw / 2, cy + 22);

            g.setFont(Theme.CARD_FONT.deriveFont(Font.BOLD, 14f));
            g.setColor(new Color(0xFF, 0xB7, 0x4D, (int) (240 * alpha)));
            Theme.drawCenteredText(g, "LORD OF THE CATACLYSM", g.getFont(), g.getColor(), cx, cy + 48);

            g.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), (int) (140 * alpha)));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(cx - 160, cy + 58, cx + 160, cy + 58);

            g.dispose();
        }

        /**
         * Draws any non-boss enemy from its own painted artwork, with the same ground
         * anchoring the champions use, so every enemy is fully inside the arena, stands
         * on the same floor and keeps a readable silhouette.
         */
        private void paintEnemyFigure(Graphics2D g2, RealTimeEnemy enemy, AnimationState state, float idleTime) {
            String key = enemyArtwork(enemy);
            float height = HERO_DRAW_SIZE * enemyHeightScale(enemy);
            java.awt.image.BufferedImage art = HeroSpriteLoader.getEnemySprite(key);
            if (art == null) {
                enemyRenderer.render(g2, (int) enemy.getX(), (int) enemy.getY(), HERO_DRAW_SIZE, state,
                        enemy.getStateProgress(), idleTime, enemy.isFacingRight());
                return;
            }
            FigureRenderer.Pose pose = FigureRenderer.poseFor(state, enemy.getStateProgress(), idleTime);
            float x = enemy.getX();
            float groundY = enemy.getY();
            FigureRenderer.shadow(g2, x, groundY, height * 0.62f,
                    state == AnimationState.JUMPING ? 0.5f : 1.0f);
            if (state == AnimationState.DASHING) {
                FigureRenderer.dashTrail(g2, art, x, groundY, height, enemy.isFacingRight(), pose);
            }
            FigureRenderer.draw(g2, art, x, groundY, height, enemy.isFacingRight(), pose);

            if (state == AnimationState.ATTACKING) {
                float strike = (float) Math.sin(enemy.getStateProgress() * Math.PI);
                float face = enemy.isFacingRight() ? 1f : -1f;
                Graphics2D arc = (Graphics2D) g2.create();
                arc.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Color tint = switch (enemy.getType()) {
                    case RIFT_CASTER -> new Color(0x9B, 0x59, 0xB6);
                    case RIFT_SKULKER -> new Color(0xE7, 0x4C, 0x3C);
                    default -> new Color(0xBD, 0x85, 0xF5);
                };
                arc.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), (int) (210 * strike)));
                int arcX = Math.round(x + face * height * 0.26f);
                int arcY = Math.round(groundY - height * 0.58f);
                int radius = Math.round(height * 0.20f);
                arc.drawArc(arcX - radius, arcY - radius, radius * 2, radius * 2,
                        enemy.isFacingRight() ? -55 : 125, 110);
                arc.dispose();
            }
            if (state == AnimationState.HURT) {
                FigureRenderer.hurtFlash(g2, x, groundY, height, 1f - enemy.getStateProgress());
            }
        }

        /** On-screen height of an enemy's artwork, used to place its overhead UI. */
        private float renderedEnemyHeight(RealTimeEnemy enemy) {
            if (enemy instanceof RealTimeBoss) {
                return BOSS_DRAW_SIZE / 180.0f * 1.45f * 190.0f;
            }
            return HERO_DRAW_SIZE * enemyHeightScale(enemy);
        }

        /** Artwork key for an enemy archetype. */
        private String enemyArtwork(RealTimeEnemy enemy) {
            String name = enemy.getEnemyName() == null ? "" : enemy.getEnemyName().toLowerCase();
            if (name.contains("iron") || name.contains("grunt")) {
                return "ironclad_grunt";
            }
            return switch (enemy.getType()) {
                case RIFT_SKULKER -> "void_skirmisher";
                case RIFT_CASTER -> "rift_cultist";
                default -> "rift_thrall";
            };
        }

        /** Enemy silhouettes stay a little shorter than the champions' full height. */
        private float enemyHeightScale(RealTimeEnemy enemy) {
            String name = enemy.getEnemyName() == null ? "" : enemy.getEnemyName().toLowerCase();
            if (name.contains("iron") || name.contains("grunt")) {
                return 0.95f;
            }
            return switch (enemy.getType()) {
                case RIFT_SKULKER -> 0.84f;
                case RIFT_CASTER -> 0.90f;
                default -> 0.88f;
            };
        }


        private void paintTelegraphWarning(Graphics2D g2, RealTimeEnemy enemy) {
            Graphics2D g = (Graphics2D) g2.create();
            int ex = (int) enemy.getX();
            int ey = (int) (enemy.getY() - renderedEnemyHeight(enemy) - 18);

            String warning = "\u26a0 ATTACK!";
            if (enemy instanceof RealTimeBoss boss) {
                String bossWarn = boss.getTelegraphWarning();
                if (bossWarn != null) {
                    warning = bossWarn;
                }
            } else if (enemy.getType() == EnemyType.RIFT_CASTER) {
                warning = "\u26a0 CHARGING VOID BOLT!";
            } else if (enemy.getType() == EnemyType.RIFT_SKULKER) {
                warning = "\u26a0 QUICK SLASH!";
            }

            g.setColor(new Color(0xF0, 0x2B, 0x42, 195));
            Theme.drawCenteredText(g, warning, Theme.SMALL_FONT, Theme.GOLD_BRIGHT, ex, ey);

            float dir = enemy.isFacingRight() ? 1.0f : -1.0f;
            g.setColor(new Color(240, 43, 66, 130));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{4.0f, 4.0f}, 0.0f));
            g.drawArc(ex + (int) (dir * 25) - 30, (int) enemy.getY() - 90, 60, 60,
                    enemy.isFacingRight() ? -45 : 135, 90);
            g.dispose();
        }

        private void paintOverheadEnemyHp(Graphics2D g2, RealTimeEnemy enemy) {
            int barW = 86;
            int barH = 7;
            int barX = (int) enemy.getX() - barW / 2;
            int barY = (int) (enemy.getY() - renderedEnemyHeight(enemy) - 14);

            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Drop shadow
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(barX + 1, barY + 1, barW, barH, 4, 4);

            // Container background
            g.setColor(new Color(0x18, 0x05, 0x0C, 220));
            g.fillRoundRect(barX, barY, barW, barH, 4, 4);

            double ratio = (double) enemy.getCurrentHp() / enemy.getMaxHp();
            int fillW = (int) Math.round(barW * Math.max(0.0, Math.min(1.0, ratio)));
            if (fillW > 0) {
                g.setPaint(new GradientPaint(barX, barY, new Color(0xFF, 0x2A, 0x48), barX, barY + barH, new Color(0x9E, 0x05, 0x1A)));
                g.fillRoundRect(barX, barY, fillW, barH, 4, 4);
            }

            g.setColor(new Color(0x5A, 0x14, 0x22));
            g.setStroke(new BasicStroke(1.0f));
            g.drawRoundRect(barX, barY, barW, barH, 4, 4);
            g.dispose();
        }

        private void paintAnnouncementBanner(Graphics2D g2, int width, int height, String title, String subtext) {
            Graphics2D g = (Graphics2D) g2.create();
            int bannerW = Math.min(680, width - 80);
            int bannerH = 64;
            int bx = (width - bannerW) / 2;
            int by = 90;

            g.setColor(new Color(10, 7, 18, 220));
            g.fillRoundRect(bx, by, bannerW, bannerH, 14, 14);

            g.setColor(Theme.GOLD_BRIGHT);
            g.setStroke(new BasicStroke(1.8f));
            g.drawRoundRect(bx, by, bannerW, bannerH, 14, 14);

            Theme.drawCenteredText(g, title, Theme.HEADING_FONT, Theme.GOLD_BRIGHT, width / 2, by + 26);
            if (subtext != null && !subtext.isEmpty()) {
                Theme.drawCenteredText(g, subtext, Theme.SMALL_FONT, Theme.TEXT, width / 2, by + 48);
            }
            g.dispose();
        }

        private void paintBossHealthBar(Graphics2D g2, int width, RealTimeBoss boss) {
            int barW = 280;
            int barH = 18;
            int bx = width - 36 - barW;
            int by = 34;

            int hp = boss.getCurrentHp();
            int maxHp = boss.getMaxHp();
            double ratio = maxHp > 0 ? (double) hp / maxHp : 0.0;

            String phaseTag = (boss.getBossPhase() >= 2) ? "PHASE II: CATACLYSM" : "PHASE I: GUARDIAN";
            String title = "THE RIFT WARDEN  ·  " + phaseTag;
            Font bossFont = Theme.SMALL_FONT.deriveFont(Font.BOLD, 13.0f);
            g2.setFont(bossFont);
            FontMetrics fm = g2.getFontMetrics(bossFont);
            g2.setColor(Theme.GOLD_BRIGHT);
            g2.drawString(title, width - 36 - fm.stringWidth(title), 26);

            String label = "HP  " + hp + " / " + maxHp;
            Color barColor = (boss.getBossPhase() >= 2) ? Theme.DANGER : Theme.VIOLET;
            paintHealthBar(g2, bx, by, barW, barH, ratio, label, barColor);

            String warning = boss.getTelegraphWarning();
            if (warning != null) {
                Theme.drawCenteredText(g2, warning, Theme.SMALL_FONT.deriveFont(Font.BOLD, 13.0f), Theme.GOLD_BRIGHT, bx + barW / 2, by + barH + 18);
            }
        }

        private void paintDungeonHUD(Graphics2D g2, int width, RealTimePlayer player) {
            // Soft atmospheric scrim: the arena art stays visible, the HUD stays legible.
            g2.setPaint(new GradientPaint(0, 0, new Color(5, 3, 10, 205), 0, 112, new Color(5, 3, 10, 0)));
            g2.fillRect(0, 0, width, 112);

            int portX = 24;
            int portY = 16;
            int portSize = 68;

            String displayHero = switch (heroName.toLowerCase()) {
                case "warrior" -> "AEGIS";
                case "assassin" -> "NYX";
                case "mage" -> "ZEPHYRA";
                default -> heroName.toUpperCase();
            };
            String roleTitle = switch (heroName.toLowerCase()) {
                case "warrior", "aegis" -> "HEAVY VANGUARD";
                case "assassin", "nyx" -> "SHADOW DUELIST";
                case "mage", "zephyra" -> "ARCANE WEAVER";
                case "kael" -> "SPELLSWORD DUELIST";
                case "vexara" -> "NETHER REAPER";
                case "draven" -> "VOLCANIC BRAWLER";
                default -> player.getHeroClass().toUpperCase();
            };

            // 1. LEFT: Hero Portrait & Live Stats
            java.awt.image.BufferedImage heroPortrait = HeroSpriteLoader.getHeroPortrait(heroName);
            drawPortraitFrame(g2, portX, portY, portSize, heroPortrait, true);

            int textX = portX + portSize + 14;
            g2.setFont(Theme.CARD_FONT.deriveFont(Font.BOLD, 17.0f));
            g2.setColor(Theme.GOLD_BRIGHT);
            g2.drawString(displayHero, textX, portY + 18);

            g2.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.5f));
            g2.setColor(new Color(0xB8, 0xAA, 0xD4));
            g2.drawString(roleTitle, textX, portY + 31);

            int barW = 240;
            int hpBarY = portY + 37;
            int pHp = player.getCurrentHp();
            int pMaxHp = player.getMaxHp();
            double pRatio = pMaxHp > 0 ? (double) pHp / pMaxHp : 0.0;
            String hpLabel = "HP  " + pHp + " / " + pMaxHp;
            paintBeveledGauge(g2, textX, hpBarY, barW, 16, pRatio, hpLabel,
                    new Color(0xFF, 0x2A, 0x48), new Color(0x9E, 0x05, 0x1A), new Color(0x22, 0x06, 0x0D));

            int spBarY = hpBarY + 20;
            float spVal = player.getSpecialMeter();
            double spRatio = Math.max(0.0, Math.min(1.0, spVal / 100.0));
            String spLabel = "SP  " + (int) spVal + " / 100";
            paintBeveledGauge(g2, textX, spBarY, barW, 13, spRatio, spLabel,
                    new Color(0x00, 0xD4, 0xFF), new Color(0x00, 0x55, 0xBB), new Color(0x06, 0x10, 0x25));

            int diaY = spBarY + 18;
            paintSpecialDiamonds(g2, textX + 4, diaY, spVal, player.isSpecialReady(), idleTime);

            // 2. CENTER: Gilded Encounter / Stage Banner
            String bannerTag = "ENCOUNTER 1";
            String bannerTitle = "VOID PATROL";
            String bannerSubtext = "Defeat the 2 approaching void beasts!";

            if (dungeonRun != null) {
                switch (dungeonRun.getCurrentStage()) {
                    case ENTRANCE -> {
                        bannerTag = "DUNGEON ENTRANCE";
                        bannerTitle = "THE RIFT ABYSS";
                        bannerSubtext = "Breach the outer perimeter and purge the void!";
                    }
                    case ENCOUNTER_ONE -> {
                        bannerTag = "ENCOUNTER 1";
                        bannerTitle = "VOID PATROL";
                        int rem = dungeonRun.getEnemiesRemainingInCurrentWave();
                        bannerSubtext = "Defeat the " + rem + " approaching void beasts!";
                    }
                    case ENCOUNTER_ONE_CLEARED -> {
                        bannerTag = "ENCOUNTER 1 SECURED";
                        bannerTitle = "CORRIDOR CLEARED";
                        bannerSubtext = "Advancing deeper into the catacombs...";
                    }
                    case ENCOUNTER_TWO -> {
                        bannerTag = "ENCOUNTER 2";
                        bannerTitle = "INNER SANCTUM";
                        int rem = dungeonRun.getEnemiesRemainingInCurrentWave();
                        bannerSubtext = "Cleanse the " + rem + " remaining rift cultists & grunts!";
                    }
                    case BOSS_GATE -> {
                        bannerTag = "GATE OF MALAKOR";
                        bannerTitle = "THE ANCIENT SEAL";
                        bannerSubtext = "The cataclysm awakens... Malakor approaches!";
                    }
                    case BOSS_ENCOUNTER -> {
                        RealTimeBoss boss = dungeonRun.getBoss();
                        bannerTag = "BOSS BATTLE  ·  PHASE " + (boss != null ? boss.getBossPhase() : 1);
                        bannerTitle = "MALAKOR, THE RIFT GOD";
                        bannerSubtext = "Survive the cataclysm and conquer the abyss!";
                    }
                    case VICTORY -> {
                        bannerTag = "VICTORY";
                        bannerTitle = "DUNGEON CONQUERED";
                        bannerSubtext = "The dimensional rift has been sealed!";
                    }
                    case DEFEAT -> {
                        bannerTag = "DEFEAT";
                        bannerTitle = "FALLEN IN THE ABYSS";
                        bannerSubtext = "Darkness consumes the chamber...";
                    }
                }
            } else if (engine != null) {
                if (engine.getEnemy() instanceof RiftWarden) {
                    bannerTag = "BOSS CHALLENGE";
                    bannerTitle = "THE RIFT WARDEN";
                    bannerSubtext = "Survive the cataclysmic multi-phase titan battle!";
                } else {
                    bannerTag = "TRIAL OF COMBAT";
                    bannerTitle = engine.getEnemy().getName().toUpperCase();
                    bannerSubtext = "Conquer the challenger in single combat!";
                }
            }

            int bannerW = 380;
            int bannerH = 68;
            int bannerX = (width - bannerW) / 2;
            paintGildedEncounterBanner(g2, bannerX, portY, bannerW, bannerH, bannerTag, bannerTitle, bannerSubtext);

            // 3. RIGHT: Primary Enemy / Boss HUD
            RealTimeEnemy targetEnemy = null;
            if (dungeonRun != null && dungeonRun.isBossActive()) {
                targetEnemy = dungeonRun.getBoss();
            } else if (rtEngine != null) {
                targetEnemy = rtEngine.getEnemy();
            }

            if (targetEnemy != null) {
                int ePortSize = 68;
                int ePortX = width - 24 - ePortSize;
                int ePortY = 16;

                java.awt.image.BufferedImage enemyArt = HeroSpriteLoader.getEnemySprite(targetEnemy.getEnemyName());
                drawPortraitFrame(g2, ePortX, ePortY, ePortSize, enemyArt, false);

                int eTextRight = ePortX - 14;
                String eName = targetEnemy.getEnemyName().toUpperCase();
                String eRole = (targetEnemy instanceof RealTimeBoss) ? "ANCIENT TITAN" :
                        switch (targetEnemy.getType()) {
                            case RIFT_SKULKER -> "VOID SKIRMISHER";
                            case RIFT_CASTER -> "RIFT CULTIST";
                            case RIFT_SPAWN -> "RIFT SPAWN";
                            default -> "RIFT SPAWN";
                        };

                FontMetrics fm = g2.getFontMetrics(Theme.CARD_FONT.deriveFont(Font.BOLD, 17.0f));
                g2.setFont(Theme.CARD_FONT.deriveFont(Font.BOLD, 17.0f));
                g2.setColor(Theme.GOLD_BRIGHT);
                g2.drawString(eName, eTextRight - fm.stringWidth(eName), ePortY + 18);

                FontMetrics fmRole = g2.getFontMetrics(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.5f));
                g2.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.5f));
                g2.setColor(new Color(0xD4, 0x8A, 0x9A));
                g2.drawString(eRole, eTextRight - fmRole.stringWidth(eRole), ePortY + 31);

                int eBarW = 240;
                int eBarX = eTextRight - eBarW;
                int eHp = targetEnemy.getCurrentHp();
                int eMaxHp = targetEnemy.getMaxHp();
                double eRatio = eMaxHp > 0 ? (double) eHp / eMaxHp : 0.0;
                String eHpLabel = "HP  " + eHp + " / " + eMaxHp;
                paintBeveledGauge(g2, eBarX, hpBarY, eBarW, 16, eRatio, eHpLabel,
                        new Color(0xFF, 0x2A, 0x48), new Color(0x9E, 0x05, 0x1A), new Color(0x22, 0x06, 0x0D));
            }
        }

        private Path2D.Float createOctagon(int x, int y, int w, int h, int cut) {
            Path2D.Float p = new Path2D.Float();
            p.moveTo(x + cut, y);
            p.lineTo(x + w - cut, y);
            p.lineTo(x + w, y + cut);
            p.lineTo(x + w, y + h - cut);
            p.lineTo(x + w - cut, y + h);
            p.lineTo(x + cut, y + h);
            p.lineTo(x, y + h - cut);
            p.lineTo(x, y + cut);
            p.closePath();
            return p;
        }

        private void drawPortraitFrame(Graphics2D g2, int x, int y, int size, java.awt.image.BufferedImage art, boolean isHero) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Path2D.Float oct = createOctagon(x, y, size, size, 12);

            // Drop shadow
            g.setColor(new Color(0, 0, 0, 180));
            g.fill(createOctagon(x + 2, y + 3, size, size, 12));

            // Background
            g.setColor(new Color(0x10, 0x08, 0x1A));
            g.fill(oct);

            // Character artwork clipped to octagon focusing on upper chest and face
            if (art != null) {
                Graphics2D gClip = (Graphics2D) g.create();
                gClip.clip(oct);
                int imgW = art.getWidth();
                int imgH = art.getHeight();
                float scale = (float) size / (imgH * 0.36f);
                int drawW = (int) (imgW * scale);
                int drawH = (int) (imgH * scale);
                int drawX = x + (size - drawW) / 2;
                int drawY = y - (int) (imgH * 0.03f * scale);
                gClip.drawImage(art, drawX, drawY, drawW, drawH, null);
                gClip.dispose();
            }

            // Gilded/Iron outer beveled rim
            Color border1 = isHero ? new Color(0xD4, 0xAF, 0x37) : new Color(0x9E, 0x30, 0x50);
            Color border2 = isHero ? new Color(0x8A, 0x68, 0x18) : new Color(0x5A, 0x18, 0x30);
            g.setPaint(new GradientPaint(x, y, border1, x, y + size, border2));
            g.setStroke(new BasicStroke(2.2f));
            g.draw(oct);

            // Inner highlight hairline
            Path2D.Float innerOct = createOctagon(x + 3, y + 3, size - 6, size - 6, 10);
            g.setColor(isHero ? new Color(0xFF, 0xF2, 0xC2, 110) : new Color(0xFF, 0x88, 0xAA, 100));
            g.setStroke(new BasicStroke(1.0f));
            g.draw(innerOct);

            // 4 Corner golden rivets
            g.setColor(isHero ? new Color(0xFF, 0xE5, 0x99) : new Color(0xFF, 0xAA, 0xCC));
            int[][] rivets = {{x + 12, y + 4}, {x + size - 12, y + 4}, {x + 12, y + size - 4}, {x + size - 12, y + size - 4}};
            for (int[] r : rivets) {
                g.fillOval(r[0] - 2, r[1] - 2, 4, 4);
            }

            g.dispose();
        }

        private void paintBeveledGauge(Graphics2D g2, int x, int y, int w, int h, double ratio, String label,
                                      Color gradStart, Color gradEnd, Color bgColor) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(bgColor);
            g.fillRoundRect(x, y, w, h, 6, 6);

            int fillW = (int) Math.round(w * Math.max(0.0, Math.min(1.0, ratio)));
            if (fillW > 0) {
                g.setPaint(new GradientPaint(x, y, gradStart, x, y + h, gradEnd));
                g.fillRoundRect(x, y, fillW, h, 6, 6);

                g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 110), x, y + (h / 2), new Color(255, 255, 255, 0)));
                g.fillRoundRect(x, y, fillW, h / 2, 6, 6);
            }

            g.setColor(new Color(0x3B, 0x2A, 0x48, 200));
            g.setStroke(new BasicStroke(1.0f));
            g.drawRoundRect(x, y, w, h, 6, 6);

            g.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.0f));
            FontMetrics fm = g.getFontMetrics();
            int strW = fm.stringWidth(label);
            int strX = x + (w - strW) / 2;
            int strY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();

            g.setColor(new Color(0, 0, 0, 220));
            g.drawString(label, strX + 1, strY + 1);
            g.setColor(Color.WHITE);
            g.drawString(label, strX, strY);

            g.dispose();
        }

        private void paintSpecialDiamonds(Graphics2D g2, int x, int y, float meter, boolean isReady, float time) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int dSize = 13;
            int spacing = 20;

            for (int i = 0; i < 3; i++) {
                int cx = x + i * spacing + dSize / 2;
                int cy = y + dSize / 2;
                boolean charged = isReady || (meter >= (i + 1) * 33.0f);

                Polygon dia = new Polygon();
                dia.addPoint(cx, cy - dSize / 2);
                dia.addPoint(cx + dSize / 2, cy);
                dia.addPoint(cx, cy + dSize / 2);
                dia.addPoint(cx - dSize / 2, cy);

                if (charged) {
                    float pulse = 0.8f + 0.2f * (float) Math.sin(time * 5.0f + i);
                    RadialGradientPaint rgp = new RadialGradientPaint(
                            cx, cy, dSize * 1.5f,
                            new float[]{0f, 0.5f, 1f},
                            new Color[]{
                                    new Color(0xE0, 0x66, 0xFF, (int) (160 * pulse)),
                                    new Color(0x99, 0x33, 0xEE, (int) (60 * pulse)),
                                    new Color(0, 0, 0, 0)
                            }
                    );
                    g.setPaint(rgp);
                    g.fillOval(cx - (int)(dSize * 1.5f), cy - (int)(dSize * 1.5f), (int)(dSize * 3.0f), (int)(dSize * 3.0f));

                    g.setPaint(new GradientPaint(cx - dSize / 2, cy - dSize / 2, new Color(0xFF, 0x88, 0xFF),
                            cx + dSize / 2, cy + dSize / 2, new Color(0x8A, 0x18, 0xDE)));
                    g.fill(dia);
                    g.setColor(new Color(0xFF, 0xEE, 0xFF));
                    g.setStroke(new BasicStroke(1.2f));
                    g.draw(dia);
                } else {
                    g.setColor(new Color(0x18, 0x0C, 0x24, 190));
                    g.fill(dia);
                    g.setColor(new Color(0x48, 0x32, 0x60));
                    g.setStroke(new BasicStroke(1.0f));
                    g.draw(dia);
                }
            }
            g.dispose();
        }

        private void paintGildedEncounterBanner(Graphics2D g2, int x, int y, int w, int h, String tag, String title, String subtext) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Path2D.Float banner = new Path2D.Float();
            int notch = 12;
            banner.moveTo(x + notch, y);
            banner.lineTo(x + w - notch, y);
            banner.lineTo(x + w, y + notch);
            banner.lineTo(x + w, y + h - notch);
            banner.lineTo(x + w - notch, y + h);
            banner.lineTo(x + notch, y + h);
            banner.lineTo(x, y + h - notch);
            banner.lineTo(x, y + notch);
            banner.closePath();

            // Translucent dark glass fill
            g.setPaint(new GradientPaint(x, y, new Color(0x14, 0x0A, 0x22, 230), x, y + h, new Color(0x0A, 0x04, 0x12, 245)));
            g.fill(banner);

            // Gilded border
            g.setColor(new Color(0xD4, 0xAF, 0x37, 210));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(banner);

            // Subtle inner glow hairline
            Path2D.Float innerBanner = new Path2D.Float();
            innerBanner.moveTo(x + notch + 2, y + 2);
            innerBanner.lineTo(x + w - notch - 2, y + 2);
            innerBanner.lineTo(x + w - 2, y + notch + 2);
            innerBanner.lineTo(x + w - 2, y + h - notch - 2);
            innerBanner.lineTo(x + w - notch - 2, y + h - 2);
            innerBanner.lineTo(x + notch + 2, y + h - 2);
            innerBanner.lineTo(x + 2, y + h - notch - 2);
            innerBanner.lineTo(x + 2, y + notch + 2);
            innerBanner.closePath();
            g.setColor(new Color(0xFF, 0xF2, 0xC2, 70));
            g.setStroke(new BasicStroke(1.0f));
            g.draw(innerBanner);

            // End diamond ornaments
            int cy = y + h / 2;
            Polygon leftDia = new Polygon();
            leftDia.addPoint(x - 5, cy);
            leftDia.addPoint(x, cy - 4);
            leftDia.addPoint(x + 5, cy);
            leftDia.addPoint(x, cy + 4);
            g.setColor(new Color(0xD4, 0xAF, 0x37));
            g.fill(leftDia);

            Polygon rightDia = new Polygon();
            rightDia.addPoint(x + w - 5, cy);
            rightDia.addPoint(x + w, cy - 4);
            rightDia.addPoint(x + w + 5, cy);
            rightDia.addPoint(x + w, cy + 4);
            g.fill(rightDia);

            int cx = x + w / 2;

            // 1. Tag
            g.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD, 10.5f));
            FontMetrics fmTag = g.getFontMetrics();
            g.setColor(new Color(0xD4, 0xAF, 0x37));
            g.drawString(tag, cx - fmTag.stringWidth(tag) / 2, y + 17);

            // 2. Title
            g.setFont(Theme.CARD_FONT.deriveFont(Font.BOLD, 17.5f));
            FontMetrics fmTitle = g.getFontMetrics();
            g.setColor(Theme.GOLD_BRIGHT);
            g.drawString(title, cx - fmTitle.stringWidth(title) / 2, y + 38);

            // 3. Subtext
            g.setFont(Theme.SMALL_FONT.deriveFont(Font.PLAIN, 11.0f));
            FontMetrics fmSub = g.getFontMetrics();
            g.setColor(new Color(0xCB, 0xC0, 0xDC));
            g.drawString(subtext, cx - fmSub.stringWidth(subtext) / 2, y + 56);

            g.dispose();
        }

        private void paintDungeonGameOverOverlay(Graphics2D g2, int width, int height, DungeonRun run) {
            g2.setColor(new Color(0, 0, 0, 215));
            g2.fillRect(0, 0, width, height);

            int centreX = width / 2;
            int centreY = height / 2;

            if (run.isVictory()) {
                Theme.drawCenteredText(g2, "DUNGEON CLEARED!", Theme.HEADING_FONT, Theme.GOLD_BRIGHT, centreX, centreY - 60);
                Theme.drawCenteredText(g2, run.getPerformanceRank(), Theme.CARD_FONT, Theme.GOLD, centreX, centreY - 20);
                String details = "Enemies Vanquished: " + run.getTotalEnemiesDefeated() + "  \u00b7  Total Time: "
                        + String.format("%.1fs", run.getTotalElapsedTime());
                Theme.drawCenteredText(g2, details, Theme.BODY_FONT, Theme.TEXT, centreX, centreY + 15);
            } else {
                Theme.drawCenteredText(g2, "YOU PERISHED IN THE DEPTHS", Theme.HEADING_FONT, Theme.DANGER, centreX, centreY - 60);
                Theme.drawCenteredText(g2, "Fell in Encounter: " + run.getCurrentStage().getTitle(), Theme.CARD_FONT, Theme.TEXT, centreX, centreY - 20);
                String details = "Enemies Vanquished: " + run.getTotalEnemiesDefeated() + "  \u00b7  Time Survived: "
                        + String.format("%.1fs", run.getTotalElapsedTime());
                Theme.drawCenteredText(g2, details, Theme.BODY_FONT, Theme.TEXT_DIM, centreX, centreY + 15);
            }

            Theme.drawCenteredText(g2, "Press [R] to Replay Dungeon  \u00b7  Click [VIEW RESULTS] below",
                    Theme.SMALL_FONT, Theme.GOLD, centreX, centreY + 60);
        }

        private static AnimationState mapEntityStateToAnimation(EntityState state, String name, boolean isHero) {
            if (state == null) {
                return AnimationState.IDLE;
            }
            return switch (state) {
                case RUNNING -> AnimationState.RUNNING;
                case JUMPING, FALLING -> AnimationState.JUMPING;
                case DASHING -> AnimationState.DASHING;
                case ATTACKING -> "Mage".equalsIgnoreCase(name) ? AnimationState.CASTING : AnimationState.ATTACKING;
                case SPECIAL -> "Mage".equalsIgnoreCase(name) ? AnimationState.CASTING : AnimationState.ATTACKING;
                case HURT -> AnimationState.HURT;
                case DEFEATED -> AnimationState.DEFEAT;
                default -> AnimationState.IDLE;
            };
        }

        private void paintRealTimeHUD(Graphics2D g2, int width, RealTimePlayer player, RealTimeEnemy enemy) {
            // Player HUD (Left)
            int pHp = player.getCurrentHp();
            int pMaxHp = player.getMaxHp();
            double pRatio = pMaxHp > 0 ? (double) pHp / pMaxHp : 0.0;
            String pLabel = "HP  " + pHp + " / " + pMaxHp;

            g2.setFont(Theme.CARD_FONT);
            g2.setColor(Theme.TEXT);
            String displayHero = switch (heroName.toLowerCase()) {
                case "warrior" -> "AEGIS";
                case "assassin" -> "NYX";
                case "mage" -> "ZEPHYRA";
                default -> heroName.toUpperCase();
            };
            String roleTitle = switch (heroName.toLowerCase()) {
                case "warrior", "aegis" -> "HEAVY VANGUARD";
                case "assassin", "nyx" -> "SHADOW DUELIST";
                case "mage", "zephyra" -> "ARCANE WEAVER";
                case "kael" -> "SPELLSWORD DUELIST";
                case "vexara" -> "NETHER REAPER";
                case "draven" -> "VOLCANIC BRAWLER";
                default -> player.getHeroClass().toUpperCase();
            };
            g2.drawString(displayHero + "  ·  " + roleTitle, 36, 26);
            paintHealthBar(g2, 36, 34, 280, 22, pRatio, pLabel, heroAccent);

            // Sleek Action Ability Badges / Cooldown Chips
            int chipY = 66;
            int chipH = 22;

            // 1. [J] STRIKE
            paintAbilityChip(g2, 36, chipY, 88, chipH, "J  STRIKE", 0f, 0f, Theme.GOLD_BRIGHT, true);

            // 2. [K] SPECIAL
            float spMax = player.getHeroClass().equalsIgnoreCase("Mage") ? 2.5f : (player.getHeroClass().equalsIgnoreCase("Assassin") ? 2.2f : 3.0f);
            float spCur = player.getSpecialCooldownTimer();
            boolean spReady = spCur <= 0f;
            String spText = spReady ? "K  SPECIAL" : String.format("K  %.1fs", spCur);
            paintAbilityChip(g2, 130, chipY, 96, chipH, spText, spCur, spMax, Theme.GOLD_BRIGHT, spReady);

            // 3. [SHIFT] DASH
            float dashMax = 1.0f;
            float dashCur = player.getDashCooldownTimer();
            boolean dashReady = dashCur <= 0f;
            String dashText = dashReady ? "SHIFT  DASH" : String.format("DASH  %.1fs", dashCur);
            paintAbilityChip(g2, 232, chipY, 102, chipH, dashText, dashCur, dashMax, new Color(0x57, 0xF2, 0x87), dashReady);

            // Center Match Timer
            String timeStr = String.format("TIME: %.1fs", rtEngine.getMatchTime());
            Theme.drawCenteredText(g2, timeStr, Theme.CARD_FONT, Theme.GOLD, width / 2, 36);

            // Enemy HUD (Right)
            int eHp = enemy.getCurrentHp();
            int eMaxHp = enemy.getMaxHp();
            double eRatio = eMaxHp > 0 ? (double) eHp / eMaxHp : 0.0;
            String eLabel = "HP  " + eHp + " / " + eMaxHp;
            int enemyBarX = width - 316;

            g2.setFont(Theme.CARD_FONT);
            g2.setColor(Theme.TEXT);
            g2.drawString(enemy.getEnemyName().toUpperCase(), enemyBarX, 26);
            paintHealthBar(g2, enemyBarX, 34, 280, 22, eRatio, eLabel, Theme.VIOLET);
        }

        private void paintPauseOverlay(Graphics2D g2, int width, int height) {
            g2.setColor(new Color(0, 0, 0, 195));
            g2.fillRect(0, 0, width, height);

            int centreX = width / 2;
            int centreY = height / 2;

            Theme.drawCenteredText(g2, "GAME PAUSED", Theme.HEADING_FONT, Theme.GOLD_BRIGHT, centreX, centreY - 60);
            Theme.drawCenteredText(g2, "Press [ESC] to Resume  \u00b7  Press [R] to Restart Encounter",
                    Theme.BODY_FONT, Theme.TEXT, centreX, centreY - 15);

            // Controls summary box
            int boxW = 420;
            int boxH = 100;
            int boxX = centreX - boxW / 2;
            int boxY = centreY + 15;
            g2.setColor(Theme.PANEL_DEEP);
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
            g2.setColor(Theme.GOLD_DIM);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

            g2.setFont(Theme.SMALL_FONT);
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString("A / D: Move Left / Right      W / Space: Jump", boxX + 35, boxY + 32);
            g2.drawString("J: Basic Attack               K: Special Attack", boxX + 35, boxY + 56);
            g2.drawString("Shift: Evade Dash             ESC: Pause / Resume", boxX + 35, boxY + 80);
        }

        private void paintGameOverOverlay(Graphics2D g2, int width, int height, boolean victory, float matchTime) {
            g2.setColor(new Color(0, 0, 0, 205));
            g2.fillRect(0, 0, width, height);

            int centreX = width / 2;
            int centreY = height / 2;

            if (victory) {
                Theme.drawCenteredText(g2, "VICTORY ACHIEVED", Theme.HEADING_FONT, Theme.GOLD_BRIGHT, centreX, centreY - 40);
                Theme.drawCenteredText(g2, "The anomaly was destroyed in " + String.format("%.1f", matchTime) + " seconds!",
                        Theme.BODY_FONT, Theme.TEXT, centreX, centreY + 5);
            } else {
                Theme.drawCenteredText(g2, "HERO DEFEATED", Theme.HEADING_FONT, Theme.DANGER, centreX, centreY - 40);
                Theme.drawCenteredText(g2, "You fell before the dimensional breach on round time " + String.format("%.1f", matchTime) + "s",
                        Theme.BODY_FONT, Theme.TEXT, centreX, centreY + 5);
            }

            Theme.drawCenteredText(g2, "Press [R] to Restart Encounter  \u00b7  Click [VIEW RESULTS] below",
                    Theme.SMALL_FONT, Theme.GOLD, centreX, centreY + 45);
        }

        private void paintFallbackFighters(Graphics2D g2, int width, int height, int centreX, int floorY, boolean isBoss) {
            int heroX = (int) (width * 0.28);
            int enemyX = (int) (width * 0.72);
            int fighterY = (int) (height * 0.44);

            paintSummoningCircle(g2, centreX, (int) (height * 0.58), fighterY);
            paintDais(g2, heroX, fighterY + 95, 85, 22, heroAccent);
            paintDais(g2, enemyX, fighterY + 95, isBoss ? 110 : 85, isBoss ? 28 : 22, Theme.VIOLET);

            if (heroRenderer != null) {
                heroRenderer.render(g2, heroX, fighterY, 240, heroState, heroProgress, idleTime, true);
            }
            if (enemyRenderer != null) {
                enemyRenderer.render(g2, enemyX, fighterY, 240, enemyState, enemyProgress, idleTime, false);
            }

            effectManager.render(g2);
        }

        /**
         * Draws the clean, environment-only arena artwork, cropped to fill the play area
         * without stretching it, then adds live torchlight and rift glow on top.
         * The scaled backdrop is cached so the 60 FPS repaint stays cheap.
         */
        private void paintAtmosphericArenaBackground(Graphics2D g2, int width, int height, int floorY, int centreX, boolean isCataclysm) {
            if (cachedBg == null || cachedW != width || cachedH != height) {
                cachedBg = buildScaledBackdrop(width, height);
                cachedW = width;
                cachedH = height;
            }
            if (cachedBg != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(cachedBg, 0, 0, null);

                // Rift light spills down onto the fighting floor.
                float riftPulse = (float) (0.85f + 0.15f * Math.sin(idleTime * 1.6f));
                RadialGradientPaint riftPool = new RadialGradientPaint(
                        width * 0.5f, height * 0.80f, width * 0.30f,
                        new float[]{0f, 0.55f, 1f},
                        new Color[]{
                                new Color(0x8A, 0x52, 0xD0, (int) (26 * riftPulse)),
                                new Color(0x4A, 0x22, 0x84, 12),
                                new Color(0, 0, 0, 0)
                        });
                g2.setPaint(riftPool);
                g2.fillRect(0, 0, width, height);

                // Real flames flicker on the two braziers painted into the arena.
                float torchFlicker = (float) (0.82f + 0.18f * Math.sin(idleTime * 8.0f) * Math.cos(idleTime * 3.7f));
                for (int i = 0; i < 2; i++) {
                    float tx = width * (i == 0 ? 0.103f : 0.897f);
                    float ty = height * 0.665f;
                    RadialGradientPaint torch = new RadialGradientPaint(
                            tx, ty, Math.max(50f, width * 0.085f),
                            new float[]{0f, 0.45f, 1f},
                            new Color[]{
                                    new Color(0xFF, 0xA8, 0x40, (int) (70 * torchFlicker)),
                                    new Color(0xD0, 0x4A, 0x08, (int) (26 * torchFlicker)),
                                    new Color(0, 0, 0, 0)
                            });
                    g2.setPaint(torch);
                    g2.fillRect(0, 0, width, height);
                }

                // Boss phase: the abyss answers the Rift Warden's presence.
                if (isCataclysm) {
                    float pulse = (float) (0.5f + 0.5f * Math.sin(idleTime * 2.5f));
                    RadialGradientPaint cataclysmWash = new RadialGradientPaint(
                            centreX, (int) (height * 0.30f), (int) (width * 0.62f),
                            new float[]{0f, 0.5f, 1f},
                            new Color[]{
                                    new Color(0x8E, 0x12, 0x2E, (int) (32 + 26 * pulse)),
                                    new Color(0x36, 0x06, 0x18, 26),
                                    new Color(0, 0, 0, 0)
                            });
                    g2.setPaint(cataclysmWash);
                    g2.fillRect(0, 0, width, height);
                }
                return;
            }

            // Emergency fallback: a clean Java2D vault, never a broken screenshot.
            g2.setPaint(new GradientPaint(0, 0, new Color(0x0A, 0x06, 0x12), 0, height, new Color(0x1A, 0x10, 0x26)));
            g2.fillRect(0, 0, width, height);
            g2.setColor(new Color(0x2C, 0x20, 0x40));
            g2.fillRect(0, floorY, width, height - floorY);
        }

        /** Scales the backdrop to cover the panel, preserving its aspect ratio. */
        private java.awt.image.BufferedImage buildScaledBackdrop(int width, int height) {
            java.awt.image.BufferedImage art = HeroSpriteLoader.load("assets/arena/dungeon_arena_background.png");
            if (art == null) {
                art = HeroSpriteLoader.load("assets/arena/rift_arena_clean.png");
            }
            if (art == null) {
                return null;
            }
            int w = Math.max(1, width);
            int h = Math.max(1, height);
            double scale = Math.max((double) w / art.getWidth(), (double) h / art.getHeight());
            int drawW = Math.max(w, (int) Math.round(art.getWidth() * scale));
            int drawH = Math.max(h, (int) Math.round(art.getHeight() * scale));

            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = out.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            bg.drawImage(art, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
            bg.dispose();
            return out;
        }

        private void paintDriftingHorizonMist(Graphics2D g2, int width, int floorY) {
            Graphics2D g = (Graphics2D) g2.create();
            for (int i = 0; i < 4; i++) {
                float offset = (float) Math.sin(idleTime * 0.8f + i * 1.6f) * 8f;
                int mx = (int) ((idleTime * 20f + i * (width / 4)) % (width + 200)) - 100;
                int my = floorY - 14 + (int) offset;
                RadialGradientPaint mist = new RadialGradientPaint(mx, my, 130f,
                        new float[]{0f, 0.7f, 1f},
                        new Color[]{new Color(0x6D, 0x44, 0xC4, 28), new Color(0x3B, 0x1E, 0x68, 12), new Color(0, 0, 0, 0)});
                g.setPaint(mist);
                g.fillOval(mx - 130, my - 20, 260, 40);
            }
            g.dispose();
        }

        private void paintGroundShadowAndLight(Graphics2D g2, int x, int y, int radius, Color accent, float glowAlpha) {
            Graphics2D g = (Graphics2D) g2.create();
            int poolRad = (int) (radius * 1.4f);
            int poolH   = (int) (radius * 0.42f);
            RadialGradientPaint pool = new RadialGradientPaint(x, y, poolRad,
                    new float[]{0f, 0.65f, 1f},
                    new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * glowAlpha)),
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (80 * glowAlpha)),
                            new Color(0, 0, 0, 0)});
            g.setPaint(pool);
            g.fillOval(x - poolRad, y - poolH / 2, poolRad * 2, poolH);
            g.dispose();
        }

        private void paintBossGroundAura(Graphics2D g2, int x, int y, int phase, float idleTime) {
            Graphics2D g = (Graphics2D) g2.create();
            Color auraColor = (phase >= 2) ? Theme.DANGER : Theme.GOLD_BRIGHT;
            int radius = 190;

            RadialGradientPaint aura = new RadialGradientPaint(x, y, radius,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(auraColor.getRed(), auraColor.getGreen(), auraColor.getBlue(), 60),
                            new Color(auraColor.getRed(), auraColor.getGreen(), auraColor.getBlue(), 18),
                            new Color(0, 0, 0, 0)});
            g.setPaint(aura);
            g.fillOval(x - radius, y - 38, radius * 2, 76);
            g.dispose();
        }

        /**
         * A thin band of violet floor mist and a very light colour grade. It passes in
         * front of the fighters' feet, so characters sit in the arena instead of on top
         * of a flat backdrop, and keeps the whole scene in the same palette.
         */
        private void paintFighterGroundBlend(Graphics2D g2, int width, int height, int floorY) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setColor(new Color(0x14, 0x0A, 0x20, 16));
            g.fillRect(0, 0, width, height);

            int top = Math.max(0, floorY - 52);
            int bottom = Math.min(height, floorY + 54);
            if (bottom > top) {
                g.setPaint(new GradientPaint(0, top, new Color(0x6A, 0x46, 0xA8, 0),
                        0, floorY, new Color(0x8C, 0x62, 0xC8, 26)));
                g.fillRect(0, top, width, floorY - top);
                g.setPaint(new GradientPaint(0, floorY, new Color(0x6A, 0x46, 0xA8, 20),
                        0, bottom, new Color(0x6A, 0x46, 0xA8, 0)));
                g.fillRect(0, floorY, width, bottom - floorY);
            }
            g.dispose();
        }

        private void paintForegroundFraming(Graphics2D g2, int width, int height) {
            Graphics2D g = (Graphics2D) g2.create();
            // Subtle corner vignettes
            RadialGradientPaint tl = new RadialGradientPaint(0, 0, 180f,
                    new float[]{0f, 1f}, new Color[]{new Color(0, 0, 0, 160), new Color(0, 0, 0, 0)});
            g.setPaint(tl);
            g.fillRect(0, 0, 180, 180);

            RadialGradientPaint tr = new RadialGradientPaint(width, 0, 180f,
                    new float[]{0f, 1f}, new Color[]{new Color(0, 0, 0, 160), new Color(0, 0, 0, 0)});
            g.setPaint(tr);
            g.fillRect(width - 180, 0, 180, 180);
            g.dispose();
        }

        private void paintSummoningCircle(Graphics2D g2, int centreX, int circleY, int fighterY) {
            // Arcane floor summoning circle
            g2.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), 40));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(centreX - 90, circleY - 28, 180, 56);
            g2.drawOval(centreX - 65, circleY - 20, 130, 40);

            // Arcane cross rays
            g2.drawLine(centreX - 90, circleY, centreX + 90, circleY);
            g2.drawLine(centreX, circleY - 28, centreX, circleY + 28);

            // Floating VS emblem
            Theme.drawRuneCircle(g2, centreX, fighterY, 32,
                    new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), 60));
            Theme.drawCenteredText(g2, "VS", Theme.HEADING_FONT, Theme.GOLD, centreX, fighterY + 10);
        }

        private void paintDais(Graphics2D g2, int centerX, int daisY, int radiusX, int radiusY, Color accent) {
            // Stepped stone pedestal
            int baseDepth = 14;

            // Pedestal base cylinder
            g2.setPaint(new GradientPaint(centerX - radiusX, daisY, new Color(0x11, 0x0d, 0x1a),
                    centerX + radiusX, daisY + baseDepth, new Color(0x1e, 0x17, 0x2d)));
            g2.fill(new java.awt.geom.Ellipse2D.Double(centerX - radiusX, daisY + baseDepth, radiusX * 2, radiusY * 2));
            g2.fillRect(centerX - radiusX, daisY + radiusY, radiusX * 2, baseDepth);

            // Pedestal top dais
            g2.setPaint(new GradientPaint(centerX, daisY - radiusY, new Color(0x28, 0x21, 0x3b),
                    centerX, daisY + radiusY, new Color(0x16, 0x11, 0x22)));
            g2.fill(new java.awt.geom.Ellipse2D.Double(centerX - radiusX, daisY, radiusX * 2, radiusY * 2));

            // Gold/runic rim
            g2.setColor(new Color(Theme.GOLD_DIM.getRed(), Theme.GOLD_DIM.getGreen(), Theme.GOLD_DIM.getBlue(), 160));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new java.awt.geom.Ellipse2D.Double(centerX - radiusX, daisY, radiusX * 2, radiusY * 2));

            // Soft radial glow on top of the dais
            RadialGradientPaint daisGlow = new RadialGradientPaint(centerX, daisY + radiusY, radiusX,
                    new float[]{0f, 1f},
                    new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 75), new Color(0, 0, 0, 0)});
            g2.setPaint(daisGlow);
            g2.fill(new java.awt.geom.Ellipse2D.Double(centerX - radiusX + 8, daisY + 2, (radiusX - 8) * 2, (radiusY - 2) * 2));
        }

        private void paintFloatingEmbers(Graphics2D g2, int width, int height) {
            for (int i = 0; i < 18; i++) {
                float speed = 25f + (i * 13 % 35);
                float emberY = height - ((idleTime * speed + i * 47) % height);
                float emberX = (width * 0.15f) + ((i * 179 + 53) % (width * 0.70f));
                float wobble = (float) Math.sin(idleTime * 2.5f + i) * 12f;

                int alpha = (int) (170 * (1f - (emberY / height)));
                alpha = Math.max(0, Math.min(255, alpha));

                Color emberCol = (i % 3 == 0) ? Theme.GOLD_BRIGHT : (i % 3 == 1 ? Theme.VIOLET.brighter() : new Color(0x57, 0xF2, 0x87));
                g2.setColor(new Color(emberCol.getRed(), emberCol.getGreen(), emberCol.getBlue(), alpha));
                int sz = (i % 4 == 0) ? 3 : 2;
                g2.fillOval((int) (emberX + wobble), (int) emberY, sz, sz);
            }
        }

        private void paintHealthBar(Graphics2D g2, int x, int y, int barWidth, int barHeight,
                                    double ratio, String label, Color accent) {
            int arc = 10;
            g2.setColor(Theme.PANEL_DEEP);
            g2.fill(new java.awt.geom.RoundRectangle2D.Double(x, y, barWidth, barHeight, arc, arc));

            int fillWidth = (int) Math.round((barWidth - 6) * Math.max(0.0, Math.min(1.0, ratio)));
            g2.setPaint(new GradientPaint(x, y, accent, x, y + barHeight, accent.darker()));
            g2.fill(new java.awt.geom.RoundRectangle2D.Double(x + 3, y + 3, fillWidth,
                    barHeight - 6, arc - 3, arc - 3));

            g2.setColor(new Color(Theme.GOLD_DIM.getRed(), Theme.GOLD_DIM.getGreen(),
                    Theme.GOLD_DIM.getBlue(), 200));
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new java.awt.geom.RoundRectangle2D.Double(x, y, barWidth, barHeight, arc, arc));

            g2.setFont(Theme.SMALL_FONT);
            FontMetrics metrics = g2.getFontMetrics(Theme.SMALL_FONT);
            g2.setColor(Theme.SHADOW);
            g2.drawString(label, x + barWidth / 2 - metrics.stringWidth(label) / 2 + 1,
                    y + barHeight / 2 + metrics.getAscent() / 2 - 1);
            g2.setColor(Theme.TEXT);
            g2.drawString(label, x + barWidth / 2 - metrics.stringWidth(label) / 2,
                    y + barHeight / 2 + metrics.getAscent() / 2 - 2);
        }

        private void paintAbilityChip(Graphics2D g2, int x, int y, int w, int h, String text,
                                      float currentCd, float maxCd, Color readyColor, boolean isReady) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 8;
            if (isReady) {
                // Background dark glass
                g.setColor(new Color(18, 14, 28, 220));
                g.fillRoundRect(x, y, w, h, arc, arc);

                // Subtle inner glow
                g.setColor(new Color(readyColor.getRed(), readyColor.getGreen(), readyColor.getBlue(), 35));
                g.fillRoundRect(x + 1, y + 1, w - 2, h - 2, arc - 2, arc - 2);

                // Border
                g.setColor(new Color(readyColor.getRed(), readyColor.getGreen(), readyColor.getBlue(), 190));
                g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(x, y, w, h, arc, arc);

                // Text
                g.setFont(Theme.SMALL_FONT);
                FontMetrics fm = g.getFontMetrics(Theme.SMALL_FONT);
                int tx = x + (w - fm.stringWidth(text)) / 2;
                int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
                g.setColor(Theme.TEXT);
                g.drawString(text, tx, ty);
            } else {
                // Cooldown: dark backing
                g.setColor(new Color(10, 8, 16, 220));
                g.fillRoundRect(x, y, w, h, arc, arc);

                // Cooldown meter fill
                float ratio = (maxCd > 0f) ? Math.max(0f, Math.min(1f, 1f - (currentCd / maxCd))) : 0f;
                int fillW = (int) ((w - 2) * ratio);
                if (fillW > 0) {
                    g.setColor(new Color(readyColor.getRed(), readyColor.getGreen(), readyColor.getBlue(), 45));
                    g.fillRoundRect(x + 1, y + 1, fillW, h - 2, arc - 2, arc - 2);
                }

                g.setColor(new Color(60, 50, 75, 180));
                g.setStroke(new BasicStroke(1.0f));
                g.drawRoundRect(x, y, w, h, arc, arc);

                g.setFont(Theme.SMALL_FONT);
                FontMetrics fm = g.getFontMetrics(Theme.SMALL_FONT);
                int tx = x + (w - fm.stringWidth(text)) / 2;
                int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
                g.setColor(Theme.TEXT_DIM);
                g.drawString(text, tx, ty);
            }
            g.dispose();
        }
    }
}
