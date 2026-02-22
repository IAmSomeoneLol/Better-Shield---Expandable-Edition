package nel.bettershield.registry;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nel.bettershield.Bettershield;

public class BetterShieldCriteria {

    public static void register() {
        // Handled via impossible triggers in 1.21 to prevent deadlocks
    }

    public static void grantAdvancement(ServerPlayerEntity player, String advancementName) {
        try {
            if (player == null || player.getServer() == null) return;

            Identifier advancementId = Identifier.of(Bettershield.MOD_ID, "bettershield/" + advancementName);
            AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(advancementId);

            if (advancement != null) {
                var tracker = player.getAdvancementTracker();
                for (String criterion : advancement.value().criteria().keySet()) {
                    tracker.grantCriterion(advancement, criterion);
                }
            }
        } catch (Exception e) {
            // 1.21 FIX: Use System out to completely bypass Logger import issues
            System.err.println("[BetterShield] Silently failed to grant advancement: " + advancementName);
        }
    }
}