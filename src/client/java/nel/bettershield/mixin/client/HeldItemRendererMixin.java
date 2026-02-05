package nel.bettershield.mixin.client;

import nel.bettershield.BettershieldClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void onRenderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {

        // 1. Check if we are rendering a Shield in First Person
        boolean isFirstPerson = (renderMode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || renderMode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND);

        if (isFirstPerson && stack.isOf(Items.SHIELD) && BettershieldClient.isChargingThrow) {

            // 2. Calculate Charge Ratio
            float maxCharge = 20.0f;
            float ratio = Math.min(1.0f, (BettershieldClient.chargeTicks) / maxCharge);

            if (ratio > 0.1f) {
                // 3. Apply "Wind Up" Animation

                // Shake at max charge
                if (ratio > 0.8f) {
                    float shake = (float)Math.sin(System.currentTimeMillis() / 100.0) * 0.02f;
                    matrices.translate(shake, shake, 0);
                }

                // Tilt Back (Winding up the arm)
                float tilt = ratio * 35.0f;
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(tilt * 0.5f));

                // Move slightly down and out
                matrices.translate(0.0, -0.1 * ratio, 0.1 * ratio);
            }
        }
    }
}