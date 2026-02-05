package nel.bettershield.mixin;

import nel.bettershield.Bettershield;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class StunMovementMixin {

    // 1. BRAIN FREEZE: Stop them from planning attacks or paths
    @Inject(method = "tick", at = @At("HEAD"))
    private void stunBrain(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.hasStatusEffect(Bettershield.STUN_EFFECT) && entity instanceof MobEntity mob) {
            // Stop pathfinding
            mob.getNavigation().stop();
            // Forget the target (Prevents melee swinging/biting)
            mob.setTarget(null);
        }
    }

    // 2. LEG FREEZE: Force "WASD" input to Zero
    // This catches Spiders, Slimes, and anything else that tries to override movement
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d freezeMovementInput(Vec3d movementInput) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // If stunned, replace their movement attempt with ZERO (0, 0, 0)
        if (entity.hasStatusEffect(Bettershield.STUN_EFFECT)) {
            // Exception: If it's a Player, we might want to let them move slowly (Slowness IV handling)
            // But if you want Players to be partially frozen too, keep this.
            // For now, let's strictly freeze MOBS, but let PLAYERS move with Slowness (defined in Effect file)
            if (!(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
                return Vec3d.ZERO;
            }
        }
        return movementInput;
    }
}