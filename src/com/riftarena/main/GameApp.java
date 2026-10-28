package com.riftarena.main;

import com.riftarena.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Dungeon Clash: Rift Arena — application entry point.
 *
 * Swing rule: all user-interface code must run on the Event Dispatch
 * Thread (EDT). SwingUtilities.invokeLater schedules our window creation
 * on that thread, which is why main() stays tiny and readable.
 */
public final class GameApp {

    /** Game code never instantiates this class; it only starts the game. */
    private GameApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
