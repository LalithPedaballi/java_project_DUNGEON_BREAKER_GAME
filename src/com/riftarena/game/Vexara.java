package com.riftarena.game;

/**
 * Vexara champion: dark reaper and nether necromancer wielding a spectral purple scythe
 * and manipulating abyssal soul currents.
 *
 * Special Ability: <b>Soul Reap</b>
 * Executes a sweeping dark scythe cleave dealing 35 damage and siphoning 15 vitality back to Vexara.
 */
public class Vexara extends Hero {

    public static final String DEFAULT_NAME = "Vexara";
    public static final int MAX_HP = 85;
    public static final int BASE_ATTACK = 17;
    public static final int HEAL_AMOUNT = 22;
    public static final int SPECIAL_COOLDOWN = 4;
    public static final int SPECIAL_DAMAGE = 35;
    public static final int SOUL_DRAIN_HEAL = 15;

    public Vexara() {
        this(DEFAULT_NAME);
    }

    public Vexara(String name) {
        super(name, MAX_HP, BASE_ATTACK, HEAL_AMOUNT, SPECIAL_COOLDOWN);
    }

    @Override
    public int executeSpecial(Combatant target) {
        int initialHp = target.getCurrentHp();
        target.takeDamage(SPECIAL_DAMAGE);
        int damageDealt = initialHp - target.getCurrentHp();

        // Bonus effect: Siphons life back to Vexara
        heal(SOUL_DRAIN_HEAL);
        return damageDealt;
    }

    @Override
    public String getSpecialName() {
        return "Soul Reap";
    }

    @Override
    public String getSpecialDescription() {
        return "Sweeps a spectral scythe for " + SPECIAL_DAMAGE + " damage and siphons " + SOUL_DRAIN_HEAL + " HP.";
    }
}

