package nel.bettershield.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.RegistryKeys;
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

        if (this.isBlocking() && this.getActiveItem().getItem() instanceof ShieldItem) {
            var registry = player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            int level = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.SWIFT_SNEAK), player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS));

            if (level > 0) {
                double baseSpeed = 0.2;
                double targetSpeed = baseSpeed + (level * 0.18);
                double multiplier = targetSpeed / baseSpeed;
                return input.multiply(multiplier, 1.0, multiplier);
            }
        }
        return input;
    }
}