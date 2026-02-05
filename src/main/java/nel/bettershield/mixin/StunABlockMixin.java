package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class StunABlockMixin {

    // 1. STOP DAMAGE (The Bite)
    // If they try to attack, we force it to fail immediately.
    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void stopBiting(Entity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity mob = (LivingEntity) (Object) this;

        if (mob.hasStatusEffect(Bettershield.STUN_EFFECT)) {
            cir.setReturnValue(false); // "Attack Failed"
        }
    }

    // 2. STOP TARGETING (The Leap)
    // Spiders leap because they have a target. If we block them from
    // ever setting a target, they will just sit there confused.
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void preventTargetLock(LivingEntity target, CallbackInfo ci) {
        LivingEntity mob = (LivingEntity) (Object) this;

        if (mob.hasStatusEffect(Bettershield.STUN_EFFECT)) {
            // If the AI tries to lock onto a player, we cancel it.
            if (target != null) {
                ci.cancel();
            }
        }
    }
}