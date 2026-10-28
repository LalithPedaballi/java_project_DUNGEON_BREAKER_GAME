package com.riftarena.realtime;

import com.riftarena.game.Assassin;
import com.riftarena.game.Hero;
import com.riftarena.game.Mage;
import com.riftarena.game.Warrior;

import java.util.List;

/**
 * Real-time player champion supporting Warrior, Mage, and Assassin.
 *
 * Encapsulates:
 *   - Continuous movement, acceleration, and arena bounds.
 *   - Jumping with gravity and landing physics.
 *   - Class-specific movement speeds and dash evasions.
 *   - Signature basic and special attack timings, active hitboxes, and cooldowns.
 *   - Projectile casting for ranged spellcasters (Mage).
 */
public class RealTimePlayer extends RealTimeEntity {

    private final Hero hero;
    private final String heroClass;

    private float moveSpeed;
    private float jumpStrength;
    private float dashSpeed;
    private float maxDashCooldown;

    private float attackBufferTimer = 0.0f;
    private float specialBufferTimer = 0.0f;
    private float dashBufferTimer = 0.0f;
    private static final float BUFFER_WINDOW = 0.28f;

    private float specialMeter = 100.0f; // 0.0 to 100.0 (starts full for battle readiness)
    private static final float MAX_SPECIAL_METER = 100.0f;

    private boolean projectileSpawnedForAttack = false;

    public RealTimePlayer(Hero hero, float startX, float startY) {
        super(startX, startY, 60.0f, 190.0f, hero.getMaxHp());
        this.hero = hero;
        this.heroClass = hero.getName();
        initializeClassParameters();
    }

    private void initializeClassParameters() {
        if ("Mage".equalsIgnoreCase(heroClass) || "Zephyra".equalsIgnoreCase(heroClass)) {
            moveSpeed = 260.0f;
            jumpStrength = -530.0f;
            dashSpeed = 580.0f;
            maxDashCooldown = 1.1f;
        } else if ("Assassin".equalsIgnoreCase(heroClass) || "Nyx".equalsIgnoreCase(heroClass)) {
            moveSpeed = 340.0f;
            jumpStrength = -560.0f;
            dashSpeed = 720.0f;
            maxDashCooldown = 0.75f;
        } else if ("Kael".equalsIgnoreCase(heroClass)) {
            moveSpeed = 310.0f;
            jumpStrength = -550.0f;
            dashSpeed = 680.0f;
            maxDashCooldown = 0.90f;
        } else if ("Vexara".equalsIgnoreCase(heroClass)) {
            moveSpeed = 265.0f;
            jumpStrength = -540.0f;
            dashSpeed = 600.0f;
            maxDashCooldown = 1.10f;
        } else if ("Draven".equalsIgnoreCase(heroClass)) {
            moveSpeed = 245.0f;
            jumpStrength = -520.0f;
            dashSpeed = 550.0f;
            maxDashCooldown = 1.30f;
        } else { // Warrior / Aegis
            moveSpeed = 280.0f;
            jumpStrength = -550.0f;
            dashSpeed = 620.0f;
            maxDashCooldown = 1.2f;
        }
    }

    @Override
    public void updateTimers(float dt) {
        super.updateTimers(dt);
        if (attackBufferTimer > 0) attackBufferTimer = Math.max(0, attackBufferTimer - dt);
        if (specialBufferTimer > 0) specialBufferTimer = Math.max(0, specialBufferTimer - dt);
        if (dashBufferTimer > 0) dashBufferTimer = Math.max(0, dashBufferTimer - dt);

        // Passive special power charge over time (+8%/sec)
        if (specialMeter < MAX_SPECIAL_METER) {
            specialMeter = Math.min(MAX_SPECIAL_METER, specialMeter + 8.0f * dt);
        }
    }

    public float getSpecialMeter() {
        return specialMeter;
    }

    public void addSpecialPower(float amount) {
        this.specialMeter = Math.min(MAX_SPECIAL_METER, this.specialMeter + amount);
    }

    public boolean isSpecialReady() {
        return specialMeter >= MAX_SPECIAL_METER && specialCooldownTimer <= 0.0f;
    }

