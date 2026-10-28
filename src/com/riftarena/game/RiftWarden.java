package com.riftarena.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The Rift Warden: a multi-phase, high-vitality boss guarding the dimensional fracture.
 *
 * Extends {@link Enemy} cleanly, serving as a polymorphic drop-in replacement for
 * standard encounters in {@link CombatEngine}.
 *
 * Progression phases:
 *   - Phase I (Guardian): 100% to >65% HP. Uses steady attacks and defensive guard stance.
 *   - Phase II (Rift Awakening): <=65% to >30% HP. Energy fractures armor; unlocks Void Rift Blast.
 *   - Phase III (Cataclysm): <=30% to 0% HP. Unbinds spacetime; unleashes Cataclysmic Rupture.
 *
 * Syllabus concepts demonstrated:
 *   - Inheritance (extends Enemy) and polymorphism.
 *   - State machine modeling through nested enums (Phase).
 *   - Method overriding for dynamic damage scaling and action selection.
 *   - Event-driven notifications via CombatEvent without Swing dependencies.
 */
public class RiftWarden extends Enemy {

    /**
     * Discrete phases of the boss encounter, each with an activation threshold.
     */
    public enum Phase {
        PHASE_I_GUARDIAN("Phase I: Guardian", 1.00),
        PHASE_II_AWAKENING("Phase II: Rift Awakening", 0.65),
        PHASE_III_CATACLYSM("Phase III: Cataclysm", 0.30);

        private final String displayName;
        private final double thresholdRatio;

        Phase(String displayName, double thresholdRatio) {
            this.displayName = displayName;
            this.thresholdRatio = thresholdRatio;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getThresholdRatio() {
            return thresholdRatio;
        }
    }

    public static final String DEFAULT_NAME = "The Rift Warden";
    public static final int DEFAULT_MAX_HP = 200;

    // Distinct attack values per phase
    public static final int PHASE_1_ATTACK = 15;
    public static final int PHASE_2_ATTACK = 20;
    public static final int PHASE_3_ATTACK = 25;

    // Special ability damage values
    public static final int PHASE_2_SPECIAL = 22;
    public static final int PHASE_3_SPECIAL = 28;

    public static final double PHASE_2_THRESHOLD_RATIO = Phase.PHASE_II_AWAKENING.getThresholdRatio();
    public static final double PHASE_3_THRESHOLD_RATIO = Phase.PHASE_III_CATACLYSM.getThresholdRatio();

    private Phase currentPhase = Phase.PHASE_I_GUARDIAN;
    private boolean phase2Triggered = false;
    private boolean phase3Triggered = false;

    public RiftWarden() {
        this(DEFAULT_NAME, DEFAULT_MAX_HP);
    }

    public RiftWarden(String name, int maxHp) {
        super(name, maxHp, PHASE_1_ATTACK, 16, Behaviour.TACTICAL);
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public boolean isPhase2Triggered() {
        return phase2Triggered;
    }

    public boolean isPhase3Triggered() {
        return phase3Triggered;
    }

    @Override
    public int getBaseAttack() {
        return switch (currentPhase) {
            case PHASE_I_GUARDIAN -> PHASE_1_ATTACK;
            case PHASE_II_AWAKENING -> PHASE_2_ATTACK;
            case PHASE_III_CATACLYSM -> PHASE_3_ATTACK;
        };
    }

    @Override
    public int getSpecialAttackDamage() {
        return switch (currentPhase) {
            case PHASE_I_GUARDIAN -> PHASE_1_ATTACK;
            case PHASE_II_AWAKENING -> PHASE_2_SPECIAL;
            case PHASE_III_CATACLYSM -> PHASE_3_SPECIAL;
        };
    }

    @Override
    public String getAttackMessage() {
        return switch (currentPhase) {
            case PHASE_I_GUARDIAN -> getName() + " swings its ancient runic halberd!";
            case PHASE_II_AWAKENING -> getName() + " strikes with awakened abyssal force!";
            case PHASE_III_CATACLYSM -> getName() + " cleaves through spacetime with cataclysmic fury!";
        };
    }

    @Override
    public String getSpecialMessage() {
        return switch (currentPhase) {
            case PHASE_I_GUARDIAN -> getName() + " channels a heavy guard strike!";
            case PHASE_II_AWAKENING -> getName() + " opens a dark vortex: VOID RIFT BLAST!";
            case PHASE_III_CATACLYSM -> getName() + " unbinds catastrophic energy: CATACLYSMIC RUPTURE!";
        };
    }

    /**
     * Inspects health threshold crossings deterministically.
     * Transitions trigger at most once and produce explicit phase change events.
     * If an attack crosses multiple thresholds simultaneously, both transitions
     * are recorded in order without duplicating or skipping state.
     */
    @Override
    public List<CombatEvent> checkPhaseTransitions() {
        if (!isAlive()) {
            return List.of();
        }

        List<CombatEvent> transitionEvents = new ArrayList<>();
        double hpRatio = (double) getCurrentHp() / getMaxHp();

        // Check Phase II transition (<= 65% HP)
        if (!phase2Triggered && hpRatio <= PHASE_2_THRESHOLD_RATIO) {
            phase2Triggered = true;
            currentPhase = Phase.PHASE_II_AWAKENING;
            transitionEvents.add(new CombatEvent(
                    CombatEvent.EventKind.PHASE_CHANGED,
                    getName(),
                    "",
                    2,
                    "*** BOSS PHASE II: RIFT AWAKENING! The Warden's armor fractures as abyssal energy erupts! ***"
            ));
        }

        // Check Phase III transition (<= 30% HP)
        if (!phase3Triggered && hpRatio <= PHASE_3_THRESHOLD_RATIO) {
            phase3Triggered = true;
            currentPhase = Phase.PHASE_III_CATACLYSM;
            transitionEvents.add(new CombatEvent(
                    CombatEvent.EventKind.PHASE_CHANGED,
                    getName(),
                    "",
                    3,
                    "*** BOSS PHASE III: CATACLYSM! Reality bends around the Warden as it channels pure destruction! ***"
            ));
        }

        return List.copyOf(transitionEvents);
    }

    @Override
    public Action decideAction(Hero hero, Random rng) {
        if (!isAlive()) {
            throw new IllegalStateException("Dead boss cannot decide actions");
        }
        Objects.requireNonNull(hero, "Hero cannot be null");
        Objects.requireNonNull(rng, "Random instance cannot be null");

        int roll = rng.nextInt(100);

        return switch (currentPhase) {
            case PHASE_I_GUARDIAN -> {
                // 60% Attack (15 dmg), 40% Defend
                yield (roll < 60) ? Action.ATTACK : Action.DEFEND;
            }
            case PHASE_II_AWAKENING -> {
                // 70% Attack (20 dmg), 20% Special (22 dmg), 10% Defend
                if (roll < 70) {
                    yield Action.ATTACK;
                } else if (roll < 90) {
                    yield Action.SPECIAL;
                } else {
                    yield Action.DEFEND;
                }
            }
            case PHASE_III_CATACLYSM -> {
                // 50% Special (28 dmg), 40% Attack (25 dmg), 10% Defend
                if (roll < 50) {
                    yield Action.SPECIAL;
                } else if (roll < 90) {
                    yield Action.ATTACK;
                } else {
                    yield Action.DEFEND;
                }
            }
        };
    }
}

