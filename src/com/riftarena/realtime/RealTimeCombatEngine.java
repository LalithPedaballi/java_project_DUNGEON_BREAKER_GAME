package com.riftarena.realtime;

import com.riftarena.game.Enemy;
import com.riftarena.game.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Authoritative real-time combat simulation engine.
 *
 * Coordinates:
 *   - Player movement, jumping, dashing, attacks, and projectile casts.
 *   - Enemy group AI pursuit, attack telegraphing, active hit windows, and recovery.
 *   - Delta-time based physics integration, ground collision, and arena boundary clamping.
 *   - Precise AABB hitbox collision detection with one-hit-per-attack guarantee across all targets.
 *   - Authoritative damage application, temporary invulnerability (i-frames), and knockback.
 *   - Dynamic multi-enemy encounters and boss mechanics.
 *   - Game flow states: play, pause, victory, defeat, and fresh restart.
 */
public class RealTimeCombatEngine {

    public interface CombatFeedbackListener {
        default void onDamageDealt(String attackerName, String targetName, float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
            onDamageDealt(hitX, hitY, damage, isCrit, isPlayerTarget);
        }
        void onDamageDealt(float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget);
        void onAttackPerformed(String attacker, boolean isSpecial);
        void onGameOver(boolean victory);
        default void onEnemyDefeated(RealTimeEnemy enemy) {}
        default void onBossPhaseTransition(RealTimeBoss boss, int newPhase) {}
    }

    private final RealTimeArena arena;
    private final InputState inputState;
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<CombatFeedbackListener> feedbackListeners = new ArrayList<>();

    private RealTimePlayer player;
    private final List<RealTimeEnemy> enemies = new ArrayList<>();
    private final java.util.Set<RealTimeEnemy> defeatedNotified = new java.util.HashSet<>();
    private final java.util.Set<RealTimeBoss> phase2Notified = new java.util.HashSet<>();

    private boolean isDungeonMode = false;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private float matchTime = 0.0f;

    public RealTimeCombatEngine(Hero hero, Enemy enemy) {
        this(hero, enemy, new RealTimeArena());
    }

    public RealTimeCombatEngine(Hero hero, Enemy enemy, RealTimeArena arena) {
        this.arena = Objects.requireNonNull(arena, "Arena cannot be null");
        this.inputState = new InputState();
        initCombatants(hero, enemy);
    }

    private void initCombatants(Hero hero, Enemy enemy) {
        float startPlayerX = arena.getWidth() * 0.25f;
        float startEnemyX = arena.getWidth() * 0.75f;
        float groundY = arena.getGroundY();

        this.player = new RealTimePlayer(hero, startPlayerX, groundY);
        this.enemies.clear();
        this.defeatedNotified.clear();
        this.phase2Notified.clear();
        if (enemy != null) {
            this.enemies.add(new RealTimeEnemy(enemy, startEnemyX, groundY));
        }
        this.projectiles.clear();
        this.matchTime = 0.0f;
        this.isGameOver = false;
        this.isVictory = false;
        this.isPaused = false;
        this.inputState.reset();
    }

