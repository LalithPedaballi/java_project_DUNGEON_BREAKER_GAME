package com.riftarena.realtime;

import com.riftarena.game.Enemy;
import com.riftarena.game.RiftWarden;

import java.util.List;

/**
 * Real-time boss controller for The Rift Warden.
 *
 * Implements:
 *   - Two distinct tactical phases (Guardian & Cataclysm).
 *   - Automatic phase transition with invulnerability burst when falling below 50% HP.
 *   - Three readable attack patterns:
 *       1. Titan Cleave: wide heavy melee sweep with 0.45s telegraph windup.
 *       2. Void Blast: ranged abyssal projectile launched when player keeps distance.
 *       3. Ground Shockwave Slam (Phase II): floor-level energy wave requiring a jump evasion.
 *   - Clear visual telegraph flags allowing UI feedback and player evasion.
 */
public class RealTimeBoss extends RealTimeEnemy {

    public enum BossAttack {
        NONE,
        TITAN_CLEAVE,
        VOID_BLAST,
        SHOCKWAVE_SLAM
    }

    private int bossPhase = 1;
    private boolean phaseTransitionTriggered = false;
    private float phaseTransitionTimer = 0.0f;
    private static final float TRANSITION_DURATION = 1.2f;

    private BossAttack currentAttack = BossAttack.NONE;
    private boolean shockwaveLaunched = false;

    public RealTimeBoss(float startX, float startY) {
        super(new Enemy("The Rift Warden", 240, 24, 15, Enemy.Behaviour.TACTICAL), EnemyType.RIFT_WARDEN, startX, startY);
    }

    public RealTimeBoss(Enemy enemy, float startX, float startY) {
        super(enemy != null ? enemy : new Enemy("The Rift Warden", 240, 24, 15, Enemy.Behaviour.TACTICAL),
                EnemyType.RIFT_WARDEN, startX, startY);
    }

    @Override
    public void updateTimers(float dt) {
        super.updateTimers(dt);

        if (state != EntityState.ATTACKING) {
            currentAttack = BossAttack.NONE;
            shockwaveLaunched = false;
        }

        if (phaseTransitionTimer > 0) {
            phaseTransitionTimer = Math.max(0, phaseTransitionTimer - dt);
            if (phaseTransitionTimer == 0 && isAlive()) {
                // Post-transition speed boost in Cataclysm phase
                transitionTo(EntityState.IDLE);
            }
        }

        // Check for Phase II transition at 50% HP threshold
        if (!phaseTransitionTriggered && currentHp <= maxHp / 2 && isAlive()) {
            triggerPhaseTwo();
        }
    }

    public void triggerPhaseTwo() {
        bossPhase = 2;
        phaseTransitionTriggered = true;
        phaseTransitionTimer = TRANSITION_DURATION;
        invulnerableTimer = TRANSITION_DURATION;
        attackCooldownTimer = 1.0f;
        transitionTo(EntityState.HURT);
        vx = 0;
    }

    @Override
    public void updateAI(RealTimePlayer player, RealTimeArena arena, List<Projectile> projectiles, float dt) {
        if (!isAlive()) {
            return;
        }

        // During phase transition, boss gathers power and remains stationary
        if (phaseTransitionTimer > 0) {
            vx = 0;
            return;
        }

        if (state == EntityState.HURT) {
            return;
        }

        if (state == EntityState.ATTACKING) {
            vx *= 0.82f;

            float progress = getAttackProgress();

            // Handle ranged Void Blast projectile release
            if (currentAttack == BossAttack.VOID_BLAST && !hasFiredProjectileThisAttack && projectiles != null) {
                if (progress >= 0.45f) {
                    hasFiredProjectileThisAttack = true;
                    float projDir = facingRight ? 1.0f : -1.0f;
                    float spawnX = x + (projDir * 65.0f);
                    float spawnY = y - 126.0f;
                    projectiles.add(new Projectile(spawnX, spawnY, projDir * 280.0f, 0.0f,
                            18.0f, 18, 200.0f, false));
                }
            }

            // Handle Phase II Ground Shockwave Slam
            if (currentAttack == BossAttack.SHOCKWAVE_SLAM && !shockwaveLaunched && projectiles != null) {
                if (progress >= 0.50f) {
                    shockwaveLaunched = true;
                    float projDir = facingRight ? 1.0f : -1.0f;
                    float groundY = arena.getGroundY();
                    // Shockwave travels directly along the floor surface (height 36px)
                    // Players who jump will be in the air (y < groundY - 45px) and avoid collision!
                    projectiles.add(new Projectile(x + (projDir * 50.0f), groundY - 18.0f,
                            projDir * 290.0f, 0.0f, 20.0f, 30, 300.0f, false));
                }
            }

            return;
        }

        if (player == null || !player.isAlive()) {
            vx = 0;
            transitionTo(EntityState.IDLE);
            return;
        }

        float dx = player.getX() - x;
        float distance = Math.abs(dx);
        facingRight = (dx > 0);

        float currentSpeed = (bossPhase >= 2) ? 185.0f : 140.0f;

        if (attackCooldownTimer <= 0) {
            decideBossAttack(distance);
        } else {
            // Maneuver towards player
            if (distance > 95.0f) {
                vx = (dx > 0 ? 1 : -1) * currentSpeed;
                transitionTo(EntityState.RUNNING);
            } else {
                vx = 0;
                transitionTo(EntityState.IDLE);
            }
        }
    }

