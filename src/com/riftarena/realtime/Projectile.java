package com.riftarena.realtime;

/**
 * Ranged projectile traveling across the battlefield (e.g. Mage's Arcane Bolt).
 */
public class Projectile {

    private float x;
    private float y;
    private final float vx;
    private final float vy;
    private final float radius;
    private final int damage;
    private final float knockbackForce;
    private final boolean fromPlayer;

    private boolean active = true;
    private float lifetime = 0.0f;
    private static final float MAX_LIFETIME = 2.5f;

    public Projectile(float x, float y, float vx, float vy, float radius,
                      int damage, float knockbackForce, boolean fromPlayer) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.damage = damage;
        this.knockbackForce = knockbackForce;
        this.fromPlayer = fromPlayer;
    }

    public void update(float dt, RealTimeArena arena) {
        if (!active) {
            return;
        }

        x += vx * dt;
        y += vy * dt;
        lifetime += dt;

        if (lifetime >= MAX_LIFETIME || arena.isOutOfBounds(x, y)) {
            active = false;
        }
    }

    public Hitbox getHitbox() {
        return new Hitbox(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getRadius() {
        return radius;
    }

    public int getDamage() {
        return damage;
    }

    public float getKnockbackForce() {
        return knockbackForce;
    }

    public boolean isFromPlayer() {
        return fromPlayer;
    }

    public float getVx() {
        return vx;
    }

    public float getVy() {
        return vy;
    }
}

