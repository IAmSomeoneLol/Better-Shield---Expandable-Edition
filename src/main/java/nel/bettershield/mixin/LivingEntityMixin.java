package nel.bettershield.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract ItemStack getActiveItem();

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void instantBlock(CallbackInfoReturnable<Boolean> cir) {
        if (this.getActiveItem().getItem() instanceof ShieldItem) {
            cir.setReturnValue(true);
        }
    }
}