package com.riftarena.realtime;

import com.riftarena.game.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Authoritative controller for dungeon progression and encounter management.
 *
 * Coordinates:
 *   - Linear progression through connected combat encounters (Entrance -> Wave 1 -> Wave 2 -> Boss Gate -> Boss -> Victory).
 *   - Spawning of enemy combinations with distinct tactical roles (Melee, Fast Stalker, Ranged Caster, Multi-phase Boss).
 *   - Gated transitions ensuring rooms cannot advance prematurely.
 *   - Real-time announcements, kill metrics, and run timers.
 *   - Clean full restart restoring full player health, reset stages, and clean arena state.
 */
public class DungeonRun {

    public interface DungeonEventListener {
        void onStageChanged(DungeonStage previousStage, DungeonStage newStage);
        void onAnnouncement(String title, String subtext, float duration);
        void onEnemyKilled(RealTimeEnemy enemy, int totalKills);
        void onDungeonConcluded(boolean victory, float elapsedTime, int totalKills);
    }

    private final Hero hero;
    private final RealTimeCombatEngine engine;
    private final List<DungeonEventListener> listeners = new ArrayList<>();

    private DungeonStage currentStage = DungeonStage.ENTRANCE;
    private float stageTimer = 0.0f;
    private float totalElapsedTime = 0.0f;
    private int totalEnemiesDefeated = 0;

    private String announcementTitle = "";
    private String announcementSubtext = "";
    private float announcementTimer = 0.0f;

    // Stage duration constants for transitions
    private static final float ENTRANCE_DURATION = 2.0f;
    private static final float CLEAR_ONE_DURATION = 1.8f;
    private static final float BOSS_GATE_DURATION = 2.2f;

    public DungeonRun(Hero hero) {
        this(hero, new RealTimeCombatEngine(hero, null));
    }

    public DungeonRun(Hero hero, RealTimeCombatEngine engine) {
        this.hero = Objects.requireNonNull(hero, "Hero cannot be null");
        this.engine = Objects.requireNonNull(engine, "Combat engine cannot be null");

        // Hook engine feedback for kill tracking and player defeat
        this.engine.addFeedbackListener(new RealTimeCombatEngine.CombatFeedbackListener() {
            @Override
            public void onDamageDealt(float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
            }

            @Override
            public void onAttackPerformed(String attacker, boolean isSpecial) {
            }

            @Override
            public void onGameOver(boolean victory) {
                // If player is dead, immediately trigger dungeon defeat
                if (!engine.getPlayer().isAlive() && currentStage != DungeonStage.DEFEAT) {
                    DungeonStage fallenStage = currentStage;
                    transitionToStage(DungeonStage.DEFEAT);
                    setAnnouncement("FALLEN IN THE DEPTHS", "Encounter: " + fallenStage.getTitle(), 5.0f);
                    notifyDungeonConcluded(false);
                }
            }

            @Override
            public void onEnemyDefeated(RealTimeEnemy enemy) {
                totalEnemiesDefeated++;
                notifyEnemyKilled(enemy);
            }

            @Override
            public void onBossPhaseTransition(RealTimeBoss boss, int newPhase) {
                setAnnouncement("TITAN AWAKENS: CATACLYSM", "The Rift Warden enters Phase II with abyssal shockwaves!", 3.0f);
            }
        });

        initDungeon();
    }

    private void initDungeon() {
        this.currentStage = DungeonStage.ENTRANCE;
        this.stageTimer = 0.0f;
        this.totalElapsedTime = 0.0f;
        this.totalEnemiesDefeated = 0;
        this.engine.setDungeonMode(true);
        this.engine.setPaused(false);
        this.engine.setGameOver(false, false);
        this.engine.setEnemies(new ArrayList<>());
        this.engine.getProjectiles().clear();
        this.engine.getInputState().reset();

        // Restore player position, cooldowns, and full health
        engine.getPlayer().resetCombatState();
        float groundY = engine.getArena().getGroundY();
        engine.getPlayer().setX(engine.getArena().getWidth() * 0.20f);
        engine.getPlayer().setY(groundY);

        setAnnouncement("DUNGEON ENTERED: " + currentStage.getTitle(), currentStage.getDescription(), ENTRANCE_DURATION);
    }

    /**
     * Advances authoritative dungeon progression and active combat simulation.
     */
    public void update(float rawDt) {
        if (engine.getInputState().isRestartRequested()) {
            engine.getInputState().consumeRestart();
            restart();
            return;
        }

        if (engine.isPaused()) {
            engine.update(rawDt);
            return;
        }

        if (isGameOver()) {
            engine.update(rawDt);
            return;
        }

        // Sub-step large delta times for stable physics and precise stage triggers
        float remaining = Math.max(0, rawDt);
        while (remaining > 0) {
            float dt = Math.min(remaining, 0.04f);
            step(dt);
            remaining -= dt;
            if (isGameOver()) {
                break;
            }
        }
    }

