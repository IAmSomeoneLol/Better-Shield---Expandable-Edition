package nel.bettershield.client;

import nel.bettershield.entity.ThrownShieldEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.RotationAxis;

public class ThrownShieldEntityRenderer extends EntityRenderer<ThrownShieldEntity, ThrownShieldEntityRenderer.ShieldRenderState> {
    private final ItemRenderer itemRenderer;

    public ThrownShieldEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
    }

    @Override
    public ShieldRenderState createRenderState() {
        return new ShieldRenderState();
    }

    @Override
    public void updateRenderState(ThrownShieldEntity entity, ShieldRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.stack = entity.getStack();
        state.spin = entity.age + tickDelta;
    }

    @Override
    public void render(ShieldRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.spin * 30.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.translate(0, -0.2, 0);

        this.itemRenderer.renderItem(state.stack, ItemDisplayContext.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, null, 0);

        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }

    public static class ShieldRenderState extends EntityRenderState {
        public ItemStack stack = ItemStack.EMPTY;
        public float spin;
    }
}