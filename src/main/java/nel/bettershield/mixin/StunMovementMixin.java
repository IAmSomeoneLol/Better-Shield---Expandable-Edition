package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class StunMovementMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void stunBrain(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
        if (entity.hasStatusEffect(stunEntry) && entity instanceof MobEntity mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d freezeMovementInput(Vec3d movementInput) {
        LivingEntity entity = (LivingEntity) (Object) this;
        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
        if (entity.hasStatusEffect(stunEntry)) {
            if (!(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
                return Vec3d.ZERO;
            }
        }
        return movementInput;
    }
}