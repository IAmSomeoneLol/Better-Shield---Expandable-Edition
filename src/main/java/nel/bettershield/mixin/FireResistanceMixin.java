package nel.bettershield.mixin;

import nel.bettershield.item.ModShieldItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class FireResistanceMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float reduceFireDamage(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Check if entity is a player and source is Fire (Lava, Fire, Magma, etc.)
        if (entity instanceof PlayerEntity player && source.isIn(DamageTypeTags.IS_FIRE)) {

            // Check Off-Hand for Shield (as specified in your request)
            ItemStack offHand = player.getOffHandStack();
            if (offHand.getItem() instanceof ModShieldItem shield && shield.isFireResistant()) {
                // Reduce damage by 23% -> Take 77% of original damage
                return amount * 0.77f;
            }
        }
        return amount;
    }
}