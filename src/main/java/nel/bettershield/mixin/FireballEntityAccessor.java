package nel.bettershield.mixin;

import net.minecraft.entity.projectile.ExplosiveProjectileEntity; // CHANGED TARGET
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplosiveProjectileEntity.class)
public interface FireballEntityAccessor {
    @Accessor("powerX")
    void setPowerX(double powerX);

    @Accessor("powerY")
    void setPowerY(double powerY);

    @Accessor("powerZ")
    void setPowerZ(double powerZ);

    @Accessor("powerX")
    double getPowerX();

    @Accessor("powerY")
    double getPowerY();

    @Accessor("powerZ")
    double getPowerZ();
}