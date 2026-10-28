package com.riftarena.game;

/**
 * Assassin champion: swift, opportunistic skirmisher excelling at finishing wounded foes.
 *
 * Special Ability: <b>Shadow Strike</b>
 * Deals 32 base damage. If the target is wounded below 50% of their maximum HP,
 * the strike lethally executes for 45 damage.
 *
 * Syllabus concepts demonstrated:
 *   - Inheritance (extends Hero).
 *   - Polymorphism and method overriding.
 *   - Conditional logic based on polymorphic object state.
 */
public class Assassin extends Hero {

    public static final String DEFAULT_NAME = "Assassin";
    public static final int MAX_HP = 95;
    public static final int BASE_ATTACK = 22;
    public static final int HEAL_AMOUNT = 18;
    public static final int SPECIAL_COOLDOWN = 3;
    public static final int BASE_SPECIAL_DAMAGE = 32;
    public static final int EXECUTE_SPECIAL_DAMAGE = 45;

    public Assassin() {
        this(DEFAULT_NAME);
    }

    public Assassin(String name) {
        super(name, MAX_HP, BASE_ATTACK, HEAL_AMOUNT, SPECIAL_COOLDOWN);
    }

    @Override
    public int executeSpecial(Combatant target) {
        int initialHp = target.getCurrentHp();
        boolean isExecute = target.getCurrentHp() < (target.getMaxHp() / 2.0);
        int damage = isExecute ? EXECUTE_SPECIAL_DAMAGE : BASE_SPECIAL_DAMAGE;

        target.takeDamage(damage);
        return initialHp - target.getCurrentHp();
    }

    @Override
    public String getSpecialName() {
        return "Shadow Strike";
    }

    @Override
    public String getSpecialDescription() {
        return "Deals " + BASE_SPECIAL_DAMAGE + " damage (escalates to "
                + EXECUTE_SPECIAL_DAMAGE + " damage if target HP < 50%).";
    }
}

