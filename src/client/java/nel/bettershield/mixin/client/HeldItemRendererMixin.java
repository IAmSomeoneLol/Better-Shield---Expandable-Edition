package nel.bettershield.mixin.client;

import nel.bettershield.BettershieldClient;
import nel.bettershield.registry.BetterShieldItems;
import net.minecraft.client.render.command.OrderedRenderCommandQueue; // 1.21.9 FIX: New Rendering Queue
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    // 1.21.9 FIX: Updated method signature to match the new OrderedRenderCommandQueue
    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("HEAD"))
    private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, CallbackInfo ci) {

        boolean isFirstPerson = (renderMode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || renderMode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);

        boolean isVanilla = stack.isOf(Items.SHIELD);
        boolean isDiamond = stack.isOf(BetterShieldItems.DIAMOND_SHIELD);
        boolean isNetherite = stack.isOf(BetterShieldItems.NETHERITE_SHIELD);

        if (isFirstPerson && (isVanilla || isDiamond || isNetherite) && BettershieldClient.isChargingThrow) {
            float maxCharge = 20.0f;
            float ratio = Math.min(1.0f, (BettershieldClient.chargeTicks) / maxCharge);

            if (ratio > 0.1f) {
                if (ratio > 0.8f) {
                    float shake = (float)Math.sin(System.currentTimeMillis() / 100.0) * 0.02f;
                    matrices.translate(shake, shake, 0);
                }

                float tilt = ratio * 35.0f;
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(tilt * 0.5f));
                matrices.translate(0.0, -0.1 * ratio, 0.1 * ratio);
            }
        }
    }
}