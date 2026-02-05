package nel.bettershield.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected ItemStack activeItemStack;
    @Shadow protected int itemUseTimeLeft;
    @Shadow public abstract int getItemUseTime();

    // Overwriting the logic that checks for the 5-tick delay
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void instantBlock(CallbackInfoReturnable<Boolean> cir) {
        if (this.activeItemStack.isOf(Items.SHIELD)) {
            // If we are using a shield, return TRUE immediately, ignoring the delay
            cir.setReturnValue(true);
        }
    }
}