package nel.bettershield.registry;

import com.mojang.serialization.Codec;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Optional;

public class BetterShieldCriteria {

    public static final CustomTrigger PARRY = Criteria.register("bettershield:parry", new CustomTrigger());
    public static final CustomTrigger SLAM_MULTI = Criteria.register("bettershield:slam_multi", new CustomTrigger());
    public static final CustomTrigger REFLECT_KILL = Criteria.register("bettershield:reflect_kill", new CustomTrigger());
    public static final CustomTrigger SHIELD_THROW_HIT = Criteria.register("bettershield:shield_throw_hit", new CustomTrigger());
    public static final CustomTrigger CAPTAIN_AMERICA = Criteria.register("bettershield:captain_america", new CustomTrigger());

    public static void register() {}

    public static class CustomTrigger extends AbstractCriterion<CustomTrigger.Conditions> {
        @Override
        public Codec<Conditions> getConditionsCodec() {
            return Codec.unit(new Conditions(Optional.empty()));
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, conditions -> true);
        }

        // --- 1.20.5 FIX: Implements AbstractCriterion.Conditions ---
        public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions { }
    }
}