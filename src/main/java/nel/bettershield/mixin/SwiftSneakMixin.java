package nel.bettershield.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem; // Import ShieldItem Class
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class SwiftSneakMixin {

    @Shadow public abstract boolean isBlocking();
    @Shadow public abstract ItemStack getActiveItem();

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d modifyMovementInput(Vec3d input) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof PlayerEntity player)) {
            return input;
        }

        // FIX: Check "instanceof ShieldItem" instead of "isOf(Items.SHIELD)"
        // This ensures it works for Diamond Shield, Netherite Shield, and Vanilla Shield.
        if (this.isBlocking() && this.getActiveItem().getItem() instanceof ShieldItem) {

            int level = EnchantmentHelper.getEquipmentLevel(Enchantments.SWIFT_SNEAK, player);

            if (level > 0) {
                // Vanilla Blocking Speed = 0.2
                // +0.18 per level
                double baseSpeed = 0.2;
                double targetSpeed = baseSpeed + (level * 0.18);

                double multiplier = targetSpeed / baseSpeed;

                return input.multiply(multiplier, 1.0, multiplier);
            }
        }

        return input;
    }
}