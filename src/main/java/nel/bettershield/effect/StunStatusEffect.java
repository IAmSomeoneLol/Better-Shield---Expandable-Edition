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

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) { return true; }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof PlayerEntity) {
            // --- 1.20.5 FIX: StatusEffects.SLOWNESS is already a RegistryEntry! ---
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 3, false, false, true));
        }
        return true;
    }
}