    private void step(float dt) {
        totalElapsedTime += dt;
        if (announcementTimer > 0) {
            announcementTimer = Math.max(0, announcementTimer - dt);
        }

        // 1. Advance real-time combat simulation
        engine.update(dt);

        // 2. Check if player was defeated during combat
        if (!engine.getPlayer().isAlive()) {
            if (currentStage != DungeonStage.DEFEAT) {
                DungeonStage fallenStage = currentStage;
                transitionToStage(DungeonStage.DEFEAT);
                setAnnouncement("FALLEN IN THE DEPTHS", "Encounter: " + fallenStage.getTitle(), 5.0f);
                notifyDungeonConcluded(false);
            }
            return;
        }

        // 3. Stage-specific state machine progression
        switch (currentStage) {
            case ENTRANCE -> {
                stageTimer += dt;
                if (stageTimer >= ENTRANCE_DURATION) {
                    startEncounterOne();
                }
            }
            case ENCOUNTER_ONE -> {
                // Check if all vanguard enemies in wave 1 are eliminated
                if (engine.getAliveEnemyCount() == 0) {
                    startEncounterOneCleared();
                }
            }
            case ENCOUNTER_ONE_CLEARED -> {
                stageTimer += dt;
                if (stageTimer >= CLEAR_ONE_DURATION) {
                    startEncounterTwo();
                }
            }
            case ENCOUNTER_TWO -> {
                // Check if all shadow guard enemies in wave 2 are eliminated
                if (engine.getAliveEnemyCount() == 0) {
                    startBossGate();
                }
            }
            case BOSS_GATE -> {
                stageTimer += dt;
                if (stageTimer >= BOSS_GATE_DURATION) {
                    startBossEncounter();
                }
            }
            case BOSS_ENCOUNTER -> {
                stageTimer += dt;
                // Boss encounter clears when the boss is defeated
                if (engine.getAliveEnemyCount() == 0) {
                    startVictory();
                }
            }
            case VICTORY, DEFEAT -> {
                // Terminal states
            }
        }
    }

    private void startEncounterOne() {
        transitionToStage(DungeonStage.ENCOUNTER_ONE);
        stageTimer = 0.0f;

        float width = engine.getArena().getWidth();
        float groundY = engine.getArena().getGroundY();

        List<RealTimeEnemy> wave = new ArrayList<>();
        // Group of 2 weaker skirmishers: 1 standard melee + 1 agile stalker safely within arena bounds
        wave.add(new RealTimeEnemy(EnemyType.RIFT_SPAWN, width * 0.58f, groundY));
        wave.add(new RealTimeEnemy(EnemyType.RIFT_SKULKER, width * 0.72f, groundY));
        engine.setEnemies(wave);

        setAnnouncement("ENCOUNTER 1: VOID PATROL", "Defeat the 2 approaching void beasts!", 2.2f);
    }

    private void startEncounterOneCleared() {
        transitionToStage(DungeonStage.ENCOUNTER_ONE_CLEARED);
        stageTimer = 0.0f;
        setAnnouncement("AREA SECURED", "Gate to inner catacombs unlocking...", CLEAR_ONE_DURATION);
    }

    private void startEncounterTwo() {
        transitionToStage(DungeonStage.ENCOUNTER_TWO);
        stageTimer = 0.0f;

        float width = engine.getArena().getWidth();
        float groundY = engine.getArena().getGroundY();

        List<RealTimeEnemy> wave = new ArrayList<>();
        // Group of 3 challenging enemies safely positioned across the arena: melee, stalker, caster
        wave.add(new RealTimeEnemy(EnemyType.RIFT_SPAWN, width * 0.54f, groundY));
        wave.add(new RealTimeEnemy(EnemyType.RIFT_SKULKER, width * 0.67f, groundY));
        wave.add(new RealTimeEnemy(EnemyType.RIFT_CASTER, width * 0.78f, groundY));
        engine.setEnemies(wave);

        setAnnouncement("ENCOUNTER 2: SHADOW GUARD", "Beware the Abyssal Caster's dark projectiles!", 2.5f);
    }

    private void startBossGate() {
        transitionToStage(DungeonStage.BOSS_GATE);
        stageTimer = 0.0f;
        setAnnouncement("ANCIENT SEAL SHATTERS", "The Titan awakens — Boss Gate Opened!", BOSS_GATE_DURATION);
    }

    public void startBossDirectly() {
        startBossEncounter();
    }

