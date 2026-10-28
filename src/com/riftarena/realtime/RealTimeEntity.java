package com.riftarena.realtime;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for all real-time battlefield combatants (Player and Enemy).
 *
 * Encapsulates:
 *   - 2D position (feet at x, y) and velocity vectors (vx, vy).
 *   - Grounded state and facing direction.
 *   - Authoritative health with strict [0, maxHp] clamping.
 *   - State machine (IDLE, RUNNING, JUMPING, FALLING, ATTACKING, HURT, DEFEATED, etc.).
 *   - Attack cooldowns and temporary damage invulnerability (i-frames).
 *   - Hit registration tracking ensuring exactly one hit per target per attack swing.
 */
public abstract class RealTimeEntity {

    protected float x;
    protected float y;
    protected float vx;
    protected float vy;

    protected float width;
    protected float height;

    protected boolean facingRight = true;
    protected boolean isGrounded = true;

    protected int currentHp;
    protected final int maxHp;

    protected EntityState state = EntityState.IDLE;
    protected float stateTime = 0.0f;

    protected float invulnerableTimer = 0.0f;
    protected float hurtTimer = 0.0f;

    protected float attackCooldownTimer = 0.0f;
    protected float specialCooldownTimer = 0.0f;
    protected float dashCooldownTimer = 0.0f;

    protected float attackDurationTimer = 0.0f;
    protected float totalAttackDuration = 0.35f;

    protected final Set<RealTimeEntity> targetsHitThisAttack = new HashSet<>();

    protected RealTimeEntity(float startX, float startY, float width, float height, int maxHp) {
        if (maxHp <= 0) {
            throw new IllegalArgumentException("Max HP must be positive: " + maxHp);
        }
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
    }

    /**
     * Updates cooldowns, i-frame timers, and state durations by delta time in seconds.
     */
    public void updateTimers(float dt) {
        stateTime += dt;

        if (invulnerableTimer > 0) {
            invulnerableTimer = Math.max(0, invulnerableTimer - dt);
        }

        if (hurtTimer > 0) {
            hurtTimer = Math.max(0, hurtTimer - dt);
            if (hurtTimer == 0 && state == EntityState.HURT && isAlive()) {
                transitionTo(isGrounded ? EntityState.IDLE : (vy < 0 ? EntityState.JUMPING : EntityState.FALLING));
            }
        }

        if (attackCooldownTimer > 0) {
            attackCooldownTimer = Math.max(0, attackCooldownTimer - dt);
        }

        if (specialCooldownTimer > 0) {
            specialCooldownTimer = Math.max(0, specialCooldownTimer - dt);
        }

        if (dashCooldownTimer > 0) {
            dashCooldownTimer = Math.max(0, dashCooldownTimer - dt);
        }

        if (attackDurationTimer > 0) {
            attackDurationTimer = Math.max(0, attackDurationTimer - dt);
            if (attackDurationTimer == 0 && (state == EntityState.ATTACKING || state == EntityState.SPECIAL) && isAlive()) {
                targetsHitThisAttack.clear();
                transitionTo(isGrounded ? EntityState.IDLE : EntityState.FALLING);
            }
        }
    }

    /**
     * Authoritative damage application with invulnerability checking, health clamping,
     * knockback physics, and death transition.
     *
     * @param rawDamage       incoming damage points
     * @param knockbackDirX   horizontal direction of knockback (-1 for left, 1 for right)
     * @param knockbackForce  speed of knockback impulse in px/s
     * @return actual damage dealt, or 0 if invulnerable or dead
     */
    public int takeDamage(int rawDamage, float knockbackDirX, float knockbackForce) {
        if (!isAlive() || isInvulnerable() || rawDamage <= 0) {
            return 0;
        }

        int previousHp = currentHp;
        currentHp = Math.max(0, currentHp - rawDamage);
        int damageDealt = previousHp - currentHp;

        if (currentHp <= 0) {
            transitionTo(EntityState.DEFEATED);
            vx = 0;
        } else {
            transitionTo(EntityState.HURT);
            hurtTimer = 0.25f;
            invulnerableTimer = (this instanceof RealTimeEnemy) ? 0.22f : 0.40f;
            vx = knockbackDirX * knockbackForce;
            vy = -140f; // slight vertical lift on impact
            isGrounded = false;
        }

        return damageDealt;
    }

    /**
     * Checks if this entity has already damaged the specified target during the current attack.
     */
    public boolean hasHitTarget(RealTimeEntity target) {
        return targetsHitThisAttack.contains(target);
    }

