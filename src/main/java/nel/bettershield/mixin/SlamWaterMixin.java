package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class SlamWaterMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Run on Server only
        if (!player.getWorld().isClient) {
            // Check if this player is currently in a "Slam" state
            if (Bettershield.SLAM_START_Y.containsKey(player.getUuid())) {

                // If they touch water at any point during the slam...
                if (player.isTouchingWater()) {
                    // ... Cancel the slam immediately.
                    Bettershield.SLAM_START_Y.remove(player.getUuid());

                    // Optional: Play a "fizz" sound to indicate cancellation
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }
        }
    }
}