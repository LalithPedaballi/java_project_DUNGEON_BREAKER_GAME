package com.riftarena.game;

/**
 * Warrior champion: resilient melee fighter with high vitality and defensive prowess.
 *
 * Special Ability: <b>Shield Slam</b>
 * Deals 22 damage to the opponent and immediately raises the Warrior's shield,
 * entering defensive stance for the incoming enemy attack.
 *
 * Syllabus concepts demonstrated:
 *   - Inheritance (extends Hero).
 *   - Constructor chaining with super(...).
 *   - Polymorphic method overriding (executeSpecial).
 */
public class Warrior extends Hero {

    public static final String DEFAULT_NAME = "Warrior";
    public static final int MAX_HP = 120;
    public static final int BASE_ATTACK = 18;
    public static final int HEAL_AMOUNT = 20;
    public static final int SPECIAL_COOLDOWN = 3;
    public static final int SPECIAL_DAMAGE = 22;

    public Warrior() {
        this(DEFAULT_NAME);
    }

    public Warrior(String name) {
        super(name, MAX_HP, BASE_ATTACK, HEAL_AMOUNT, SPECIAL_COOLDOWN);
    }

    @Override
    public int executeSpecial(Combatant target) {
        int initialHp = target.getCurrentHp();
        target.takeDamage(SPECIAL_DAMAGE);
        int damageDealt = initialHp - target.getCurrentHp();

        // Bonus effect: Warrior braces in defensive stance
        setDefending(true);
        return damageDealt;
    }

    @Override
    public String getSpecialName() {
        return "Shield Slam";
    }

    @Override
    public String getSpecialDescription() {
        return "Strikes for " + SPECIAL_DAMAGE + " damage and braces into defensive stance.";
    }
}

