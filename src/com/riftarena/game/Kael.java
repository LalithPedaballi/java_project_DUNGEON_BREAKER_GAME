package com.riftarena.game;

/**
 * Kael champion: agile spellsword duelist wielding a high-precision rapier
 * and channeling arcane energy through crimson martial finesse.
 *
 * Special Ability: <b>Crimson Thrust</b>
 * Delivers a lightning-fast piercing fencing thrust dealing 36 heavy damage
 * directly penetrating the enemy's guard.
 */
public class Kael extends Hero {

    public static final String DEFAULT_NAME = "Kael";
    public static final int MAX_HP = 100;
    public static final int BASE_ATTACK = 16;
    public static final int HEAL_AMOUNT = 18;
    public static final int SPECIAL_COOLDOWN = 3;
    public static final int SPECIAL_DAMAGE = 36;

    public Kael() {
        this(DEFAULT_NAME);
    }

    public Kael(String name) {
        super(name, MAX_HP, BASE_ATTACK, HEAL_AMOUNT, SPECIAL_COOLDOWN);
    }

    @Override
    public int executeSpecial(Combatant target) {
        int initialHp = target.getCurrentHp();
        // Crimson Thrust pierces through defenses
        boolean priorDefense = target.isDefending();
        target.setDefending(false);
        target.takeDamage(SPECIAL_DAMAGE);
        target.setDefending(priorDefense);
        return initialHp - target.getCurrentHp();
    }

    @Override
    public String getSpecialName() {
        return "Crimson Thrust";
    }

    @Override
    public String getSpecialDescription() {
        return "Delivers a lightning-fast rapier strike dealing " + SPECIAL_DAMAGE + " damage piercing defenses.";
    }
}