    /**
     * Registers a successful hit on the specified target for the active attack.
     */
    public void recordHitOnTarget(RealTimeEntity target) {
        if (target != null) {
            targetsHitThisAttack.add(target);
        }
    }

    /**
     * Changes entity state and resets state timer.
     */
    public void transitionTo(EntityState newState) {
        if (this.state != newState) {
            this.state = newState;
            this.stateTime = 0.0f;
        }
    }

    /**
     * Returns the bounding hurtbox of this entity (where it can be hit).
     */
    public Hitbox getHurtbox() {
        return new Hitbox(x - width * 0.5f, y - height, width, height);
    }

    /**
     * Returns the active attack hitbox, or null if the entity is not currently attacking
     * or is outside the active damage frames.
     */
    public abstract Hitbox getActiveAttackHitbox();

    /**
     * Returns the damage dealt by the currently active attack.
     */
    public abstract int getAttackDamage();

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getVx() {
        return vx;
    }

    public void setVx(float vx) {
        this.vx = vx;
    }

    public float getVy() {
        return vy;
    }

    public void setVy(float vy) {
        this.vy = vy;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setGrounded(boolean grounded) {
        this.isGrounded = grounded;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = Math.max(0, Math.min(maxHp, currentHp));
        if (this.currentHp <= 0) {
            transitionTo(EntityState.DEFEATED);
        } else if (state == EntityState.DEFEATED) {
            transitionTo(EntityState.IDLE);
        }
    }

    public void heal(int amount) {
        if (amount > 0 && isAlive()) {
            this.currentHp = Math.min(maxHp, this.currentHp + amount);
        }
    }

    public void restoreFullHp() {
        this.currentHp = this.maxHp;
        this.invulnerableTimer = 0.0f;
        this.hurtTimer = 0.0f;
        this.attackCooldownTimer = 0.0f;
        this.specialCooldownTimer = 0.0f;
        this.dashCooldownTimer = 0.0f;
        this.attackDurationTimer = 0.0f;
        this.totalAttackDuration = 0.0f;
        this.targetsHitThisAttack.clear();
        this.vx = 0.0f;
        this.vy = 0.0f;
        transitionTo(EntityState.IDLE);
    }

    public int getMaxHp() {
        return maxHp;
    }

    public boolean isAlive() {
        return currentHp > 0;
    }

    public boolean isInvulnerable() {
        return invulnerableTimer > 0;
    }

    public EntityState getState() {
        return state;
    }

    public float getStateTime() {
        return stateTime;
    }

    public float getAttackCooldownTimer() {
        return attackCooldownTimer;
    }

    public void setAttackCooldownTimer(float timer) {
        this.attackCooldownTimer = Math.max(0, timer);
    }

    public float getSpecialCooldownTimer() {
        return specialCooldownTimer;
    }

    public void setSpecialCooldownTimer(float timer) {
        this.specialCooldownTimer = Math.max(0, timer);
    }

    public float getDashCooldownTimer() {
        return dashCooldownTimer;
    }

    public void setDashCooldownTimer(float timer) {
        this.dashCooldownTimer = Math.max(0, timer);
    }

    public float getInvulnerableTimer() {
        return invulnerableTimer;
    }

    public void setInvulnerableTimer(float invulnerableTimer) {
        this.invulnerableTimer = Math.max(0, invulnerableTimer);
    }

    public float getHurtTimer() {
        return hurtTimer;
    }

    public boolean isHurt() {
        return hurtTimer > 0 || state == EntityState.HURT;
    }

    public float getAttackProgress() {
        if (totalAttackDuration <= 0) {
            return 0.0f;
        }
        float progress = 1.0f - (attackDurationTimer / totalAttackDuration);
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    /**
     * Normalized progress value (0.0 to 1.0) appropriate for the entity's active state.
     * Maps to attack frame completion, hurt recoil recovery, or defeat animation.
     */
    public float getStateProgress() {
        if (state == EntityState.ATTACKING || state == EntityState.SPECIAL) {
            return getAttackProgress();
        }
        if (state == EntityState.HURT) {
            return Math.max(0.0f, Math.min(1.0f, 1.0f - (hurtTimer / 0.25f)));
        }
        if (state == EntityState.DEFEATED) {
            return Math.max(0.0f, Math.min(1.0f, stateTime / 0.8f));
        }
        return stateTime;
    }
}
