package nel.bettershield.mixin;

import nel.bettershield.registry.BetterShieldCriteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class ReflectKillMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (source.getSource() != null) {
            // FIX: use getCommandTags
            if (source.getSource().getCommandTags().contains("bettershield_reflected")) {

                if (source.getAttacker() instanceof ServerPlayerEntity player) {
                    BetterShieldCriteria.REFLECT_KILL.trigger(player);
                }
            }
        }
    }
}