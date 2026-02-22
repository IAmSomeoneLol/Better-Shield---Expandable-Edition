package nel.bettershield.registry;

import com.mojang.serialization.Codec;
import nel.bettershield.Bettershield;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.Optional;

public class BetterShieldCriteria {

    public static final CustomTrigger PARRY = new CustomTrigger();
    public static final CustomTrigger SLAM_MULTI = new CustomTrigger();
    public static final CustomTrigger REFLECT_KILL = new CustomTrigger();
    public static final CustomTrigger SHIELD_THROW_HIT = new CustomTrigger();
    public static final CustomTrigger CAPTAIN_AMERICA = new CustomTrigger();

    public static void register() {
        // 1.21 FIX: Must register criteria into the main Registries object
        Registry.register(Registries.CRITERION, Identifier.of(Bettershield.MOD_ID, "parry"), PARRY);
        Registry.register(Registries.CRITERION, Identifier.of(Bettershield.MOD_ID, "slam_multi"), SLAM_MULTI);
        Registry.register(Registries.CRITERION, Identifier.of(Bettershield.MOD_ID, "reflect_kill"), REFLECT_KILL);
        Registry.register(Registries.CRITERION, Identifier.of(Bettershield.MOD_ID, "shield_throw_hit"), SHIELD_THROW_HIT);
        Registry.register(Registries.CRITERION, Identifier.of(Bettershield.MOD_ID, "captain_america"), CAPTAIN_AMERICA);
    }

    public static class CustomTrigger extends AbstractCriterion<CustomTrigger.Conditions> {
        @Override
        public Codec<Conditions> getConditionsCodec() {
            return Conditions.CODEC;
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, conditions -> true);
        }

        public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {
            public static final Codec<Conditions> CODEC = Codec.unit(new Conditions(Optional.empty()));
        }
    }
}