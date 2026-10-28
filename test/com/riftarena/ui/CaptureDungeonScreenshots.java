package com.riftarena.ui;

import com.riftarena.game.Assassin;
import com.riftarena.game.Draven;
import com.riftarena.game.Enemy;
import com.riftarena.game.Kael;
import com.riftarena.game.Mage;
import com.riftarena.game.Vexara;
import com.riftarena.game.Warrior;
import com.riftarena.realtime.DungeonRun;
import com.riftarena.realtime.DungeonStage;
import com.riftarena.realtime.EnemyType;
import com.riftarena.realtime.EntityState;
import com.riftarena.realtime.RealTimeBoss;
import com.riftarena.realtime.RealTimeEnemy;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility capturing high-resolution offscreen PNG screenshots directly from the
 * running {@link CombatPanel} and {@link DungeonRun}.
 */
public class CaptureDungeonScreenshots {

    private static final String ARTIFACT_DIR =
            "/Users/p.srilalithreddy/.gemini/antigravity/brain/b489deef-e391-4811-8880-ff2847668f34/screenshots";
    private static final String ROOT_DIR = "screenshots";

    public static void main(String[] args) throws Exception {
        new File(ARTIFACT_DIR).mkdirs();
        new File(ROOT_DIR).mkdirs();

        captureMainMenu();
        captureHeroSelection();
        captureEntrance();
        captureEncounterOne();
        captureEncounterTwo();
        captureBossGate();
        captureBossEncounter();
        captureBossCinematicEntrance();
        captureDungeonVictory();
        captureDungeonDefeat();
        captureResultScreen();
        captureWarriorAttack();
        captureMageCasting();
        captureAssassinAttack();
        captureKaelCombat();
        captureVexaraCombat();
        captureDravenCombat();
        captureRiftWardenPhase3();

        System.out.println("ALL SCREENSHOTS GENERATED SUCCESSFULLY.");
        System.exit(0);
    }

    private static CombatPanel createCombatPanel() {
        CombatPanel panel = new CombatPanel(null);
        panel.setSize(new Dimension(1280, 800));
        panel.doLayout();
        CombatPanel.ArenaView arena = panel.getArenaView();
        arena.setSize(new Dimension(1280, 732));
        arena.doLayout();
        return panel;
    }

    private static BufferedImage renderPanel(CombatPanel panel, int width, int height) {
        panel.setSize(new Dimension(width, height));
        layoutComponent(panel);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();
        return image;
    }

    private static void saveImage(BufferedImage img, String filename) throws Exception {
        File rootFile = new File(ROOT_DIR, filename);
        File artifactFile = new File(ARTIFACT_DIR, filename);
        ImageIO.write(img, "PNG", rootFile);
        Files.copy(rootFile.toPath(), artifactFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Saved: " + rootFile.getAbsolutePath() + " and " + artifactFile.getAbsolutePath());
    }

    private static void captureMainMenu() throws Exception {
        MenuPanel panel = new MenuPanel(null);
        panel.setSize(new Dimension(1280, 800));
        layoutComponent(panel);

        BufferedImage image = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        saveImage(image, "main_menu.png");
    }

    private static void layoutComponent(java.awt.Component comp) {
        comp.doLayout();
        if (comp instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layoutComponent(child);
            }
        }
    }

    private static void captureHeroSelection() throws Exception {
        HeroSelectPanel panel = new HeroSelectPanel(null);
        panel.setSize(new Dimension(1280, 800));
        layoutComponent(panel);

        BufferedImage image = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        saveImage(image, "hero_selection.png");
    }

