package com.riftarena.ui.render;

import com.riftarena.game.CombatEvent;
import com.riftarena.game.RiftWarden;
import com.riftarena.ui.Theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sequenced presentation queue translating synchronous {@link CombatEvent} lists
 * into smooth, ordered visual combat animations, hit reactions, and floating numbers.
 *
 * Fully preserves model-view separation:
 *   - The combat engine calculates everything instantly and authoritatively.
 *   - The presentation queue only coordinates visual delays, character states, and effects.
 *   - Can be fast-forwarded or completed instantly via {@link #finishImmediate()}.
 *
 * Syllabus concepts demonstrated:
 *   - Queue data structures and state machines.
 *   - Decoupled event presentation without blocking the Swing Event Dispatch Thread.
 */
public class PresentationQueue {

    /**
     * Interface through which the queue directs the arena view during playback.
     */
    public interface ArenaDirector {
        void setHeroState(AnimationState state);
        void setEnemyState(AnimationState state);
        void setHeroProgress(float progress);
        void setEnemyProgress(float progress);
        void spawnDamageOnHero(int amount, boolean isCrit);
        void spawnDamageOnEnemy(int amount, boolean isCrit);
        void spawnHealOnHero(int amount);
        void spawnHealOnEnemy(int amount);
        void spawnShieldOnHero();
        void spawnShieldOnEnemy();
        void spawnHeroAttackEffect(String heroName, boolean isSpecial);
        void spawnEnemyAttackEffect(boolean isBoss, int phaseNumber, boolean isSpecial);
        void spawnPhaseTransition(int phaseNumber);
        void onBattleConcluded(boolean heroVictory);
    }

    private final ArenaDirector director;
    private final List<CombatEvent> pendingEvents = new ArrayList<>();
    private Runnable onCompleteCallback;

    private boolean isPlaying = false;
    private float stepTimer = 0.0f;
    private int currentStepIndex = 0;
    private List<PresentationStep> steps = new ArrayList<>();

    private record PresentationStep(
            float duration,
            Runnable onStart,
            Runnable onImpact,
            Runnable onEnd
    ) {}

    public PresentationQueue(ArenaDirector director) {
        this.director = Objects.requireNonNull(director, "ArenaDirector cannot be null");
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Begins sequenced visual playback for a batch of combat events.
     */
    public void playEvents(List<CombatEvent> events, String heroName, boolean isBoss, Runnable onComplete) {
        this.pendingEvents.clear();
        if (events != null) {
            this.pendingEvents.addAll(events);
        }
        this.onCompleteCallback = onComplete;
        this.steps = buildSteps(pendingEvents, heroName, isBoss);
        this.currentStepIndex = 0;
        this.stepTimer = 0.0f;
        this.isPlaying = true;

        if (steps.isEmpty()) {
            finishImmediate();
            return;
        }

        // Start first step
        steps.get(0).onStart().run();
    }

    /**
     * Updates playback timers (typically called from the 60 FPS Swing timer).
     */
    public void update(float deltaSeconds) {
        if (!isPlaying || steps.isEmpty()) {
            return;
        }

        stepTimer += deltaSeconds;
        PresentationStep currentStep = steps.get(currentStepIndex);

        // Mid-point impact trigger (~halfway through animation)
        float impactThreshold = currentStep.duration * 0.45f;
        if (stepTimer >= impactThreshold && (stepTimer - deltaSeconds) < impactThreshold) {
            currentStep.onImpact().run();
        }

        // Normalize progress [0, 1] for active step
        float progress = Math.min(1.0f, stepTimer / currentStep.duration);
        director.setHeroProgress(progress);
        director.setEnemyProgress(progress);

        if (stepTimer >= currentStep.duration) {
            currentStep.onEnd().run();
            currentStepIndex++;
            stepTimer = 0.0f;

            if (currentStepIndex < steps.size()) {
                steps.get(currentStepIndex).onStart().run();
            } else {
                finishImmediate();
            }
        }
    }

    /**
     * Immediately finishes all pending animations, snapping characters to their final
     * resting poses and firing completion callbacks. Used during rapid clicks and automated tests.
     */
    public void finishImmediate() {
        if (!isPlaying && steps.isEmpty()) {
            if (onCompleteCallback != null) {
                Runnable cb = onCompleteCallback;
                onCompleteCallback = null;
                cb.run();
            }
            return;
        }

        // Execute any unreached impacts or terminal steps
        for (int i = currentStepIndex; i < steps.size(); i++) {
            PresentationStep step = steps.get(i);
            if (i > currentStepIndex) {
                step.onStart().run();
            }
            step.onImpact().run();
            step.onEnd().run();
        }

        isPlaying = false;
        steps.clear();
        currentStepIndex = 0;
        stepTimer = 0.0f;

        if (onCompleteCallback != null) {
            Runnable cb = onCompleteCallback;
            onCompleteCallback = null;
            cb.run();
        }
    }

    // ------------------------------------------------------------------
    // Step Generation from Authoritative Events
    // ------------------------------------------------------------------

    private List<PresentationStep> buildSteps(List<CombatEvent> events, String heroName, boolean isBoss) {
        List<PresentationStep> sequence = new ArrayList<>();
        boolean battleConcluded = events.stream().anyMatch(e -> e.kind() == CombatEvent.EventKind.BATTLE_ENDED);

        for (int i = 0; i < events.size(); i++) {
            CombatEvent event = events.get(i);

            switch (event.kind()) {
                case ACTION_USED -> {
                    boolean isHero = isHeroEvent(event, heroName);
                    boolean isSpecial = event.message().toLowerCase().contains("special")
                            || event.message().toLowerCase().contains("shield slam")
                            || event.message().toLowerCase().contains("arcane nova")
                            || event.message().toLowerCase().contains("shadow strike")
                            || event.message().toLowerCase().contains("void rift")
                            || event.message().toLowerCase().contains("cataclysmic");

                    // Find corresponding consequence (DAMAGE_DEALT or HEALED or DEFENSE_RAISED)
                    CombatEvent consequence = findNextConsequence(events, i + 1);

                    if (isHero) {
                        sequence.add(createHeroActionStep(heroName, isSpecial, consequence, battleConcluded));
                    } else {
                        sequence.add(createEnemyActionStep(isBoss, isSpecial, consequence, battleConcluded));
                    }
                }
                case PHASE_CHANGED -> {
                    sequence.add(createPhaseTransitionStep(event.value()));
                }
                case BATTLE_ENDED -> {
                    boolean victory = event.message().toLowerCase().contains("victory")
                            || event.message().toLowerCase().contains("slain")
                            || event.message().toLowerCase().contains("vanquished")
                            || event.message().toLowerCase().contains("collapsed");
                    sequence.add(createBattleEndStep(victory));
                }
                default -> {
                    // Handled within action consequence
                }
            }
        }

        return sequence;
    }

    private boolean isHeroEvent(CombatEvent event, String heroName) {
        if (event.sourceName() != null && event.sourceName().equalsIgnoreCase(heroName)) {
            return true;
        }
        return "Hero".equalsIgnoreCase(event.sourceName());
    }

    private CombatEvent findNextConsequence(List<CombatEvent> events, int startIndex) {
        for (int i = startIndex; i < events.size(); i++) {
            CombatEvent e = events.get(i);
            if (e.kind() == CombatEvent.EventKind.DAMAGE_DEALT
                    || e.kind() == CombatEvent.EventKind.HEALED
                    || e.kind() == CombatEvent.EventKind.DEFENSE_RAISED) {
                return e;
            }
            if (e.kind() == CombatEvent.EventKind.ACTION_USED) {
                break; // Stop at next action
            }
        }
        return null;
    }

    private PresentationStep createHeroActionStep(String heroName, boolean isSpecial, CombatEvent consequence,
                                                  boolean battleConcluded) {
        float duration = isSpecial ? 0.45f : 0.35f;

        return new PresentationStep(
                duration,
                () -> {
                    AnimationState actState = "Mage".equalsIgnoreCase(heroName)
                            ? AnimationState.CASTING : AnimationState.ATTACKING;
                    director.setHeroState(actState);
                },
                () -> {
                    director.spawnHeroAttackEffect(heroName, isSpecial);
                    if (consequence != null) {
                        if (consequence.kind() == CombatEvent.EventKind.DAMAGE_DEALT) {
                            director.setEnemyState(AnimationState.HURT);
                            director.spawnDamageOnEnemy(consequence.value(), isSpecial);
                        } else if (consequence.kind() == CombatEvent.EventKind.HEALED) {
                            director.spawnHealOnHero(consequence.value());
                        } else if (consequence.kind() == CombatEvent.EventKind.DEFENSE_RAISED) {
                            director.setHeroState(AnimationState.DEFENDING);
                            director.spawnShieldOnHero();
                        }
                    }
                },
                () -> {
                    if (!battleConcluded) {
                        if (consequence != null && consequence.kind() == CombatEvent.EventKind.DEFENSE_RAISED) {
                            director.setHeroState(AnimationState.DEFENDING);
                        } else {
                            director.setHeroState(AnimationState.IDLE);
                        }
                        director.setEnemyState(AnimationState.IDLE);
                    }
                }
        );
    }

    private PresentationStep createEnemyActionStep(boolean isBoss, boolean isSpecial, CombatEvent consequence,
                                                   boolean battleConcluded) {
        float duration = isSpecial ? 0.45f : 0.35f;

        return new PresentationStep(
                duration,
                () -> {
                    director.setEnemyState(AnimationState.ATTACKING);
                },
                () -> {
                    director.spawnEnemyAttackEffect(isBoss, 1, isSpecial);
                    if (consequence != null) {
                        if (consequence.kind() == CombatEvent.EventKind.DAMAGE_DEALT) {
                            director.setHeroState(AnimationState.HURT);
                            director.spawnDamageOnHero(consequence.value(), isSpecial);
                        } else if (consequence.kind() == CombatEvent.EventKind.HEALED) {
                            director.spawnHealOnEnemy(consequence.value());
                        } else if (consequence.kind() == CombatEvent.EventKind.DEFENSE_RAISED) {
                            director.setEnemyState(AnimationState.DEFENDING);
                            director.spawnShieldOnEnemy();
                        }
                    }
                },
                () -> {
                    if (!battleConcluded) {
                        if (consequence != null && consequence.kind() == CombatEvent.EventKind.DEFENSE_RAISED) {
                            director.setEnemyState(AnimationState.DEFENDING);
                        } else {
                            director.setEnemyState(AnimationState.IDLE);
                        }
                        director.setHeroState(AnimationState.IDLE);
                    }
                }
        );
    }

    private PresentationStep createPhaseTransitionStep(int phaseNumber) {
        return new PresentationStep(
                0.55f,
                () -> {
                    director.setEnemyState(AnimationState.CASTING);
                    director.spawnPhaseTransition(phaseNumber);
                },
                () -> {},
                () -> {
                    director.setEnemyState(AnimationState.IDLE);
                }
        );
    }

    private PresentationStep createBattleEndStep(boolean victory) {
        Runnable applyEnd = () -> {
            if (victory) {
                director.setHeroState(AnimationState.VICTORY);
                director.setEnemyState(AnimationState.DEFEAT);
            } else {
                director.setHeroState(AnimationState.DEFEAT);
                director.setEnemyState(AnimationState.VICTORY);
            }
            director.onBattleConcluded(victory);
        };

        return new PresentationStep(
                0.40f,
                applyEnd,
                applyEnd,
                applyEnd
        );
    }
}
