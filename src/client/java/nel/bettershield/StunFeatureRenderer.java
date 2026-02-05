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
import net.minecraft.util.Identifier;
import org.joml.Quaternionf; // Uses JOML for rotation

public class StunFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

    // Pointing to your PARTICLE folder since you moved it there
    private static final Identifier STAR_TEXTURE = new Identifier("bettershield", "textures/particle/stun_star.png");

    public StunFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // This check now works for Mobs because showIcon=true!
        if (entity.hasStatusEffect(Bettershield.STUN_EFFECT)) {

            Quaternionf cameraRotation = MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation();
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(STAR_TEXTURE));

            // Animation Time (Fast Spin)
            float time = (entity.age + tickDelta) * 0.4f;

            // Height: Eye Height + 0.5 blocks (Just above forehead)
            // This relies on the entity's defined eye height, which is always correct (never at feet).
            float y = entity.getEyeHeight(entity.getPose()) + 0.5f;

            for (int i = 0; i < 3; i++) {
                matrices.push();

                // 1. ORBIT MATH (Manual Calculation)
                // We calculate the X and Z offset manually using Sin/Cos
                double offset = (Math.PI * 2 / 3) * i;
                double angle = time + offset;
                double radius = 0.6;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                // 2. MOVE TO POSITION
                // Since FeatureRenderer starts at the entity's feet (0,0,0),
                // we just translate up to the head (y) and out to the ring (x, z).
                matrices.translate(x, y, z);

                // 3. FACE CAMERA (Billboard)
                matrices.multiply(cameraRotation);

                // 4. DRAW
                renderStar(matrices, vertexConsumer, 0.25f, light);

                matrices.pop();
            }
        }
    }

    private void renderStar(MatrixStack matrices, VertexConsumer vertexConsumer, float size, int light) {
        MatrixStack.Entry entry = matrices.peek();

        // Full Brightness (Glows in Dark)
        int glowLight = 15728880;

        vertexConsumer.vertex(entry.getPositionMatrix(), -size, -size, 0.0F).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F).next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, -size, 0.0F).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F).next();
        vertexConsumer.vertex(entry.getPositionMatrix(), size, size, 0.0F).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F).next();
        vertexConsumer.vertex(entry.getPositionMatrix(), -size, size, 0.0F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(glowLight).normal(entry.getNormalMatrix(), 0.0F, 1.0F, 0.0F).next();
    }
}