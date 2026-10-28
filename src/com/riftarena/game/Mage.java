package com.riftarena.game;

/**
 * Mage champion: high-damage spellcaster capable of piercing defensive barriers.
 *
 * Special Ability: <b>Arcane Nova</b>
 * Unleashes a devastating burst of pure magical energy dealing 40 damage.
 * Arcane Nova bypasses and shatters the target's defensive stance, guaranteeing
 * full unmitigated damage.
 *
 * Syllabus concepts demonstrated:
 *   - Inheritance (extends Hero).
 *   - Polymorphism and method overriding.
 */
public class Mage extends Hero {

    public static final String DEFAULT_NAME = "Mage";
    public static final int MAX_HP = 80;
    public static final int BASE_ATTACK = 25;
    public static final int HEAL_AMOUNT = 22;
    public static final int SPECIAL_COOLDOWN = 4;
    public static final int SPECIAL_DAMAGE = 40;

    public Mage() {
        this(DEFAULT_NAME);
    }

    public Mage(String name) {
        super(name, MAX_HP, BASE_ATTACK, HEAL_AMOUNT, SPECIAL_COOLDOWN);
    }

    @Override
    public int executeSpecial(Combatant target) {
        int initialHp = target.getCurrentHp();

        // Armor-piercing effect: breaks through defensive stance
        if (target.isDefending()) {
            target.setDefending(false);
        }

        target.takeDamage(SPECIAL_DAMAGE);
        return initialHp - target.getCurrentHp();
    }

    @Override
    public String getSpecialName() {
        return "Arcane Nova";
    }

    @Override
    public String getSpecialDescription() {
        return "Deals " + SPECIAL_DAMAGE + " damage, completely piercing and shattering enemy defense.";
    }
}