    private void decideBossAttack(float distance) {
        targetsHitThisAttack.clear();
        hasFiredProjectileThisAttack = false;
        shockwaveLaunched = false;
        vx = 0;

        if (bossPhase >= 2 && distance >= 120.0f && Math.random() < 0.55) {
            // Phase II: Ground Shockwave Slam
            currentAttack = BossAttack.SHOCKWAVE_SLAM;
            totalAttackDuration = 1.05f;
            attackDurationTimer = 1.05f;
            attackCooldownTimer = 1.80f;
            transitionTo(EntityState.ATTACKING);
        } else if (distance > 180.0f) {
            // Distant target: Void Blast projectile
            currentAttack = BossAttack.VOID_BLAST;
            totalAttackDuration = 0.85f;
            attackDurationTimer = 0.85f;
            attackCooldownTimer = 1.70f;
            transitionTo(EntityState.ATTACKING);
        } else {
            // Close target: Colossal Titan Cleave
            currentAttack = BossAttack.TITAN_CLEAVE;
            totalAttackDuration = 0.90f;
            attackDurationTimer = 0.90f;
            attackCooldownTimer = (bossPhase >= 2) ? 1.25f : 1.65f;
            transitionTo(EntityState.ATTACKING);
        }
    }

    @Override
    public Hitbox getActiveAttackHitbox() {
        if (state != EntityState.ATTACKING) {
            return null;
        }

        // Only Titan Cleave deals direct melee collision; other attacks use projectiles
        if (currentAttack != BossAttack.TITAN_CLEAVE) {
            return null;
        }

        float progress = getAttackProgress();
        // Active damage frames after 0.45s telegraph
        if (progress >= 0.45f && progress <= 0.80f) {
            float boxWidth = 115.0f;
            float boxHeight = 140.0f;
            float bx = facingRight ? x : (x - boxWidth);
            return new Hitbox(bx, y - height * 0.70f, boxWidth, boxHeight);
        }

        return null;
    }

    @Override
    public int getAttackDamage() {
        if (currentAttack == BossAttack.TITAN_CLEAVE) {
            return (bossPhase >= 2) ? 28 : 24;
        }
        return type.getBaseDamage();
    }

    @Override
    public float getAttackKnockback() {
        return (currentAttack == BossAttack.TITAN_CLEAVE) ? 280.0f : 200.0f;
    }

    public int getBossPhase() {
        return bossPhase;
    }

    public boolean isPhaseTransitionActive() {
        return phaseTransitionTimer > 0;
    }

    public BossAttack getCurrentAttack() {
        return currentAttack;
    }

    /**
     * Checks if boss is in the readable telegraph windup before a dangerous strike.
     */
    public boolean isTelegraphingSlam() {
        return state == EntityState.ATTACKING && currentAttack == BossAttack.SHOCKWAVE_SLAM && !shockwaveLaunched;
    }

    public boolean isTelegraphingCleave() {
        return state == EntityState.ATTACKING && currentAttack == BossAttack.TITAN_CLEAVE && getAttackProgress() < 0.45f;
    }

    public String getTelegraphWarning() {
        if (isTelegraphingSlam()) {
            return "\u26a0 GROUND SLAM \u2014 JUMP TO EVADE!";
        }
        if (isTelegraphingCleave()) {
            return "\u26a0 HEAVY CLEAVE \u2014 DASH OR STEP BACK!";
        }
        return null;
    }
}