    private static void captureEntrance() throws Exception {
        CombatPanel panel = createCombatPanel();
        panel.startDungeon(new Warrior());
        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "dungeon_entrance.png");
    }

    private static void captureEncounterOne() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        // Advance into Encounter 1
        run.update(2.2f);
        panel.getArenaView().stepAnimation(0.25f);

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(320);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);

        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(680);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(980);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().getEffectManager().spawnSlash(680, gy - 60, Theme.GOLD_BRIGHT, 55, false);
        panel.getArenaView().getEffectManager().spawnHitSparks(680, gy - 60, Theme.GOLD_BRIGHT, 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(680, gy - 110, 24, false);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "encounter_one.png");
    }

    private static void captureEncounterTwo() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        // Fast forward to Encounter 2
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f);
        run.update(1.9f); // now in ENCOUNTER_TWO

        panel.getArenaView().stepAnimation(0.35f);
        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(280);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);

        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(580);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(800);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
            if (run.getCombatEngine().getEnemies().size() > 2) {
                RealTimeEnemy foe3 = run.getCombatEngine().getEnemies().get(2);
                foe3.setX(1020);
                foe3.setY(gy);
                foe3.setFacingRight(false);
            }
        }

        // Caster attack effect
        panel.getArenaView().getEffectManager().spawnSpellBurst(480, gy - 60, Theme.HERO_MAGE);
        panel.getArenaView().getEffectManager().spawnHitSparks(480, gy - 60, Theme.HERO_MAGE, -1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(480, gy - 110, 18, false);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "encounter_two.png");
    }

    private static void captureBossGate() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f);
        run.update(1.9f);
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f); // Transition to BOSS_GATE

        panel.getArenaView().stepAnimation(0.2f);
        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "boss_gate.png");
    }

    private static void captureBossEncounter() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.startBossDirectly();
        run.update(3.5f); // past the reveal banner

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(300);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        if (run.getBoss() != null) {
            run.getBoss().setX(820);
            run.getBoss().setY(gy);
            run.getBoss().setFacingRight(false);
        }
        panel.getArenaView().stepAnimation(0.45f);
        panel.getArenaView().getEffectManager().spawnSlash(820, gy - 100, Theme.VIOLET, 105, true);
        panel.getArenaView().getEffectManager().spawnHitSparks(820, gy - 100, Theme.GOLD_BRIGHT, -1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(820, gy - 160, 45, true);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "boss_encounter.png");
    }

    private static void captureBossCinematicEntrance() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.startBossDirectly();

        panel.getArenaView().getEffectManager().clearAll();
        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(240);
        run.getPlayer().setY(gy);
        run.getPlayer().restoreFullHp();
        run.getPlayer().transitionTo(EntityState.IDLE);
        if (run.getBoss() != null) {
            run.getBoss().setX(740);
            run.getBoss().setY(gy);
            run.getBoss().transitionTo(EntityState.IDLE);
            run.getBoss().restoreFullHp();
        }
        run.setStageTimer(1.4f);
        panel.getArenaView().stepAnimation(0.25f);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "boss_cinematic_entrance.png");
    }

    private static void captureDungeonVictory() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f);
        run.update(1.9f);
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f);
        run.update(2.3f);
        run.getBoss().takeDamage(999, 1, 0);
        run.update(0.05f);

        panel.getArenaView().stepAnimation(0.5f);
        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "dungeon_victory.png");
    }

    private static void captureDungeonDefeat() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly
        run.getPlayer().takeDamage(999, 0, 0);
        run.update(0.05f);

        panel.getArenaView().stepAnimation(0.5f);
        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "dungeon_defeat.png");
    }

    private static void captureResultScreen() throws Exception {
        MainFrame frame = new MainFrame();
        frame.setSize(1024, 680);
        ResultPanel result = frame.getResultPanel();
        result.setOutcome(true, 48, 6, "S (TITAN SLAYER)");
        result.setSize(new Dimension(1024, 680));
        layoutComponent(result);

        BufferedImage img = new BufferedImage(1024, 680, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        result.paint(g2);
        g2.dispose();

        saveImage(img, "dungeon_result.png");
    }

    private static void captureWarriorAttack() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(330);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        run.getPlayer().transitionTo(EntityState.ATTACKING);
        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(520);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            foe1.takeDamage(24, 1, 0);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(680);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().stepAnimation(0.18f);
        panel.getArenaView().getEffectManager().spawnSlash(520, gy - 60, Theme.GOLD_BRIGHT, 70, false);
        panel.getArenaView().getEffectManager().spawnHitSparks(520, gy - 60, Theme.GOLD_BRIGHT, 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(520, gy - 110, 24, false);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "warrior_attack.png");
    }

    private static void captureMageCasting() throws Exception {
        CombatPanel panel = createCombatPanel();
        Mage mage = new Mage();
        panel.startDungeon(mage);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(330);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        run.getPlayer().transitionTo(EntityState.ATTACKING);
        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(520);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            foe1.takeDamage(40, 1, 0);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(680);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().stepAnimation(0.20f);
        panel.getArenaView().getEffectManager().spawnSpellBurst(520, gy - 60, Theme.HERO_MAGE);
        panel.getArenaView().getEffectManager().spawnHitSparks(520, gy - 60, Theme.HERO_MAGE, 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(520, gy - 110, 40, true);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "mage_casting.png");
    }

    private static void captureAssassinAttack() throws Exception {
        CombatPanel panel = createCombatPanel();
        Assassin assassin = new Assassin();
        panel.startDungeon(assassin);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(330);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        run.getPlayer().transitionTo(EntityState.ATTACKING);
        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(520);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            foe1.takeDamage(32, 1, 0);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(680);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().stepAnimation(0.14f);
        panel.getArenaView().getEffectManager().spawnSlash(520, gy - 60, Theme.HERO_ASSASSIN, 60, false);
        panel.getArenaView().getEffectManager().spawnSlash(530, gy - 70, new Color(0x57, 0xF2, 0x87), 55, true);
        panel.getArenaView().getEffectManager().spawnHitSparks(520, gy - 60, Theme.HERO_ASSASSIN, 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(520, gy - 110, 32, false);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "assassin_attack.png");
    }

    private static void captureKaelCombat() throws Exception {
        CombatPanel panel = createCombatPanel();
        Kael kael = new Kael();
        panel.startDungeon(kael);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(320);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        run.getPlayer().transitionTo(EntityState.ATTACKING);
        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(690);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            foe1.takeDamage(36, 1, 0);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(980);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().stepAnimation(0.18f);
        panel.getArenaView().getEffectManager().spawnSlash(690, gy - 60, new Color(0xEF, 0x53, 0x50), 75, false);
        panel.getArenaView().getEffectManager().spawnHitSparks(690, gy - 60, new Color(0xFF, 0x8A, 0x80), 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(690, gy - 110, 36, true);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "kael_combat.png");
    }

    private static void captureVexaraCombat() throws Exception {
        CombatPanel panel = createCombatPanel();
        Vexara vexara = new Vexara();
        panel.startDungeon(vexara);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(330);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        run.getPlayer().transitionTo(EntityState.SPECIAL);
        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(520);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            foe1.takeDamage(35, 1, 0);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(680);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().stepAnimation(0.24f);
        panel.getArenaView().getEffectManager().spawnSlash(520, gy - 60, new Color(0xBA, 0x68, 0xC8), 85, true);
        panel.getArenaView().getEffectManager().spawnHitSparks(520, gy - 60, new Color(0xE1, 0xBE, 0xE7), 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(520, gy - 110, 35, true);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "vexara_combat.png");
    }

    private static void captureDravenCombat() throws Exception {
        CombatPanel panel = createCombatPanel();
        Draven draven = new Draven();
        panel.startDungeon(draven);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(330);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        run.getPlayer().transitionTo(EntityState.SPECIAL);
        if (!run.getCombatEngine().getEnemies().isEmpty()) {
            RealTimeEnemy foe1 = run.getCombatEngine().getEnemies().get(0);
            foe1.setX(520);
            foe1.setY(gy);
            foe1.setFacingRight(false);
            foe1.takeDamage(42, 1, 0);
            if (run.getCombatEngine().getEnemies().size() > 1) {
                RealTimeEnemy foe2 = run.getCombatEngine().getEnemies().get(1);
                foe2.setX(680);
                foe2.setY(gy);
                foe2.setFacingRight(false);
            }
        }
        panel.getArenaView().stepAnimation(0.22f);
        panel.getArenaView().getEffectManager().spawnSlash(520, gy - 60, new Color(0xFF, 0x6D, 0x00), 95, true);
        panel.getArenaView().getEffectManager().spawnHitSparks(520, gy - 60, new Color(0xFF, 0xAB, 0x40), 1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(520, gy - 110, 42, true);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "draven_combat.png");
    }

    private static void captureRiftWardenPhase3() throws Exception {
        CombatPanel panel = createCombatPanel();
        Warrior warrior = new Warrior();
        panel.startDungeon(warrior);
        DungeonRun run = panel.getDungeonRun();
        run.update(4.7f); // entrance (2.0s) + room announcement (2.5s) passed cleanly
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f);
        run.update(1.9f);
        for (RealTimeEnemy e : run.getCombatEngine().getEnemies()) e.takeDamage(999, 1, 0);
        run.update(0.04f);
        run.update(3.6f);
        run.setStageTimer(3.5f);

        int gy = (int) run.getCombatEngine().getArena().getGroundY();
        run.getPlayer().setX(280);
        run.getPlayer().setY(gy);
        run.getPlayer().setFacingRight(true);
        RealTimeBoss boss = run.getBoss();
        if (boss != null) {
            boss.setX(640);
            boss.setY(gy);
            boss.setFacingRight(false);
            boss.triggerPhaseTwo();
        }
        panel.getArenaView().stepAnimation(0.2f);
        panel.getArenaView().getEffectManager().spawnSlash(640, gy - 70, Theme.DANGER, 80, true);
        panel.getArenaView().getEffectManager().spawnHitSparks(640, gy - 70, Theme.DANGER, -1.0f);
        panel.getArenaView().getEffectManager().spawnFloatingDamage(640, gy - 120, 52, true);

        panel.getArenaView().clearToasts();
        BufferedImage img = renderPanel(panel, 1280, 800);
        saveImage(img, "rift_warden_phase3.png");
    }
}
