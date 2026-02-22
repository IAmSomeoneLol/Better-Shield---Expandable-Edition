package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class StunABlockMixin {

    // 1.21.2 FIX: ServerWorld is now the first parameter!
    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void stopBiting(ServerWorld world, Entity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity mob = (LivingEntity) (Object) this;
        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
        if (stunEntry != null && mob.hasStatusEffect(stunEntry)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void preventTargetLock(LivingEntity target, CallbackInfo ci) {
        LivingEntity mob = (LivingEntity) (Object) this;
        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
        if (stunEntry != null && mob.hasStatusEffect(stunEntry)) {
            if (target != null) {
                ci.cancel();
            }
        }
    }
}