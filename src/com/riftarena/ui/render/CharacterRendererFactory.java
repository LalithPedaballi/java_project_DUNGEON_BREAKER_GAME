package com.riftarena.ui.render;

import com.riftarena.game.Assassin;
import com.riftarena.game.Combatant;
import com.riftarena.game.Enemy;
import com.riftarena.game.Hero;
import com.riftarena.game.Mage;
import com.riftarena.game.RiftWarden;
import com.riftarena.game.Warrior;

/**
 * Factory helper resolving the appropriate {@link CharacterRenderer} for any {@link Combatant}.
 *
 * Syllabus concepts demonstrated:
 *   - Factory pattern (Unit IV): centralizing object instantiation.
 *   - Pattern matching / polymorphism: dispatching based on concrete subclass types.
 */
public final class CharacterRendererFactory {

    private CharacterRendererFactory() {
    }

    /**
     * Resolves the dedicated 2D character renderer for a given combatant.
     *
     * @param combatant the fighter model instance
     * @return the concrete CharacterRenderer
     */
    public static CharacterRenderer forCombatant(Combatant combatant) {
        if (combatant instanceof RiftWarden warden) {
            RiftWardenRenderer renderer = new RiftWardenRenderer();
            renderer.setPhase(warden.getCurrentPhase());
            return renderer;
        }
        if (combatant instanceof Warrior) {
            return new WarriorRenderer();
        }
        if (combatant instanceof Mage) {
            return new MageRenderer();
        }
        if (combatant instanceof Assassin) {
            return new AssassinRenderer();
        }
        if (combatant instanceof com.riftarena.game.Kael) {
            return new KaelRenderer();
        }
        if (combatant instanceof com.riftarena.game.Vexara) {
            return new VexaraRenderer();
        }
        if (combatant instanceof com.riftarena.game.Draven) {
            return new DravenRenderer();
        }
        if (combatant instanceof Enemy) {
            return new RiftSpawnRenderer();
        }
        return new WarriorRenderer(); // Safe fallback
    }

    /**
     * Helper resolving hero renderer from hero name string.
     */
    public static CharacterRenderer forHeroName(String heroName) {
        if (heroName == null) {
            return new WarriorRenderer();
        }
        return switch (heroName.trim().toLowerCase()) {
            case "mage", "zephyra" -> new MageRenderer();
            case "assassin", "nyx" -> new AssassinRenderer();
            case "kael" -> new KaelRenderer();
            case "vexara" -> new VexaraRenderer();
            case "draven" -> new DravenRenderer();
            default -> new WarriorRenderer(); // warrior, aegis
        };
    }
}

