package nel.bettershield.registry;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nel.bettershield.Bettershield;

public class BetterShieldCriteria {

    public static void register() {
    }

    public static void grantAdvancement(ServerPlayerEntity player, String advancementName) {
        if (player == null || player.getServer() == null) return;

        Identifier advancementId = Identifier.of(Bettershield.MOD_ID, "bettershield/" + advancementName);
        AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(advancementId);

        if (advancement != null) {
            // 1.21.5 FIX: Explicitly grant the "unlock" trigger!
            player.getAdvancementTracker().grantCriterion(advancement, "unlock");
        } else {
            // FIXED: Using .warning() for java.util.logging.Logger
            Bettershield.LOGGER.warning("Could not find advancement to grant: " + advancementId);
        }
    }
}