package nel.bettershield.client;

import nel.bettershield.registry.BetterShieldItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes; // --- NEW 1.20.5 COMPONENT ---
import net.minecraft.component.type.BannerPatternsComponent; // --- NEW 1.20.5 COMPONENT ---
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModShieldRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private ShieldEntityModel model;

    private static final SpriteIdentifier DIAMOND_SHIELD_BASE = new SpriteIdentifier(
            TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, new Identifier("bettershield", "entity/diamond_shield_base"));
    private static final SpriteIdentifier DIAMOND_SHIELD_NO_PATTERN = new SpriteIdentifier(
            TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, new Identifier("bettershield", "entity/diamond_shield_base_nopattern"));

    private static final SpriteIdentifier NETHERITE_SHIELD_BASE = new SpriteIdentifier(
            TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, new Identifier("bettershield", "entity/netherite_shield_base"));
    private static final SpriteIdentifier NETHERITE_SHIELD_NO_PATTERN = new SpriteIdentifier(
            TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, new Identifier("bettershield", "entity/netherite_shield_base_nopattern"));

    public ModShieldRenderer() {
    }

    private void initModel() {
        if (this.model == null) {
            this.model = new ShieldEntityModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(EntityModelLayers.SHIELD));
        }
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        initModel();

        boolean isNetherite = stack.isOf(BetterShieldItems.NETHERITE_SHIELD);

        // --- 1.20.5 FIX: Checking for Base Color component instead of NBT ---
        boolean hasBanner = stack.contains(DataComponentTypes.BASE_COLOR);

        matrices.push();
        matrices.scale(1.0F, -1.0F, -1.0F);

        SpriteIdentifier spriteId;
        if (isNetherite) {
            spriteId = hasBanner ? NETHERITE_SHIELD_BASE : NETHERITE_SHIELD_NO_PATTERN;
        } else {
            spriteId = hasBanner ? DIAMOND_SHIELD_BASE : DIAMOND_SHIELD_NO_PATTERN;
        }

        VertexConsumer vertexConsumer = spriteId.getSprite().getTextureSpecificVertexConsumer(
                ItemRenderer.getDirectItemGlintConsumer(
                        vertexConsumers, this.model.getLayer(spriteId.getAtlasId()), true, stack.hasGlint()));

        this.model.getHandle().render(matrices, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        if (hasBanner) {
            // --- 1.20.5 FIX: Component Magic instead of decoding NBT lists ---
            DyeColor baseColor = stack.get(DataComponentTypes.BASE_COLOR);
            BannerPatternsComponent patterns = stack.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);

            BannerBlockEntityRenderer.renderCanvas(
                    matrices, vertexConsumers, light, overlay, this.model.getPlate(), spriteId, false,
                    baseColor != null ? baseColor : DyeColor.WHITE, patterns, stack.hasGlint()
            );
        } else {
            this.model.getPlate().render(matrices, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        matrices.pop();
    }
}