package com.riftarena.realtime;

import java.awt.event.KeyEvent;

/**
 * Tracks real-time continuous keyboard input state for the player.
 *
 * Supports WASD, Arrow keys, J (Attack), K (Special), Shift (Dash),
 * ESC (Pause/Resume), and R (Restart).
 */
public class InputState {

    private boolean moveLeft;
    private boolean moveRight;
    private boolean jump;
    private boolean attackRequested;
    private boolean specialRequested;
    private boolean dashRequested;
    private boolean pauseRequested;
    private boolean restartRequested;

    public void onKeyPressed(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> moveLeft = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> moveRight = true;
            case KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_SPACE -> jump = true;
            case KeyEvent.VK_J -> attackRequested = true;
            case KeyEvent.VK_K -> specialRequested = true;
            case KeyEvent.VK_SHIFT -> dashRequested = true;
            case KeyEvent.VK_ESCAPE -> pauseRequested = true;
            case KeyEvent.VK_R -> restartRequested = true;
        }
    }

    public void onKeyReleased(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> moveLeft = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> moveRight = false;
            case KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_SPACE -> jump = false;
            case KeyEvent.VK_J -> attackRequested = false;
            case KeyEvent.VK_K -> specialRequested = false;
            case KeyEvent.VK_SHIFT -> dashRequested = false;
            case KeyEvent.VK_ESCAPE -> pauseRequested = false;
            case KeyEvent.VK_R -> restartRequested = false;
        }
    }

    public void reset() {
        moveLeft = false;
        moveRight = false;
        jump = false;
        attackRequested = false;
        specialRequested = false;
        dashRequested = false;
        pauseRequested = false;
        restartRequested = false;
    }

    public boolean isMoveLeft() {
        return moveLeft;
    }

    public void setMoveLeft(boolean moveLeft) {
        this.moveLeft = moveLeft;
    }

    public boolean isMoveRight() {
        return moveRight;
    }

    public void setMoveRight(boolean moveRight) {
        this.moveRight = moveRight;
    }

    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public boolean isAttackRequested() {
        return attackRequested;
    }

    public void setAttackRequested(boolean attackRequested) {
        this.attackRequested = attackRequested;
    }

    public boolean isSpecialRequested() {
        return specialRequested;
    }

    public void setSpecialRequested(boolean specialRequested) {
        this.specialRequested = specialRequested;
    }

    public boolean isDashRequested() {
        return dashRequested;
    }

    public void setDashRequested(boolean dashRequested) {
        this.dashRequested = dashRequested;
    }

    public boolean isPauseRequested() {
        return pauseRequested;
    }

    public void consumePause() {
        this.pauseRequested = false;
    }

    public boolean isRestartRequested() {
        return restartRequested;
    }

    public void consumeRestart() {
        this.restartRequested = false;
    }
}

