package nel.bettershield.client;

import nel.bettershield.entity.ThrownShieldEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class ThrownShieldEntityRenderer extends EntityRenderer<ThrownShieldEntity> {
    private final ItemRenderer itemRenderer;

    public ThrownShieldEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownShieldEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Spin the shield
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw(tickDelta) - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getPitch(tickDelta)));

        // Add a "spinning disk" effect
        float age = entity.age + tickDelta;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 30.0f));

        // Render the ItemStack
        this.itemRenderer.renderItem(entity.getStack(), ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), entity.getId());

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(ThrownShieldEntity entity) {
        return null; // ItemRenderer handles texture
    }
}