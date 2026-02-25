package nel.bettershield.mixin.client;

import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldModelRenderer.class)
public class ShieldTextureMixin {
    private static final ThreadLocal<String> CURRENT_SHIELD = ThreadLocal.withInitial(() -> "");

    // 1.21.9 FIX: Added the missing 'int' parameter (likely an animation tick/frame value) right before CallbackInfo!
    @Inject(method = "render", at = @At("HEAD"))
    private void captureShieldType(ComponentMap components, ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, int overlay, boolean glint, int extraInt, CallbackInfo ci) {
        if (components != null && components.contains(DataComponentTypes.ITEM_NAME)) {
            CURRENT_SHIELD.set(components.get(DataComponentTypes.ITEM_NAME).getString().toLowerCase());
        } else {
            CURRENT_SHIELD.set("");
        }
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private SpriteIdentifier swapTexture(SpriteIdentifier original) {
        String shield = CURRENT_SHIELD.get();
        if (shield.contains("diamond")) {
            return new SpriteIdentifier(TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Identifier.of("bettershield", "entity/diamond_shield_base_nopattern"));
        }
        else if (shield.contains("netherite")) {
            return new SpriteIdentifier(TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Identifier.of("bettershield", "entity/netherite_shield_base_nopattern"));
        }
        return original;
    }
}