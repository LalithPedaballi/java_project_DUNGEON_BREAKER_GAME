package com.riftarena.realtime;

/**
 * Progression stages for the Solo Leveling-inspired dungeon run.
 */
public enum DungeonStage {

    /**
     * Atmospheric entry sequence displaying dungeon name and primary objective.
     */
    ENTRANCE("THE ABYSSAL CRYPT", "Infiltrate the sunken depths and extinguish The Rift Warden"),

    /**
     * Wave 1: Vanguard of void skirmishers (melee Spawn + agile Skulker).
     */
    ENCOUNTER_ONE("VOID PATROL", "Defeat the forward patrol of void beasts"),

    /**
     * Transition state: First room cleared, ancient passage unsealing.
     */
    ENCOUNTER_ONE_CLEARED("AREA SECURED", "The gate to the inner catacombs unlocks"),

    /**
     * Wave 2: Reinforced shadow guard including the ranged Abyssal Caster.
     */
    ENCOUNTER_TWO("SHADOW GUARD", "Eliminate the shadow skirmishers and abyssal caster"),

    /**
     * Transition state: Inner sanctum seal breaking with atmospheric shift.
     */
    BOSS_GATE("ANCIENT SEAL BREAKING", "Dimensional barrier collapsing — Boss approaching!"),

    /**
     * Climax: Dedicated boss battle against The Rift Warden across multiple phases.
     */
    BOSS_ENCOUNTER("THE RIFT WARDEN", "Defeat the colossal Titan of the Dimensional Rift"),

    /**
     * Terminal victory state: all encounters and boss vanquished.
     */
    VICTORY("DUNGEON CLEARED", "The abyssal rift has been completely purged and sealed"),

    /**
     * Terminal defeat state: hero succumbed to dungeon hazards.
     */
    DEFEAT("FALLEN IN THE DEPTHS", "Your essence was consumed by the void");

    private final String title;
    private final String description;

    DungeonStage(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

