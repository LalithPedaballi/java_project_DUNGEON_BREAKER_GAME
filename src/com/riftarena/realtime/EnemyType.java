package com.riftarena.realtime;

/**
 * Defines the archetype, behavior profile, and combat parameters for real-time enemies.
 */
public enum EnemyType {

    /**
     * Standard melee void horror. Closes in steadily and delivers telegraphed claw strikes.
     */
    RIFT_SPAWN("Rift Spawn", 85, 165.0f, 85.0f, 14, 0.68f, 0.36f, 1.30f, 220.0f, 70.0f, 196.0f),

    /**
     * Swift, agile stalker. Closes distance rapidly and attacks with quick, relentless slashes.
     */
    RIFT_SKULKER("Rift Skulker", 60, 235.0f, 75.0f, 11, 0.45f, 0.22f, 0.85f, 180.0f, 60.0f, 168.0f),

    /**
     * Ranged abyssal sorcerer. Maintains tactical distance, retreats if crowded,
     * and charges up visible dark void bolt projectiles.
     */
    RIFT_CASTER("Rift Caster", 70, 125.0f, 480.0f, 16, 0.80f, 0.50f, 2.20f, 160.0f, 65.0f, 189.0f),

    /**
     * Colossal dungeon boss: The Rift Warden.
     * Features high durability, halberd cleaves, void blasts, and a dangerous Phase II ground shockwave slam.
     */
    RIFT_WARDEN("The Rift Warden", 240, 140.0f, 115.0f, 24, 0.90f, 0.45f, 1.60f, 280.0f, 95.0f, 252.0f);

    private final String displayName;
    private final int maxHp;
    private final float moveSpeed;
    private final float attackRange;
    private final int baseDamage;
    private final float attackDuration;
    private final float windupDuration;
    private final float attackCooldown;
    private final float knockbackForce;
    private final float width;
    private final float height;

    EnemyType(String displayName, int maxHp, float moveSpeed, float attackRange,
              int baseDamage, float attackDuration, float windupDuration,
              float attackCooldown, float knockbackForce, float width, float height) {
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.moveSpeed = moveSpeed;
        this.attackRange = attackRange;
        this.baseDamage = baseDamage;
        this.attackDuration = attackDuration;
        this.windupDuration = windupDuration;
        this.attackCooldown = attackCooldown;
        this.knockbackForce = knockbackForce;
        this.width = width;
        this.height = height;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public float getAttackDuration() {
        return attackDuration;
    }

    public float getWindupDuration() {
        return windupDuration;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public float getKnockbackForce() {
        return knockbackForce;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}