    /**
     * Advances the authoritative simulation by delta time (clamped for stability).
     */
    public void update(float rawDt) {
        // Clamp frame delta time to avoid large physics tunnels during frame drops
        float dt = Math.min(rawDt, 0.05f);

        // Handle pause toggle
        if (inputState.isPauseRequested()) {
            inputState.consumePause();
            if (!isGameOver) {
                isPaused = !isPaused;
            }
        }

        // Handle restart
        if (inputState.isRestartRequested()) {
            inputState.consumeRestart();
            restart();
            return;
        }

        if (isPaused) {
            // Cancel pending action triggers and freeze player velocity while paused
            inputState.setAttackRequested(false);
            inputState.setSpecialRequested(false);
            inputState.setDashRequested(false);
            player.setVx(0.0f);
            return;
        }

        if (isGameOver) {
            // Completely freeze and discard all inputs once match/dungeon concludes
            inputState.reset();
            player.setVx(0.0f);
            return;
        }

        matchTime += dt;

        // 1. Update cooldown timers
        player.updateTimers(dt);
        for (int i = 0; i < enemies.size(); i++) {
            enemies.get(i).updateTimers(dt);
        }

        // 2. Handle player input and intentions
        EntityState prevPlayerState = player.getState();
        player.handleInput(inputState, arena, projectiles, dt);
        if (player.getState() != prevPlayerState &&
                (player.getState() == EntityState.ATTACKING || player.getState() == EntityState.SPECIAL)) {
            notifyAttack(player.getHeroClass(), player.getState() == EntityState.SPECIAL);
        }

        // 3. Update enemy AI
        for (int i = 0; i < enemies.size(); i++) {
            RealTimeEnemy e = enemies.get(i);
            if (e.isAlive()) {
                EntityState prevEnemyState = e.getState();
                e.updateAI(player, arena, projectiles, dt);
                if (e.getState() != prevEnemyState && e.getState() == EntityState.ATTACKING) {
                    notifyAttack(e.getEnemyName(), false);
                }
            }
        }

        // 4. Physical integration & arena collisions
        arena.applyPhysics(player, dt);
        for (int i = 0; i < enemies.size(); i++) {
            RealTimeEnemy e = enemies.get(i);
            if (e.isAlive()) {
                arena.applyPhysics(e, dt);
            }
        }

        // 5. Update projectiles and resolve projectile hits
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile proj = projectiles.get(i);
            proj.update(dt, arena);

            if (proj.isActive()) {
                if (proj.isFromPlayer()) {
                    // Check collision against all alive enemies
                    for (int j = 0; j < enemies.size(); j++) {
                        RealTimeEnemy e = enemies.get(j);
                        if (e.isAlive() && !e.isInvulnerable()) {
                            if (proj.getHitbox().intersects(e.getHurtbox())) {
                                float knockbackDir = proj.getVx() >= 0 ? 1.0f : -1.0f;
                                int damageDealt = e.takeDamage(proj.getDamage(), knockbackDir, proj.getKnockbackForce());
                                if (damageDealt > 0) {
                                    player.addSpecialPower(20.0f);
                                    notifyHit(player.getHeroClass(), e.getEnemyName(), proj.getX(), proj.getY(), damageDealt, true, false);
                                    if (!e.isAlive()) {
                                        notifyEnemyDefeated(e);
                                    }
                                }
                                proj.deactivate();
                                break;
                            }
                        }
                    }
                } else {
                    // Enemy projectile hitting player (deactivates on impact with player hurtbox or i-frame shield)
                    if (player.isAlive() && proj.getHitbox().intersects(player.getHurtbox())) {
                        if (!player.isInvulnerable()) {
                            float knockbackDir = proj.getVx() >= 0 ? 1.0f : -1.0f;
                            int damageDealt = player.takeDamage(proj.getDamage(), knockbackDir, proj.getKnockbackForce());
                            if (damageDealt > 0) {
                                String projName = (proj.getRadius() >= 18) ? "Ground Shockwave" : "Void Bolt";
                                notifyHit(projName, player.getHeroClass(), player.getX(), player.getY() - 40.0f, damageDealt, false, true);
                            }
                        }
                        proj.deactivate();
                    }
                }
            }
        }
        projectiles.removeIf(p -> !p.isActive());

        // 6. Player melee/area hit detection
        Hitbox playerAttackHitbox = player.getActiveAttackHitbox();
        if (playerAttackHitbox != null) {
            for (int i = 0; i < enemies.size(); i++) {
                RealTimeEnemy e = enemies.get(i);
                if (e.isAlive() && !player.hasHitTarget(e)) {
                    if (playerAttackHitbox.intersects(e.getHurtbox())) {
                        float knockbackDir = player.isFacingRight() ? 1.0f : -1.0f;
                        int damageDealt = e.takeDamage(player.getAttackDamage(), knockbackDir, player.getAttackKnockback());
                        player.recordHitOnTarget(e);

                        if (damageDealt > 0) {
                            player.addSpecialPower(20.0f);
                            boolean isSpecial = (player.getState() == EntityState.SPECIAL);
                            notifyHit(player.getHeroClass(), e.getEnemyName(), e.getX(), e.getY() - 65.0f, damageDealt, isSpecial, false);
                            if (!e.isAlive()) {
                                notifyEnemyDefeated(e);
                            }
                        }
                    }
                }
            }
        }

        // 7. Enemy attack hit detection
        for (int i = 0; i < enemies.size(); i++) {
            RealTimeEnemy e = enemies.get(i);
            if (e.isAlive()) {
                Hitbox enemyAttackHitbox = e.getActiveAttackHitbox();
                if (enemyAttackHitbox != null && player.isAlive() && !e.hasHitTarget(player)) {
                    if (enemyAttackHitbox.intersects(player.getHurtbox())) {
                        float knockbackDir = e.isFacingRight() ? 1.0f : -1.0f;
                        int damageDealt = player.takeDamage(e.getAttackDamage(), knockbackDir, e.getAttackKnockback());
                        e.recordHitOnTarget(player);

                        if (damageDealt > 0) {
                            player.addSpecialPower(15.0f);
                            notifyHit(e.getEnemyName(), player.getHeroClass(), player.getX(), player.getY() - 65.0f, damageDealt, false, true);
                        }
                    }
                }
            }
        }

