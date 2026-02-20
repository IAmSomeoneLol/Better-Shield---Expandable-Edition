package nel.bettershield.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class StunStatusEffect extends StatusEffect {
    public StunStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x5A5A5A);
    }

    // --- 1.20.2 FIX: AttributeContainer was removed from this specific method signature ---
    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        // KEEP: Slowness for Players
        // This makes it so they can't sprint away while stunned, but they can see.
        // showIcon = true is still required for the stars to render!
        if (entity instanceof PlayerEntity) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 3, false, false, true));
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
    }
}