    /**
     * Processes player keyboard input, executes attacks, triggers dashes, and handles jumping.
     */
    public void handleInput(InputState input, RealTimeArena arena, List<Projectile> projectiles, float dt) {
        if (!isAlive() || state == EntityState.HURT) {
            return;
        }

        // Buffer action intentions so rapid taps during animation recovery execute smoothly
        if (input.isAttackRequested()) {
            attackBufferTimer = BUFFER_WINDOW;
        }
        if (input.isSpecialRequested()) {
            specialBufferTimer = BUFFER_WINDOW;
        }
        if (input.isDashRequested()) {
            dashBufferTimer = BUFFER_WINDOW;
        }

        // 1. Dash handling
        if (state == EntityState.DASHING) {
            if (stateTime >= 0.18f) {
                transitionTo(isGrounded ? EntityState.IDLE : EntityState.FALLING);
            }
            return;
        }

        // 2. Attack execution
        if (state == EntityState.ATTACKING || state == EntityState.SPECIAL) {
            // Decelerate horizontal momentum during attack swing
            vx *= 0.85f;

            // Mage projectile spawn check
            if ("Mage".equalsIgnoreCase(heroClass) && state == EntityState.ATTACKING) {
                if (!projectileSpawnedForAttack && getAttackProgress() >= 0.25f) {
                    float spawnX = x + (facingRight ? 45.0f : -45.0f);
                    float spawnY = y - 105.0f;
                    float projSpeed = facingRight ? 580.0f : -580.0f;
                    projectiles.add(new Projectile(spawnX, spawnY, projSpeed, 0, 16.0f, 22, 220.0f, true));
                    projectileSpawnedForAttack = true;
                }
            }

            // Assassin Shadow Strike forward burst
            if ("Assassin".equalsIgnoreCase(heroClass) && state == EntityState.SPECIAL) {
                if (getAttackProgress() >= 0.12f && getAttackProgress() <= 0.38f) {
                    vx = (facingRight ? 1 : -1) * 460.0f;
                }
            }
            return;
        }

        // 3. Movement input
        if (input.isMoveLeft() && !input.isMoveRight()) {
            vx = -moveSpeed;
            facingRight = false;
        } else if (input.isMoveRight() && !input.isMoveLeft()) {
            vx = moveSpeed;
            facingRight = true;
        } else {
            vx = 0;
        }

        // 4. Jump
        if (input.isJump() && isGrounded) {
            vy = jumpStrength;
            isGrounded = false;
            transitionTo(EntityState.JUMPING);
        }

        // 5. Dash / Evade
        if (dashBufferTimer > 0 && dashCooldownTimer <= 0) {
            dashBufferTimer = 0.0f;
            input.setDashRequested(false);
            dashCooldownTimer = maxDashCooldown;
            invulnerableTimer = 0.22f; // i-frames during evade dash
            transitionTo(EntityState.DASHING);
            vx = (facingRight ? 1 : -1) * dashSpeed;
            return;
        }

        // 6. Special Attack (Requires ready special power meter and cooldown)
        if (specialBufferTimer > 0 && isSpecialReady()) {
            specialBufferTimer = 0.0f;
            attackBufferTimer = 0.0f;
            input.setSpecialRequested(false);
            triggerSpecialAttack();
            return;
        }

        // 7. Basic Attack
        if (attackBufferTimer > 0 && attackCooldownTimer <= 0) {
            attackBufferTimer = 0.0f;
            input.setAttackRequested(false);
            triggerBasicAttack();
            return;
        }

        // 8. Physical state mapping
        if (isGrounded) {
            if (Math.abs(vx) > 10.0f) {
                transitionTo(EntityState.RUNNING);
            } else {
                transitionTo(EntityState.IDLE);
            }
        } else {
            if (vy < 0) {
                transitionTo(EntityState.JUMPING);
            } else {
                transitionTo(EntityState.FALLING);
            }
        }
    }

