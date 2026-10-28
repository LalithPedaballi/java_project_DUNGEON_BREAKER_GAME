package com.riftarena.realtime;

import com.riftarena.game.Enemy;

import java.util.List;

/**
 * Real-time enemy controller implementing pursuit AI, attack telegraphs,
 * active strike windows, recovery cycles, and archetype differentiation
 * (Melee Skirmishers, Agile Stalkers, and Ranged Sorcerers).
 */
public class RealTimeEnemy extends RealTimeEntity {

    protected final Enemy enemy;
    protected final EnemyType type;

    protected boolean hasFiredProjectileThisAttack = false;

    public RealTimeEnemy(Enemy enemy, float startX, float startY) {
        this(enemy, EnemyType.RIFT_SPAWN, startX, startY);
    }

    public RealTimeEnemy(Enemy enemy, EnemyType type, float startX, float startY) {
        super(startX, startY, type.getWidth(), type.getHeight(),
                enemy != null && enemy.getMaxHp() > 0 ? enemy.getMaxHp() : type.getMaxHp());
        this.enemy = (enemy != null) ? enemy
                : new Enemy(type.getDisplayName(), type.getMaxHp(), type.getBaseDamage(), 10, Enemy.Behaviour.TACTICAL);
        this.type = type;
        this.facingRight = false; // Enemies face left toward the player by default
    }

    public RealTimeEnemy(EnemyType type, float startX, float startY) {
        this(null, type, startX, startY);
    }

    /**
     * Backward-compatible delegation without projectile list.
     */
    public void updateAI(RealTimePlayer player, RealTimeArena arena, float dt) {
        updateAI(player, arena, null, dt);
    }

    /**
     * Executes real-time AI decision-making: player tracking, range evaluation,
     * attack windups, projectile launching, and cooldown recovery.
     */
    public void updateAI(RealTimePlayer player, RealTimeArena arena, List<Projectile> projectiles, float dt) {
        if (!isAlive() || state == EntityState.HURT) {
            return;
        }

        if (state == EntityState.ATTACKING) {
            // Decelerate during active strike
            vx *= 0.82f;

            // Handle ranged enemy projectile release at windup completion
            if (type == EnemyType.RIFT_CASTER && !hasFiredProjectileThisAttack && projectiles != null) {
                float windupRatio = type.getWindupDuration() / type.getAttackDuration();
                if (getAttackProgress() >= windupRatio) {
                    hasFiredProjectileThisAttack = true;
                    float projDir = facingRight ? 1.0f : -1.0f;
                    float spawnX = x + (projDir * (width * 0.5f + 15.0f));
                    float spawnY = y - height * 0.55f;
                    projectiles.add(new Projectile(spawnX, spawnY, projDir * 330.0f, 0.0f,
                            14.0f, type.getBaseDamage(), 170.0f, false));
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

        // Turn to face opponent
        facingRight = (dx > 0);

        if (type == EnemyType.RIFT_CASTER) {
            updateCasterAI(dx, distance, arena);
        } else {
            updateMeleeAI(dx, distance);
        }
    }

    private void updateMeleeAI(float dx, float distance) {
        if (distance <= type.getAttackRange()) {
            // Within strike range
            vx = 0;
            if (attackCooldownTimer <= 0) {
                // Initiate readable attack sequence: windup -> strike -> recovery
                targetsHitThisAttack.clear();
                hasFiredProjectileThisAttack = false;
                totalAttackDuration = type.getAttackDuration();
                attackDurationTimer = type.getAttackDuration();
                attackCooldownTimer = type.getAttackCooldown();
                transitionTo(EntityState.ATTACKING);
            } else {
                transitionTo(EntityState.IDLE);
            }
        } else if (distance <= 850.0f) {
            // Pursue player
            vx = (dx > 0 ? 1 : -1) * type.getMoveSpeed();
            transitionTo(EntityState.RUNNING);
        } else {
            // Idle when player is beyond detection range
            vx = 0;
            transitionTo(EntityState.IDLE);
        }
    }

    private void updateCasterAI(float dx, float distance, RealTimeArena arena) {
        float minSafeDistance = 210.0f;

        // Check if caster is cornered against arena boundary
        boolean isCornered = false;
        if (arena != null) {
            float cornerBuffer = 80.0f;
            float minX = arena.getLeftBound() + width * 0.5f + cornerBuffer;
            float maxX = arena.getRightBound() - width * 0.5f - cornerBuffer;
            if (dx > 0 && x <= minX) {
                isCornered = true; // Player is to the right, but caster is up against left wall
            } else if (dx < 0 && x >= maxX) {
                isCornered = true; // Player is to the left, but caster is up against right wall
            }
        }

        if (distance < minSafeDistance && !isCornered) {
            // Back away from the player to maintain ranged advantage when space permits
            vx = (dx > 0 ? -1 : 1) * type.getMoveSpeed();
            transitionTo(EntityState.RUNNING);
        } else if (distance <= type.getAttackRange() || isCornered) {
            // In casting range or forced to stand ground when cornered
            vx = 0;
            if (attackCooldownTimer <= 0) {
                targetsHitThisAttack.clear();
                hasFiredProjectileThisAttack = false;
                totalAttackDuration = type.getAttackDuration();
                attackDurationTimer = type.getAttackDuration();
                attackCooldownTimer = type.getAttackCooldown();
                transitionTo(EntityState.ATTACKING);
            } else {
                transitionTo(EntityState.IDLE);
            }
        } else {
            // Advance towards casting range
            vx = (dx > 0 ? 1 : -1) * type.getMoveSpeed();
            transitionTo(EntityState.RUNNING);
        }
    }

    @Override
    public Hitbox getActiveAttackHitbox() {
        if (state != EntityState.ATTACKING) {
            return null;
        }

        // Casters deal damage through projectiles, not melee hitboxes
        if (type == EnemyType.RIFT_CASTER) {
            return null;
        }

        float progress = getAttackProgress();
        float minProgress = (type == EnemyType.RIFT_SPAWN) ? 0.36f : (type.getWindupDuration() / type.getAttackDuration());
        float maxProgress = (type == EnemyType.RIFT_SPAWN) ? 0.68f : 0.85f;

        // Active damage frames occur strictly after the windup phase
        if (progress >= minProgress && progress <= maxProgress) {
            float boxWidth = (type == EnemyType.RIFT_SKULKER) ? 75.0f : 88.0f;
            float bx = facingRight ? x : (x - boxWidth);
            return new Hitbox(bx, y - 154.0f, boxWidth, 112.0f);
        }

        return null;
    }

    /**
     * Checks if the enemy is in the readable windup phase of an attack,
     * giving the player a clear window to react, step back, or dodge.
     */
    public boolean isTelegraphing() {
        if (state != EntityState.ATTACKING) {
            return false;
        }
        float windupRatio = type.getWindupDuration() / type.getAttackDuration();
        return getAttackProgress() < windupRatio;
    }

    @Override
    public int getAttackDamage() {
        return (enemy != null) ? enemy.getBaseAttack() : type.getBaseDamage();
    }

    public float getAttackKnockback() {
        return type.getKnockbackForce();
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public EnemyType getType() {
        return type;
    }

    public String getEnemyName() {
        return (enemy != null) ? enemy.getName() : type.getDisplayName();
    }
}