    private void startBossEncounter() {
        transitionToStage(DungeonStage.BOSS_ENCOUNTER);
        stageTimer = 0.0f;

        float width = engine.getArena().getWidth();
        float groundY = engine.getArena().getGroundY();

        List<RealTimeEnemy> bossList = new ArrayList<>();
        bossList.add(new RealTimeBoss(width * 0.72f, groundY));
        engine.setEnemies(bossList);

        setAnnouncement("BOSS ENCOUNTER: THE RIFT WARDEN", "Titan of the Cataclysm — Watch for telegraph warnings!", 3.0f);
    }

    private void startVictory() {
        transitionToStage(DungeonStage.VICTORY);
        stageTimer = 0.0f;
        engine.setGameOver(true, true);
        setAnnouncement("DUNGEON CLEARED!", "The dimensional rift has been sealed!", 6.0f);
        notifyDungeonConcluded(true);
    }

    /**
     * Completely resets the entire dungeon session for a fresh replay.
     */
    public void restart() {
        DungeonStage prev = this.currentStage;
        initDungeon();
        notifyStageChanged(prev, DungeonStage.ENTRANCE);
    }

    private void transitionToStage(DungeonStage newStage) {
        DungeonStage prev = this.currentStage;
        this.currentStage = newStage;
        notifyStageChanged(prev, newStage);
    }

    public void setAnnouncement(String title, String subtext, float duration) {
        this.announcementTitle = title;
        this.announcementSubtext = subtext;
        this.announcementTimer = duration;
        notifyAnnouncement(title, subtext, duration);
    }

    // ------------------------------------------------------------------
    // Event Notification
    // ------------------------------------------------------------------

    public void addListener(DungeonEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyStageChanged(DungeonStage prev, DungeonStage next) {
        for (DungeonEventListener l : listeners) {
            l.onStageChanged(prev, next);
        }
    }

    private void notifyAnnouncement(String title, String subtext, float duration) {
        for (DungeonEventListener l : listeners) {
            l.onAnnouncement(title, subtext, duration);
        }
    }

    private void notifyEnemyKilled(RealTimeEnemy enemy) {
        for (DungeonEventListener l : listeners) {
            l.onEnemyKilled(enemy, totalEnemiesDefeated);
        }
    }

    private void notifyDungeonConcluded(boolean victory) {
        for (DungeonEventListener l : listeners) {
            l.onDungeonConcluded(victory, totalElapsedTime, totalEnemiesDefeated);
        }
    }

    // ------------------------------------------------------------------
    // Getters and Accessors
    // ------------------------------------------------------------------

    public DungeonStage getCurrentStage() {
        return currentStage;
    }

    public float getStageTimer() {
        return stageTimer;
    }

    public void setStageTimer(float stageTimer) {
        this.stageTimer = stageTimer;
    }

    public float getTotalElapsedTime() {
        return totalElapsedTime;
    }

    public int getTotalEnemiesDefeated() {
        return totalEnemiesDefeated;
    }

    public int getEnemiesRemainingInCurrentWave() {
        return engine.getAliveEnemyCount();
    }

    public Hero getHero() {
        return hero;
    }

    public RealTimeCombatEngine getCombatEngine() {
        return engine;
    }

    public RealTimePlayer getPlayer() {
        return engine.getPlayer();
    }

    public boolean isBossActive() {
        return currentStage == DungeonStage.BOSS_ENCOUNTER && !engine.getEnemies().isEmpty()
                && (engine.getEnemies().get(0) instanceof RealTimeBoss);
    }

    public RealTimeBoss getBoss() {
        if (isBossActive()) {
            return (RealTimeBoss) engine.getEnemies().get(0);
        }
        return null;
    }

    public boolean isGameOver() {
        return currentStage == DungeonStage.VICTORY || currentStage == DungeonStage.DEFEAT;
    }

    public boolean isVictory() {
        return currentStage == DungeonStage.VICTORY;
    }

    public boolean isDefeat() {
        return currentStage == DungeonStage.DEFEAT;
    }

    public String getAnnouncementTitle() {
        return announcementTitle;
    }

    public String getAnnouncementSubtext() {
        return announcementSubtext;
    }

    public boolean isAnnouncementActive() {
        return announcementTimer > 0;
    }

    public String getPerformanceRank() {
        if (!isVictory()) {
            return "D-RANK (FALLEN)";
        }
        if (totalElapsedTime <= 45.0f) {
            return "S-RANK (LEGENDARY HUNTER)";
        } else if (totalElapsedTime <= 75.0f) {
            return "A-RANK (ELITE HUNTER)";
        } else {
            return "B-RANK (SURVIVOR)";
        }
    }
}
