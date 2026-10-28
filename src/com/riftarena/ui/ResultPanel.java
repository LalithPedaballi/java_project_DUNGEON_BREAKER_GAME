package com.riftarena.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable result screen shown after any battle or completed dungeon run.
 *
 * Visualizes authoritative clear time, enemies defeated, and performance Hunter Rank
 * inside a framed dark-fantasy victory/defeat layout.
 */
public class ResultPanel extends JPanel {

    private static final String VICTORY_HEADING = "VICTORY";
    private static final String DEFEAT_HEADING = "DEFEAT";

    private boolean victory = true;
    private int turns = 0;
    private int elapsedSeconds = 0;
    private int enemiesDefeated = 0;
    private String performanceRank = null;

    private final Theme.RiftButton againButton;
    private final Theme.RiftButton menuButton;

    public ResultPanel(MainFrame frame) {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        // --- Footer buttons ---------------------------------------------
        JPanel buttonRow = new JPanel(new GridBagLayout());
        buttonRow.setOpaque(false);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 60, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 14, 8, 14);

        againButton = Theme.button("PLAY AGAIN", Theme.GOLD_BRIGHT);
        againButton.setPreferredSize(new Dimension(220, 48));
        againButton.setFocusable(false);
        againButton.addActionListener(e -> {
            if (frame != null) {
                frame.showScreen(MainFrame.Screen.HERO_SELECT);
            }
        });

        menuButton = Theme.button("MAIN MENU", Theme.VIOLET);
        menuButton.setPreferredSize(new Dimension(220, 48));
        menuButton.setFocusable(false);
        menuButton.addActionListener(e -> {
            if (frame != null) {
                frame.showScreen(MainFrame.Screen.MENU);
            }
        });

        buttonRow.add(againButton, constraints);
        buttonRow.add(menuButton, constraints);
        add(buttonRow, BorderLayout.SOUTH);

