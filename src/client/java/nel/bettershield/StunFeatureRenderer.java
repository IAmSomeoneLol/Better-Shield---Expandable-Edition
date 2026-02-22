package nel.bettershield;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

public class StunFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    // 1.21 FIX: Identifier.of()
    private static final Identifier STAR_TEXTURE = Identifier.of("bettershield", "textures/particle/stun_star.png");

    public StunFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

        var stunEntry = Registries.STATUS_EFFECT.getEntry(Bettershield.STUN_EFFECT);
        if (entity.hasStatusEffect(stunEntry)) {

            Quaternionf cameraRotation = MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation();
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(STAR_TEXTURE));

            float time = (entity.age + tickDelta) * 0.4f;
            float y = entity.getEyeHeight(entity.getPose()) + 0.5f;

            for (int i = 0; i < 3; i++) {
                matrices.push();

                double offset = (Math.PI * 2 / 3) * i;
                double angle = time + offset;
                double radius = 0.6;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                matrices.translate(x, y, z);
                matrices.multiply(cameraRotation);
                renderStar(matrices, vertexConsumer, 0.25f, light);
                matrices.pop();
            }
        }
    }

    private void renderStar(MatrixStack matrices, VertexConsumer vertexConsumer, float size, int light) {
        MatrixStack.Entry entry = matrices.peek();
        int glowLight = 15728880;

        // 1.21 FIX: Removed .next() from vertex chain, it is done implicitly now
        vertexConsumer.vertex(entry, -size, -size, 0.0F).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.vertex(entry, size, -size, 0.0F).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.vertex(entry, size, size, 0.0F).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.vertex(entry, -size, size, 0.0F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry, 0.0F, 1.0F, 0.0F);
    }
}