    private void triggerBasicAttack() {
        targetsHitThisAttack.clear();
        projectileSpawnedForAttack = false;

        String cls = heroClass.toLowerCase();
        if (cls.contains("mage") || cls.contains("zephyra")) {
            totalAttackDuration = 0.32f;
            attackDurationTimer = 0.32f;
            attackCooldownTimer = 0.48f;
        } else if (cls.contains("assassin") || cls.contains("nyx")) {
            totalAttackDuration = 0.22f;
            attackDurationTimer = 0.22f;
            attackCooldownTimer = 0.26f;
        } else if (cls.contains("kael")) {
            totalAttackDuration = 0.25f;
            attackDurationTimer = 0.25f;
            attackCooldownTimer = 0.30f;
        } else if (cls.contains("vexara")) {
            totalAttackDuration = 0.36f;
            attackDurationTimer = 0.36f;
            attackCooldownTimer = 0.40f;
        } else if (cls.contains("draven")) {
            totalAttackDuration = 0.38f;
            attackDurationTimer = 0.38f;
            attackCooldownTimer = 0.44f;
        } else { // Warrior / Aegis
            totalAttackDuration = 0.34f;
            attackDurationTimer = 0.34f;
            attackCooldownTimer = 0.38f;
        }

        transitionTo(EntityState.ATTACKING);
    }

    private void triggerSpecialAttack() {
        targetsHitThisAttack.clear();
        specialMeter = 0.0f; // Consume special power meter

        String cls = heroClass.toLowerCase();
        if (cls.contains("mage") || cls.contains("zephyra")) {
            totalAttackDuration = 0.44f;
            attackDurationTimer = 0.44f;
            specialCooldownTimer = 4.5f;
        } else if (cls.contains("assassin") || cls.contains("nyx")) {
            totalAttackDuration = 0.30f;
            attackDurationTimer = 0.30f;
            specialCooldownTimer = 3.2f;
        } else if (cls.contains("kael")) {
            totalAttackDuration = 0.35f;
            attackDurationTimer = 0.35f;
            specialCooldownTimer = 3.5f;
        } else if (cls.contains("vexara")) {
            totalAttackDuration = 0.45f;
            attackDurationTimer = 0.45f;
            specialCooldownTimer = 4.2f;
            heal(15); // Soul Reap life drain
        } else if (cls.contains("draven")) {
            totalAttackDuration = 0.46f;
            attackDurationTimer = 0.46f;
            specialCooldownTimer = 4.0f;
            invulnerableTimer = 0.30f; // Magma brace
        } else { // Warrior / Aegis
            totalAttackDuration = 0.40f;
            attackDurationTimer = 0.40f;
            specialCooldownTimer = 3.5f;
            invulnerableTimer = 0.35f; // Shield Slam guard
        }

        transitionTo(EntityState.SPECIAL);
    }

    @Override
    public Hitbox getActiveAttackHitbox() {
        if (state != EntityState.ATTACKING && state != EntityState.SPECIAL) {
            return null;
        }

        float progress = getAttackProgress();
        String cls = heroClass.toLowerCase();

        if (state == EntityState.ATTACKING) {
            if (cls.contains("mage") || cls.contains("zephyra")) {
                // Mage basic attack uses physical projectiles rather than a melee hitbox
                return null;
            } else if (cls.contains("assassin") || cls.contains("nyx")) {
                if (progress >= 0.12f && progress <= 0.78f) {
                    float bx = facingRight ? x : (x - 90.0f);
                    return new Hitbox(bx, y - 140.0f, 90.0f, 105.0f);
                }
            } else if (cls.contains("kael")) {
                if (progress >= 0.12f && progress <= 0.76f) {
                    float bx = facingRight ? x : (x - 105.0f);
                    return new Hitbox(bx, y - 133.0f, 105.0f, 98.0f);
                }
            } else if (cls.contains("vexara")) {
                if (progress >= 0.14f && progress <= 0.80f) {
                    float bx = facingRight ? x : (x - 125.0f);
                    return new Hitbox(bx, y - 161.0f, 125.0f, 147.0f);
                }
            } else if (cls.contains("draven")) {
                if (progress >= 0.16f && progress <= 0.80f) {
                    float bx = facingRight ? x : (x - 100.0f);
                    return new Hitbox(bx, y - 154.0f, 100.0f, 126.0f);
                }
            } else { // Warrior / Aegis
                if (progress >= 0.14f && progress <= 0.78f) {
                    float bx = facingRight ? x : (x - 110.0f);
                    return new Hitbox(bx, y - 154.0f, 110.0f, 119.0f);
                }
            }
        } else if (state == EntityState.SPECIAL) {
            if (cls.contains("mage") || cls.contains("zephyra")) {
                // Arcane Nova: large 360-degree area blast
                if (progress >= 0.16f && progress <= 0.78f) {
                    return new Hitbox(x - 120.0f, y - 189.0f, 240.0f, 210.0f);
                }
            } else if (cls.contains("assassin") || cls.contains("nyx")) {
                // Shadow Strike
                if (progress >= 0.12f && progress <= 0.82f) {
                    float bx = facingRight ? (x - 20.0f) : (x - 130.0f);
                    return new Hitbox(bx, y - 154.0f, 150.0f, 126.0f);
                }
            } else if (cls.contains("kael")) {
                // Crimson Thrust
                if (progress >= 0.12f && progress <= 0.82f) {
                    float bx = facingRight ? x : (x - 140.0f);
                    return new Hitbox(bx, y - 140.0f, 140.0f, 112.0f);
                }
            } else if (cls.contains("vexara")) {
                // Soul Reap
                if (progress >= 0.14f && progress <= 0.82f) {
                    float bx = facingRight ? (x - 30.0f) : (x - 160.0f);
                    return new Hitbox(bx, y - 175.0f, 190.0f, 175.0f);
                }
            } else if (cls.contains("draven")) {
                // Magma Eruption: volcanic ground blast around Draven
                if (progress >= 0.16f && progress <= 0.84f) {
                    return new Hitbox(x - 125.0f, y - 168.0f, 250.0f, 182.0f);
                }
            } else { // Warrior / Aegis
                // Shield Slam
                if (progress >= 0.16f && progress <= 0.78f) {
                    float bx = facingRight ? x : (x - 120.0f);
                    return new Hitbox(bx, y - 161.0f, 120.0f, 133.0f);
                }
            }
        }

        return null;
    }

