package nel.bettershield.mixin;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(ArrowEntity.class)
public interface ArrowEntityAccessor {
    @Accessor("potion")
    Potion getPotion();

    @Accessor("effects")
    Set<StatusEffectInstance> getCustomEffects();

    @Invoker("setColor")
    void invokeSetColor(int color);
}