        // 8. Track and notify any newly defeated enemies & boss phase changes
        for (int i = 0; i < enemies.size(); i++) {
            RealTimeEnemy e = enemies.get(i);
            if (!e.isAlive() && !defeatedNotified.contains(e)) {
                defeatedNotified.add(e);
                notifyEnemyDefeated(e);
            }
            if (e instanceof RealTimeBoss boss) {
                if (boss.getBossPhase() >= 2 && !phase2Notified.contains(boss)) {
                    phase2Notified.add(boss);
                    notifyBossPhaseTransition(boss, boss.getBossPhase());
                }
            }
        }

        // 9. Match outcome evaluation
        if (!player.isAlive()) {
            isGameOver = true;
            isVictory = false;
            notifyGameOver(false);
        } else if (!isDungeonMode && !enemies.isEmpty() && getAliveEnemyCount() == 0) {
            isGameOver = true;
            isVictory = true;
            notifyGameOver(true);
        }
    }

    /**
     * Completely restarts the encounter with fresh combatants, full HP, cleared cooldowns,
     * and reset positions.
     */
    public void restart() {
        Enemy primaryEnemy = enemies.isEmpty() ? null : enemies.get(0).getEnemy();
        initCombatants(player.getHero(), primaryEnemy);
    }

    public void addFeedbackListener(CombatFeedbackListener listener) {
        if (listener != null) {
            feedbackListeners.add(listener);
        }
    }

    private void notifyHit(String attackerName, String targetName, float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
        for (CombatFeedbackListener l : feedbackListeners) {
            l.onDamageDealt(attackerName, targetName, hitX, hitY, damage, isCrit, isPlayerTarget);
        }
    }

    private void notifyHit(float hitX, float hitY, int damage, boolean isCrit, boolean isPlayerTarget) {
        notifyHit("Attacker", "Target", hitX, hitY, damage, isCrit, isPlayerTarget);
    }

    private void notifyAttack(String attacker, boolean isSpecial) {
        for (CombatFeedbackListener l : feedbackListeners) {
            l.onAttackPerformed(attacker, isSpecial);
        }
    }

    private void notifyGameOver(boolean victory) {
        for (CombatFeedbackListener l : feedbackListeners) {
            l.onGameOver(victory);
        }
    }

    private void notifyEnemyDefeated(RealTimeEnemy enemy) {
        for (CombatFeedbackListener l : feedbackListeners) {
            l.onEnemyDefeated(enemy);
        }
    }

    private void notifyBossPhaseTransition(RealTimeBoss boss, int newPhase) {
        for (CombatFeedbackListener l : feedbackListeners) {
            l.onBossPhaseTransition(boss, newPhase);
        }
    }

    // ------------------------------------------------------------------
    // Multi-enemy Encounter Configuration
    // ------------------------------------------------------------------

    public void setEnemies(List<RealTimeEnemy> newEnemies) {
        this.enemies.clear();
        this.defeatedNotified.clear();
        this.phase2Notified.clear();
        if (newEnemies != null) {
            this.enemies.addAll(newEnemies);
        }
        // Reset match outcome flags if we're repopulating for a new wave/encounter
        if (player.isAlive() && !enemies.isEmpty()) {
            this.isGameOver = false;
            this.isVictory = false;
        }
    }

    public void addEnemy(RealTimeEnemy e) {
        if (e != null) {
            this.enemies.add(e);
        }
    }

    public List<RealTimeEnemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public int getAliveEnemyCount() {
        int count = 0;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).isAlive()) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Getters and Accessors
    // ------------------------------------------------------------------

    public RealTimePlayer getPlayer() {
        return player;
    }

    public RealTimeEnemy getEnemy() {
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).isAlive()) {
                return enemies.get(i);
            }
        }
        return enemies.isEmpty() ? null : enemies.get(0);
    }

    public RealTimeArena getArena() {
        return arena;
    }

    public InputState getInputState() {
        return inputState;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver, boolean victory) {
        this.isGameOver = gameOver;
        this.isVictory = victory;
        if (gameOver) {
            notifyGameOver(victory);
        }
    }

    public boolean isVictory() {
        return isVictory;
    }

    public boolean isDungeonMode() {
        return isDungeonMode;
    }

    public void setDungeonMode(boolean dungeonMode) {
        this.isDungeonMode = dungeonMode;
    }

    public float getMatchTime() {
        return matchTime;
    }
}
