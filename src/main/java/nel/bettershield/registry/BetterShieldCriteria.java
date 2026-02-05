package nel.bettershield.registry;

import com.google.gson.JsonObject;
import nel.bettershield.Bettershield;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate; // Correct Import for 1.20.1
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class BetterShieldCriteria {

    // --- TRIGGERS ---
    public static final CustomTrigger PARRY = new CustomTrigger(new Identifier(Bettershield.MOD_ID, "parry"));
    public static final CustomTrigger SLAM_MULTI = new CustomTrigger(new Identifier(Bettershield.MOD_ID, "slam_multi"));
    public static final CustomTrigger REFLECT_KILL = new CustomTrigger(new Identifier(Bettershield.MOD_ID, "reflect_kill"));
    public static final CustomTrigger SHIELD_THROW_HIT = new CustomTrigger(new Identifier(Bettershield.MOD_ID, "shield_throw_hit"));
    public static final CustomTrigger CAPTAIN_AMERICA = new CustomTrigger(new Identifier(Bettershield.MOD_ID, "captain_america"));

    public static void register() {
        net.minecraft.advancement.criterion.Criteria.register(PARRY);
        net.minecraft.advancement.criterion.Criteria.register(SLAM_MULTI);
        net.minecraft.advancement.criterion.Criteria.register(REFLECT_KILL);
        net.minecraft.advancement.criterion.Criteria.register(SHIELD_THROW_HIT);
        net.minecraft.advancement.criterion.Criteria.register(CAPTAIN_AMERICA);
    }

    // --- CUSTOM TRIGGER CLASS ---
    public static class CustomTrigger extends AbstractCriterion<CustomTrigger.Conditions> {
        private final Identifier id;

        public CustomTrigger(Identifier id) {
            this.id = id;
        }

        @Override
        public Identifier getId() {
            return id;
        }

        @Override
        protected Conditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
            return new Conditions(id, playerPredicate);
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, conditions -> true);
        }

        public static class Conditions extends AbstractCriterionConditions {
            public Conditions(Identifier id, LootContextPredicate playerPredicate) {
                super(id, playerPredicate);
            }
        }
    }
}