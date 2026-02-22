package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StunDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void nullifyDamageFromStunnedSource(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
            if (stunEntry != null && attacker.hasStatusEffect(stunEntry)) {
                cir.setReturnValue(false);
            }
        }
    }
}