    @Override
    public int getAttackDamage() {
        String cls = heroClass.toLowerCase();
        if (state == EntityState.SPECIAL) {
            if (cls.contains("mage") || cls.contains("zephyra")) return 40;
            if (cls.contains("draven")) return 42;
            if (cls.contains("kael")) return 36;
            if (cls.contains("vexara")) return 35;
            if (cls.contains("assassin") || cls.contains("nyx")) return 35;
            return 25; // Warrior / Aegis Shield Slam
        } else {
            if (cls.contains("draven")) return 24;
            if (cls.contains("mage") || cls.contains("zephyra")) return 22;
            if (cls.contains("assassin") || cls.contains("nyx")) return 22;
            if (cls.contains("vexara")) return 17;
            if (cls.contains("kael")) return 16;
            return 18; // Warrior / Aegis
        }
    }

    public float getAttackKnockback() {
        String cls = heroClass.toLowerCase();
        if (state == EntityState.SPECIAL) {
            if (cls.contains("draven")) return 460.0f;
            if (cls.contains("mage") || cls.contains("zephyra")) return 360.0f;
            if (cls.contains("vexara")) return 340.0f;
            if (cls.contains("kael")) return 320.0f;
            if (cls.contains("assassin") || cls.contains("nyx")) return 300.0f;
            return 420.0f; // Warrior
        } else {
            if (cls.contains("draven")) return 280.0f;
            if (cls.contains("vexara")) return 220.0f;
            if (cls.contains("kael")) return 210.0f;
            if (cls.contains("mage") || cls.contains("zephyra")) return 200.0f;
            if (cls.contains("assassin") || cls.contains("nyx")) return 190.0f;
            return 250.0f; // Warrior
        }
    }

    public Hero getHero() {
        return hero;
    }

    public String getHeroClass() {
        return heroClass;
    }

    public float getMaxDashCooldown() {
        return maxDashCooldown;
    }

    public float getMaxSpecialCooldown() {
        String cls = heroClass.toLowerCase();
        if (cls.contains("mage") || cls.contains("zephyra")) return 4.5f;
        if (cls.contains("vexara")) return 4.2f;
        if (cls.contains("draven")) return 4.0f;
        if (cls.contains("kael")) return 3.5f;
        if (cls.contains("assassin") || cls.contains("nyx")) return 3.2f;
        return 3.5f; // Warrior
    }

    /**
     * Resets player combat state completely for a fresh encounter or dungeon restart.
     */
    public void resetCombatState() {
        restoreFullHp();
        specialMeter = 100.0f;
        attackBufferTimer = 0.0f;
        specialBufferTimer = 0.0f;
        dashBufferTimer = 0.0f;
        projectileSpawnedForAttack = false;
        vx = 0.0f;
        vy = 0.0f;
        facingRight = true;
    }
}

