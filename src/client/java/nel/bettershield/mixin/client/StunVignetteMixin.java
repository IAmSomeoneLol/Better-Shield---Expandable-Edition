package nel.bettershield.mixin.client;

import nel.bettershield.Bettershield;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity; // <--- THIS WAS MISSING!
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class StunVignetteMixin {

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void stunDarkness(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof ClientPlayerEntity player) {
            if (player.hasStatusEffect(Bettershield.STUN_EFFECT)) {
                // Return 0.0f to make it look dark/pulsing if we were using Night Vision logic,
                // but since we are relying on the "Hidden Darkness" effect we added earlier,
                // we mostly just need this class to exist and compile without errors.
            }
        }
    }
}