        // Screen-reader friendly heading (kept off-screen visually)
        JLabel accessibilityHeading = new JLabel(VICTORY_HEADING);
        accessibilityHeading.setVisible(false);
        add(accessibilityHeading, BorderLayout.NORTH);
    }

    /**
     * Configures the screen for the given outcome and redisplays it.
     */
    public void setOutcome(boolean victory) {
        setOutcome(victory, 0);
    }

    /**
     * Configures the screen with the outcome and total turns elapsed.
     */
    public void setOutcome(boolean victory, int turns) {
        this.victory = victory;
        this.turns = turns;
        this.elapsedSeconds = turns;
        this.enemiesDefeated = victory ? 1 : 0;
        this.performanceRank = victory ? "SURVIVOR" : "FALLEN";
        repaint();
    }

    /**
     * Configures the screen with authoritative dungeon run statistics.
     */
    public void setOutcome(boolean victory, int elapsedSeconds, int enemiesDefeated, String rank) {
        this.victory = victory;
        this.elapsedSeconds = elapsedSeconds;
        this.enemiesDefeated = enemiesDefeated;
        this.performanceRank = rank;
        this.turns = elapsedSeconds;
        repaint();
    }

    public boolean isVictory() {
        return victory;
    }

    public int getTurns() {
        return turns;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getEnemiesDefeated() {
        return enemiesDefeated;
    }

    public String getPerformanceRank() {
        return performanceRank;
    }

    public Theme.RiftButton getPlayAgainButton() {
        return againButton;
    }

    public Theme.RiftButton getMainMenuButton() {
        return menuButton;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int centreX = width / 2;

        Theme.paintBackground(g2, width, height);

        Color accent = victory ? Theme.GOLD : Theme.DANGER;
        int titleY = (int) (height * 0.22);

        // Radiant halo circle behind heading
        RadialGradientPaint halo = new RadialGradientPaint(centreX, titleY - 10, 160f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), victory ? 70 : 45),
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20),
                        new Color(0, 0, 0, 0)});
        g2.setPaint(halo);
        g2.fillOval(centreX - 160, titleY - 170, 320, 320);

        Theme.drawRuneCircle(g2, centreX, titleY - 20, Math.min(width, height) / 6,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55));

        // Heading with outcome gradient
        if (victory) {
            Theme.drawCenteredText(g2, VICTORY_HEADING, Theme.TITLE_FONT, new Color(0, 0, 0, 180), centreX + 2, titleY + 3);
            Theme.drawCenteredGradientText(g2, VICTORY_HEADING, Theme.TITLE_FONT, centreX, titleY,
                    Theme.GOLD_BRIGHT, Theme.GOLD_DIM);
        } else {
            Theme.drawCenteredText(g2, DEFEAT_HEADING, Theme.TITLE_FONT, new Color(0, 0, 0, 180), centreX + 2, titleY + 3);
            Theme.drawCenteredGradientText(g2, DEFEAT_HEADING, Theme.TITLE_FONT, centreX, titleY,
                    new Color(0xF0, 0x47, 0x47), new Color(0x7A, 0x1A, 0x1A));
        }

        // Flavour line
        String flavour = victory
                ? "The dimensional rift dims. The arena remembers your victory."
                : "The abyssal depths claimed your soul. Rise and challenge the rift once more.";
        Theme.drawCenteredText(g2, flavour, Theme.SUBTITLE_FONT, Theme.TEXT_DIM, centreX, titleY + 48);

        // Framed Statistics Card
        int cardW = Math.min(540, width - 80);
        int cardH = 200;
        int cardX = centreX - cardW / 2;
        int cardY = titleY + 85;

        g2.setPaint(new GradientPaint(cardX, cardY, new Color(0x18, 0x13, 0x26, 230),
                cardX, cardY + cardH, new Color(0x10, 0x0D, 0x1A, 240)));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 16, 16));

        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 16, 16));

        // Card Header
        g2.setFont(Theme.CARD_FONT);
        g2.setColor(Theme.GOLD);
        FontMetrics fm = g2.getFontMetrics();
        String summaryTitle = victory ? "\u2605  HUNTER TRIAL CONQUERED  \u2605" : "\u2620  TRIAL RECORD  \u2620";
        g2.drawString(summaryTitle, centreX - fm.stringWidth(summaryTitle) / 2, cardY + 32);

        g2.setColor(new Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), 60));
        g2.drawLine(cardX + 30, cardY + 44, cardX + cardW - 30, cardY + 44);

        // Authoritative statistics breakdown
        String timeStr = String.format("%02d:%02d (%ds)", elapsedSeconds / 60, elapsedSeconds % 60, elapsedSeconds);
        String timeLabel = elapsedSeconds > 0 ? timeStr : "\u2014";
        String killsLabel = String.valueOf(enemiesDefeated);
        String rankStr = (performanceRank != null) ? performanceRank : (victory ? "SURVIVOR" : "FALLEN");

        int rowY = cardY + 76;
        paintStatLine(g2, "TOTAL CLEAR TIME", timeLabel, cardX + 35, rowY, cardW - 70, Theme.TEXT);
        rowY += 34;
        paintStatLine(g2, "ENEMIES VANQUISHED", killsLabel, cardX + 35, rowY, cardW - 70, Theme.TEXT);
        rowY += 36;

        // Elevated Rank Badge with glowing filigree
        int badgeW = cardW - 70;
        int badgeH = 32;
        int badgeX = cardX + 35;
        int badgeY = rowY - 18;

        g2.setColor(new Color(15, 10, 24, 220));
        g2.fill(new RoundRectangle2D.Double(badgeX, badgeY, badgeW, badgeH, 10, 10));

        Color rankColor = victory ? Theme.GOLD_BRIGHT : Theme.DANGER;
        g2.setColor(new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 160));
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(new RoundRectangle2D.Double(badgeX, badgeY, badgeW, badgeH, 10, 10));

        g2.setFont(Theme.BODY_FONT);
        g2.setColor(Theme.TEXT_DIM);
        g2.drawString("PERFORMANCE RANK", badgeX + 16, badgeY + 21);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics bfm = g2.getFontMetrics();
        int rW = bfm.stringWidth(rankStr);
        g2.setColor(rankColor);
        g2.drawString(rankStr, badgeX + badgeW - rW - 16, badgeY + 21);

        Theme.drawFrame(g2, width, height);
        g2.dispose();
    }

    private void paintStatLine(Graphics2D g2, String label, String value, int x, int y, int totalW, Color valColor) {
        g2.setFont(Theme.BODY_FONT);
        g2.setColor(Theme.TEXT_DIM);
        g2.drawString(label, x, y);

        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int valW = fm.stringWidth(value);
        g2.setColor(valColor);
        g2.drawString(value, x + totalW - valW, y);
    }
}
