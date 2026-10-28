package com.riftarena.ui;

import com.riftarena.game.Assassin;
import com.riftarena.game.CombatEngine;
import com.riftarena.game.Enemy;
import com.riftarena.game.Hero;
import com.riftarena.game.Mage;
import com.riftarena.game.RiftWarden;
import com.riftarena.game.Warrior;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.Objects;

/**
 * The single application window.
 *
 * MainFrame holds every screen inside one JPanel managed by a CardLayout.
 * A CardLayout is a Swing layout manager that shows exactly one of its
 * child panels at a time — like a deck of cards where only the top card
 * is visible. Navigation is explicit: call showScreen(...) and the frame
 * brings the matching panel to the front.
 *
 * Later milestones only need to extend this navigation API (for example
 * startBattle already prepares the combat screen with a hero name); the
 * panels themselves never need to know about each other.
 *
 * Syllabus concepts demonstrated here:
 *   - Unit II:  classes, constructors, composition (the frame owns panels)
 *   - Unit IV:  inheritance (extends JFrame)
 */
public class MainFrame extends JFrame {

    /** Application title used consistently for the window and dialogs. */
    public static final String TITLE = "Dungeon Clash: Rift Arena";

    /** Every screen the game can display. Adding a screen = adding a value here. */
    public enum Screen { MENU, HERO_SELECT, COMBAT, RESULT }

    /** Available combat encounter types. */
    public enum EncounterType {
        SKIRMISH("Arena Skirmish", "Rift Spawn (Standard)"),
        BOSS("Boss Encounter", "The Rift Warden (3 Phases)");

        private final String title;
        private final String subtitle;

        EncounterType(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }
    }

    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 800;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);

    private final HeroSelectPanel heroSelectPanel;
    private final CombatPanel combatPanel;
    private final ResultPanel resultPanel;

    private EncounterType encounterType = EncounterType.SKIRMISH;
    private Screen currentScreen = Screen.MENU;

    public MainFrame() {
        super(TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        MenuPanel menuPanel = new MenuPanel(this);
        heroSelectPanel = new HeroSelectPanel(this);
        combatPanel = new CombatPanel(this);
        resultPanel = new ResultPanel(this);

        screens.add(menuPanel, Screen.MENU.name());
        screens.add(heroSelectPanel, Screen.HERO_SELECT.name());
        screens.add(combatPanel, Screen.COMBAT.name());
        screens.add(resultPanel, Screen.RESULT.name());
        setContentPane(screens);

        setMinimumSize(new Dimension(1024, 680));
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setLocationRelativeTo(null);

        // Always boot into the main menu.
        showScreen(Screen.MENU);

        // Open maximized where the window manager supports it; the size
        // set above remains as the sensible fallback.
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    // ------------------------------------------------------------------
    // Explicit navigation API — the only way screens change
    // ------------------------------------------------------------------

    /** Brings the requested screen to the front. */
    public void showScreen(Screen screen) {
        this.currentScreen = Objects.requireNonNull(screen, "Screen cannot be null");
        cardLayout.show(screens, screen.name());
    }

    /** Returns the currently displayed screen card. */
    public Screen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Instantiates a fresh Hero and Enemy model and launches a new combat encounter.
     * Guaranteed to produce fresh character state with full HP, reset cooldowns, and turn 1.
     *
     * @param heroName the selected champion name ("Warrior", "Mage", or "Assassin")
     */
    public void startBattle(String heroName) {
        startBattle(createHero(heroName));
    }

    /**
     * Prepares and starts a battle with the specified Hero according to active encounter type.
     */
    public void startBattle(Hero hero) {
        Enemy enemy = (encounterType == EncounterType.BOSS)
                ? new RiftWarden()
                : new Enemy("Rift Spawn", 110, 16, 16, Enemy.Behaviour.TACTICAL);
        startBattle(hero, enemy);
    }

    /**
     * Starts a battle specifically configured as a Boss encounter against The Rift Warden.
     */
    public void startBossBattle(String heroName) {
        setEncounterType(EncounterType.BOSS);
        startBattle(heroName);
    }

    public EncounterType getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(EncounterType encounterType) {
        this.encounterType = Objects.requireNonNull(encounterType);
        if (heroSelectPanel != null) {
            heroSelectPanel.updateEncounterHeader();
        }
    }

    public HeroSelectPanel getHeroSelectPanel() {
        return heroSelectPanel;
    }

    /**
     * Starts a battle with custom hero and enemy combatants.
     */
    public void startBattle(Hero hero, Enemy enemy) {
        CombatEngine engine = new CombatEngine(hero, enemy);
        combatPanel.startBattle(engine);
        showScreen(Screen.COMBAT);
    }

    /**
     * Factory helper mapping hero names from UI selection to concrete Hero instances.
     */
    public static Hero createHero(String heroName) {
        if (heroName == null) {
            return new Warrior();
        }
        return switch (heroName.trim().toLowerCase()) {
            case "mage", "zephyra" -> new Mage();
            case "assassin", "nyx" -> new Assassin();
            case "kael" -> new com.riftarena.game.Kael();
            case "vexara" -> new com.riftarena.game.Vexara();
            case "draven" -> new com.riftarena.game.Draven();
            default -> new Warrior(); // warrior, aegis
        };
    }

    /**
     * Prepares and starts a full dungeon run with the specified Hero.
     */
    public void startDungeon(String heroName) {
        startBattle(heroName);
    }

    /**
     * Prepares and starts a full dungeon run with the specified Hero instance.
     */
    public void startDungeon(Hero hero) {
        startBattle(hero);
    }

    /** Shows the reusable result screen configured as victory or defeat. */
    public void showResult(boolean victory) {
        showResult(victory, 0);
    }

    /** Shows the result screen with live combat statistics. */
    public void showResult(boolean victory, int turns) {
        resultPanel.setOutcome(victory, turns);
        showScreen(Screen.RESULT);
    }

    /** Shows the result screen with authoritative dungeon run statistics. */
    public void showResult(boolean victory, int elapsedSeconds, int enemiesDefeated, String rank) {
        resultPanel.setOutcome(victory, elapsedSeconds, enemiesDefeated, rank);
        showScreen(Screen.RESULT);
    }

    public CombatPanel getCombatPanel() {
        return combatPanel;
    }

    public ResultPanel getResultPanel() {
        return resultPanel;
    }

    /** Used by the Quit button: closes the window and ends the JVM cleanly. */
    public void quitGame() {
        dispose();
        System.exit(0);
    }
}
