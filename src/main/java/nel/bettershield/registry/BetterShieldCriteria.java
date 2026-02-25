package nel.bettershield.registry;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import nel.bettershield.Bettershield;

public class BetterShieldCriteria {

    public static void register() {
    }

    public static void grantAdvancement(ServerPlayerEntity player, String advancementName) {
        if (player == null || ((ServerWorld) player.getEntityWorld()).getServer() == null) return;

        Identifier advancementId = Identifier.of(Bettershield.MOD_ID, "bettershield/" + advancementName);
        AdvancementEntry advancement = ((ServerWorld) player.getEntityWorld()).getServer().getAdvancementLoader().get(advancementId);

        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "unlock");
        } else {
            Bettershield.LOGGER.warning("Could not find advancement to grant: " + advancementId);
        }
    }
}