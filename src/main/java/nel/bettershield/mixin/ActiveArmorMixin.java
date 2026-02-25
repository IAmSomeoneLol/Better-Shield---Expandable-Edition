package nel.bettershield.mixin;

import nel.bettershield.registry.BetterShieldEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class ActiveArmorMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float reduceDamageWithActiveArmor(float amount, ServerWorld serverWorld, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack offHand = entity.getOffHandStack();

        if (offHand.getItem() instanceof ShieldItem) {
            int level = EnchantmentHelper.getLevel(entity.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(BetterShieldEnchantments.ACTIVE_ARMOR), offHand);

            if (level > 0) {
                float reduction = level * 0.04f;
                return amount * (1.0f - reduction);
            }
        }
        return amount;
    }
}