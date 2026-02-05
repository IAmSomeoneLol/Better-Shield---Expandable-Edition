package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StunDamageMixin {

    // Runs whenever ANY living entity (Player, Mob, Animal) is about to take damage
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void nullifyDamageFromStunnedSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {

        // Check who is attacking
        if (source.getAttacker() instanceof LivingEntity attacker) {

            // If the ATTACKER is stunned, they cannot deal damage.
            if (attacker.hasStatusEffect(Bettershield.STUN_EFFECT)) {
                cir.setReturnValue(false); // Cancel the damage completely
            }
        }
    }
}