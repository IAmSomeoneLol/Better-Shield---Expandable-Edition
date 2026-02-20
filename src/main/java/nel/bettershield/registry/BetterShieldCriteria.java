package nel.bettershield.registry;

import com.google.gson.JsonObject;
import nel.bettershield.Bettershield;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class BetterShieldCriteria {

    // --- TRIGGERS ---
    public static CustomTrigger PARRY;
    public static CustomTrigger SLAM_MULTI;
    public static CustomTrigger REFLECT_KILL;
    public static CustomTrigger SHIELD_THROW_HIT;
    public static CustomTrigger CAPTAIN_AMERICA;

    public static void register() {
        // --- 1.20.2 FIX: Registration now handles the String ID and Object simultaneously ---
        PARRY = net.minecraft.advancement.criterion.Criteria.register(Bettershield.MOD_ID + ":parry", new CustomTrigger());
        SLAM_MULTI = net.minecraft.advancement.criterion.Criteria.register(Bettershield.MOD_ID + ":slam_multi", new CustomTrigger());
        REFLECT_KILL = net.minecraft.advancement.criterion.Criteria.register(Bettershield.MOD_ID + ":reflect_kill", new CustomTrigger());
        SHIELD_THROW_HIT = net.minecraft.advancement.criterion.Criteria.register(Bettershield.MOD_ID + ":shield_throw_hit", new CustomTrigger());
        CAPTAIN_AMERICA = net.minecraft.advancement.criterion.Criteria.register(Bettershield.MOD_ID + ":captain_america", new CustomTrigger());
    }

    // --- CUSTOM TRIGGER CLASS ---
    public static class CustomTrigger extends AbstractCriterion<CustomTrigger.Conditions> {

        // Note: getId() was removed by Mojang in 1.20.2, so we don't need it here anymore!

        @Override
        protected Conditions conditionsFromJson(JsonObject obj, Optional<LootContextPredicate> playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
            return new Conditions(playerPredicate); // Removed ID parameter
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, conditions -> true);
        }

        public static class Conditions extends AbstractCriterionConditions {
            public Conditions(Optional<LootContextPredicate> playerPredicate) {
                super(playerPredicate); // Removed ID parameter from superclass
            }
        }
    }
}