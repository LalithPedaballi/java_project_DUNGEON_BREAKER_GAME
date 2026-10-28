package com.riftarena.game;

/**
 * Draven champion: volcanic juggernaut and magma brawler encased in molten stone armor
 * who channels volcanic seismic energy through earth-shattering punches.
 *
 * Special Ability: <b>Magma Eruption</b>
 * Slams the ground with volcanic fury, erupting molten magma pillars for 42 devastating damage.
 */
public class Draven extends Hero {

    public static final String DEFAULT_NAME = "Draven";
    public static final int MAX_HP = 135;
    public static final int BASE_ATTACK = 22;
    public static final int HEAL_AMOUNT = 15;
    public static final int SPECIAL_COOLDOWN = 4;
    public static final int SPECIAL_DAMAGE = 42;

    public Draven() {
        this(DEFAULT_NAME);
    }

    public Draven(String name) {
        super(name, MAX_HP, BASE_ATTACK, HEAL_AMOUNT, SPECIAL_COOLDOWN);
    }

    @Override
    public int executeSpecial(Combatant target) {
        int initialHp = target.getCurrentHp();
        target.takeDamage(SPECIAL_DAMAGE);
        return initialHp - target.getCurrentHp();
    }

    @Override
    public String getSpecialName() {
        return "Magma Eruption";
    }

    @Override
    public String getSpecialDescription() {
        return "Slams the ground erupting molten pillars for " + SPECIAL_DAMAGE + " devastating damage.";
    }
}

