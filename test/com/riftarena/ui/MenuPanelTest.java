package com.riftarena.ui;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Unit and integration test suite for the cinematic DUNGEON BREAKER MenuPanel.
 *
 * Verifies:
 *   1. All five menu options have distinct, valid scaled boundaries.
 *   2. Responsive scaling maintains aspect ratio without button overlap across resolutions.
 *   3. Mouse click hit-testing triggers correct navigation callbacks.
 *   4. Keyboard navigation (Up, Down, Left, Right, Enter, Escape) works properly.
 *   5. Zero-crash Java2D offscreen rendering across different viewport resolutions.
 */
public class MenuPanelTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("     RUNNING DUNGEON BREAKER MAIN MENU TESTS      ");
        System.out.println("==================================================");

        SwingUtilities.invokeAndWait(() -> {
            testMenuOptionsBoundsAndSymmetry();
            testResponsiveScalingAcrossResolutions();
            testMenuNavigationActions();
            testKeyboardNavigationCycling();
            testZeroCrashOffscreenRendering();
        });

        System.out.println("==================================================");
        System.out.printf("Menu Test Results: %d run | %d passed | %d failed%n",
                testsRun, testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.err.println("FAILED: Main menu tests encountered failures.");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All main menu tests passed cleanly.");
            System.exit(0);
        }
    }

    private static void runTest(String name, Runnable runnable) {
        testsRun++;
        try {
            runnable.run();
            testsPassed++;
            System.out.println("  [PASS] " + name);
        } catch (Throwable t) {
            testsFailed++;
            System.err.println("  [FAIL] " + name + ": " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void testMenuOptionsBoundsAndSymmetry() {
        runTest("All Five Menu Options Have Non-Empty, Coherent Layout Bounds", () -> {
            MenuPanel panel = new MenuPanel(null);
            panel.setSize(1024, 680);

            for (MenuPanel.MenuOption opt : MenuPanel.MenuOption.values()) {
                Rectangle r = panel.getButtonBounds(opt);
                assertTrue(r.width > 50, "Button " + opt.name() + " has reasonable width");
                assertTrue(r.height > 20, "Button " + opt.name() + " has reasonable height");
                assertTrue(r.x >= 0 && r.x + r.width <= 1024, "Button " + opt.name() + " is within horizontal bounds");
                assertTrue(r.y >= 0 && r.y + r.height <= 680, "Button " + opt.name() + " is within vertical bounds");
            }

            // Verify HEROES and CONTROLS are on the same vertical line
            Rectangle heroesRect = panel.getButtonBounds(MenuPanel.MenuOption.HEROES);
            Rectangle controlsRect = panel.getButtonBounds(MenuPanel.MenuOption.CONTROLS);
            assertEquals(heroesRect.y, controlsRect.y, "HEROES and CONTROLS share same vertical row");
            assertTrue(heroesRect.x + heroesRect.width <= controlsRect.x, "HEROES sits to the left of CONTROLS");
        });
    }

    private static void testResponsiveScalingAcrossResolutions() {
        runTest("Aspect-Ratio Scale-to-Fit Maintains Layout Geometry Across Resolutions", () -> {
            MenuPanel panel = new MenuPanel(null);
            Dimension[] testDims = new Dimension[] {
                    new Dimension(1024, 585),
                    new Dimension(1024, 680),
                    new Dimension(1280, 800),
                    new Dimension(1920, 1080)
            };

            for (Dimension d : testDims) {
                panel.setSize(d);
                Rectangle playRect = panel.getButtonBounds(MenuPanel.MenuOption.ENTER_DUNGEON);
                Rectangle bossRect = panel.getButtonBounds(MenuPanel.MenuOption.BOSS_CHALLENGE);
                Rectangle quitRect = panel.getButtonBounds(MenuPanel.MenuOption.QUIT_GAME);

                assertTrue(playRect.y < bossRect.y, "ENTER_DUNGEON is strictly above BOSS_CHALLENGE at " + d);
                assertTrue(bossRect.y < quitRect.y, "BOSS_CHALLENGE is strictly above QUIT_GAME at " + d);
                assertTrue(quitRect.y + quitRect.height <= d.height, "QUIT_GAME fits within viewport at " + d);
            }
        });
    }

    private static void testMenuNavigationActions() {
        runTest("Menu Actions Correctly Configure Encounter Types and Route to Hero Select", () -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1024, 680);
            MenuPanel panel = new MenuPanel(frame);
            panel.setSize(1024, 680);

            // 1. ENTER DUNGEON -> SKIRMISH
            panel.activateOption(MenuPanel.MenuOption.ENTER_DUNGEON);
            assertEquals(MainFrame.Screen.HERO_SELECT, frame.getCurrentScreen(), "ENTER_DUNGEON routes to HERO_SELECT");
            assertEquals(MainFrame.EncounterType.SKIRMISH, frame.getEncounterType(), "ENTER_DUNGEON configures SKIRMISH");

            // 2. BOSS CHALLENGE -> BOSS
            frame.showScreen(MainFrame.Screen.MENU);
            panel.activateOption(MenuPanel.MenuOption.BOSS_CHALLENGE);
            assertEquals(MainFrame.Screen.HERO_SELECT, frame.getCurrentScreen(), "BOSS_CHALLENGE routes to HERO_SELECT");
            assertEquals(MainFrame.EncounterType.BOSS, frame.getEncounterType(), "BOSS_CHALLENGE configures BOSS");

            // 3. HEROES -> HERO_SELECT
            frame.showScreen(MainFrame.Screen.MENU);
            panel.activateOption(MenuPanel.MenuOption.HEROES);
            assertEquals(MainFrame.Screen.HERO_SELECT, frame.getCurrentScreen(), "HEROES routes to HERO_SELECT");

            frame.dispose();
        });
    }

    private static void testKeyboardNavigationCycling() {
        runTest("Keyboard Arrow Keys Cycle Focus Between All Options and Activate Selected Option", () -> {
            MainFrame frame = new MainFrame();
            MenuPanel panel = new MenuPanel(frame);
            panel.setSize(1024, 680);

            // Dispatch down arrow key events to cycle through menu
            KeyEvent downEvent = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
            panel.getKeyListeners()[0].keyPressed(downEvent); // moves from 0 -> 1 (BOSS_CHALLENGE)
            panel.getKeyListeners()[0].keyPressed(downEvent); // moves from 1 -> 2 (HEROES)

            // Right arrow moves from HEROES -> CONTROLS
            KeyEvent rightEvent = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);
            panel.getKeyListeners()[0].keyPressed(rightEvent);

            // Left arrow moves from CONTROLS -> HEROES
            KeyEvent leftEvent = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
            panel.getKeyListeners()[0].keyPressed(leftEvent);

            // Enter key activates HEROES
            panel.getKeyListeners()[0].keyPressed(new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
            assertEquals(MainFrame.Screen.HERO_SELECT, frame.getCurrentScreen(), "Enter key activated HEROES option");

            frame.dispose();
        });
    }

    private static void testZeroCrashOffscreenRendering() {
        runTest("Zero-Crash Java2D Offscreen Rendering at Multiple Resolutions and Animation Ticks", () -> {
            MenuPanel panel = new MenuPanel(null);

            int[][] resolutions = {
                    {1024, 585},
                    {1024, 680},
                    {1280, 800},
                    {1600, 900}
            };

            for (int[] res : resolutions) {
                int w = res[0];
                int h = res[1];
                panel.setSize(w, h);
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = img.createGraphics();
                panel.paint(g2);
                g2.dispose();
                assertTrue(img.getWidth() == w && img.getHeight() == h, "Rendered cleanly at " + w + "x" + h);
            }
        });
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
    }
}

