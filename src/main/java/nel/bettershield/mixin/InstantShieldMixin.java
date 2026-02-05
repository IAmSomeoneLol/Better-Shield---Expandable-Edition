package nel.bettershield.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntity.class)
public abstract class InstantShieldMixin {

    // Vanilla code says: "If itemUseTime >= 5, then block."
    // We change that 5 to 0.
    @ModifyConstant(method = "isBlocking", constant = @Constant(intValue = 5))
    private int removeShieldDelay(int constant) {
        return 0;
    }
}