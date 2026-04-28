package nel.bettershield.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class StunStarParticle extends SpriteBillboardParticle {
    protected StunStarParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
        this.maxAge = 2;
        this.scale = 0.25f;
        this.collidesWithWorld = false;
        this.gravityStrength = 0.0f;
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vX, double vY, double vZ) {
            StunStarParticle particle = new StunStarParticle(world, x, y, z);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}