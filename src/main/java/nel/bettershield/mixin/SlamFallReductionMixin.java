package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public abstract class SlamFallReductionMixin {

    // 1.21.5 FIX: fallDistance is a double now! We must return and accept a double.
    @ModifyVariable(method = "handleFallDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double reduceFallDamage(double fallDistance) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Check if SLAM_COOLDOWN exists
        if (Bettershield.SLAM_COOLDOWN.containsKey(player.getUuid())) {
            long cooldownEnd = Bettershield.SLAM_COOLDOWN.get(player.getUuid());
            long now = player.getWorld().getTime();

            // If the cooldown was set recently (less than 1 second ago),
            // it means we just finished a Slam.
            // (Cooldown is 60 ticks. If 50+ ticks remain, we are fresh).
            if (cooldownEnd - now > 50) {
                // Reduce Fall Damage by 25%
                return fallDistance * 0.75;
            }
        }

        return fallDistance;
    }
}