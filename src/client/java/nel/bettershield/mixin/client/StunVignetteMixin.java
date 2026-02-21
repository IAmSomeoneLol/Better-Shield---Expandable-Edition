package nel.bettershield.mixin.client;

import nel.bettershield.Bettershield;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class StunVignetteMixin {

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void stunDarkness(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof ClientPlayerEntity player) {
            // --- 1.20.5 FIX: Wrap in RegistryEntry ---
            var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
            if (player.hasStatusEffect(stunEntry)) {
                // Return 0.0f to make it look dark/pulsing if we were using Night Vision logic
            }
        }
    }
}