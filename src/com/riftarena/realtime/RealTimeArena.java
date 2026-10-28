package com.riftarena.realtime;

/**
 * Physical arena bounds, ground plane, and gravity simulation.
 */
public class RealTimeArena {

    public static final float DEFAULT_WIDTH = 1120.0f;
    public static final float DEFAULT_HEIGHT = 720.0f;
    public static final float DEFAULT_GROUND_Y = 520.0f;
    public static final float DEFAULT_LEFT_BOUND = 70.0f;
    public static final float DEFAULT_RIGHT_BOUND = 1050.0f;
    public static final float GRAVITY = 1350.0f;

    private final float width;
    private final float height;
    private final float groundY;
    private final float leftBound;
    private final float rightBound;

    public RealTimeArena() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GROUND_Y, DEFAULT_LEFT_BOUND, DEFAULT_RIGHT_BOUND);
    }

    public RealTimeArena(float width, float height, float groundY, float leftBound, float rightBound) {
        this.width = width;
        this.height = height;
        this.groundY = groundY;
        this.leftBound = leftBound;
        this.rightBound = rightBound;
    }

    /**
     * Applies velocity integration, gravity, screen boundary clamping, and ground collision.
     */
    public void applyPhysics(RealTimeEntity entity, float dt) {
        if (entity == null) {
            return;
        }

        // 1. Horizontal movement and arena boundary clamping
        entity.x += entity.vx * dt;
        float minX = leftBound + entity.width * 0.5f;
        float maxX = rightBound - entity.width * 0.5f;
        if (entity.x < minX) {
            entity.x = minX;
            if (entity.vx < 0) {
                entity.vx = 0;
            }
        } else if (entity.x > maxX) {
            entity.x = maxX;
            if (entity.vx > 0) {
                entity.vx = 0;
            }
        }

        // 2. Vertical movement and gravity
        if (entity.getState() != EntityState.DASHING) {
            entity.vy += GRAVITY * dt;
        }
        entity.y += entity.vy * dt;

        // 3. Ground collision
        if (entity.y >= groundY) {
            entity.y = groundY;
            entity.vy = 0;
            entity.isGrounded = true;

            if (entity.getState() == EntityState.JUMPING || entity.getState() == EntityState.FALLING) {
                if (Math.abs(entity.vx) > 20.0f) {
                    entity.transitionTo(EntityState.RUNNING);
                } else {
                    entity.transitionTo(EntityState.IDLE);
                }
            }
        } else {
            entity.isGrounded = false;
            if (entity.getState() == EntityState.IDLE || entity.getState() == EntityState.RUNNING) {
                entity.transitionTo(EntityState.FALLING);
            } else if (entity.getState() == EntityState.JUMPING && entity.vy > 0) {
                entity.transitionTo(EntityState.FALLING);
            }
        }
    }

    public boolean isOutOfBounds(float px, float py) {
        return px < leftBound - 60 || px > rightBound + 60 || py < -100 || py > groundY + 100;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getGroundY() {
        return groundY;
    }

    public float getLeftBound() {
        return leftBound;
    }

    public float getRightBound() {
        return rightBound;
    }
}

