package com.riftarena.realtime;

/**
 * Axis-Aligned Bounding Box (AABB) used for collision detection, hurtboxes,
 * and attack hitboxes in the real-time combat system.
 *
 * Immutable value object representing a 2D rectangular area on the battlefield.
 */
public record Hitbox(float x, float y, float width, float height) {

    public Hitbox {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Hitbox dimensions cannot be negative: " + width + "x" + height);
        }
    }

    public float getMinX() {
        return x;
    }

    public float getMaxX() {
        return x + width;
    }

    public float getMinY() {
        return y;
    }

    public float getMaxY() {
        return y + height;
    }

    public float getCenterX() {
        return x + width * 0.5f;
    }

    public float getCenterY() {
        return y + height * 0.5f;
    }

    /**
     * Checks whether this hitbox intersects with another hitbox.
     */
    public boolean intersects(Hitbox other) {
        if (other == null) {
            return false;
        }
        return getMinX() < other.getMaxX() &&
                getMaxX() > other.getMinX() &&
                getMinY() < other.getMaxY() &&
                getMaxY() > other.getMinY();
    }

    /**
     * Checks whether a point (px, py) lies inside this hitbox.
     */
    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}

