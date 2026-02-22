package nel.bettershield.mixin;

import nel.bettershield.item.ModShieldItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class FireResistanceMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float reduceFireDamage(float amount, ServerWorld serverWorld, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof PlayerEntity player && source.isIn(DamageTypeTags.IS_FIRE)) {
            ItemStack offHand = player.getOffHandStack();
            if (offHand.getItem() instanceof ModShieldItem shield && shield.isFireResistant()) {
                return amount * 0.77f;
            }
        }
        return amount;
    }
}