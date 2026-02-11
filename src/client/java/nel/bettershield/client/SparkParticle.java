package nel.bettershield.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class SparkParticle extends SpriteBillboardParticle {

    protected SparkParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);

        // 1. VELOCITY
        this.velocityX = vx + (Math.random() * 0.2 - 0.1);
        this.velocityY = vy + (Math.random() * 0.2);
        this.velocityZ = vz + (Math.random() * 0.2 - 0.1);

        // 2. SCALE (SIZE) - Updated to 0.075f base
        // Randomized slightly between 0.075 and 0.125
        this.scale = 0.075f + (float)(Math.random() * 0.05);

        // 3. LIFESPAN
        this.maxAge = 10 + this.random.nextInt(10);

        // 4. PHYSICS PROPERTIES
        this.collidesWithWorld = true;
        this.gravityStrength = 1.0f;
    }

    // --- NEW: FULL BRIGHTNESS ---
    // This makes the particle ignore world lighting (like fire/lava/glowstone)
    @Override
    public int getBrightness(float tint) {
        // 15728880 is the integer value for full sky light and full block light (240, 240)
        return 15728880;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        // --- PHYSICS ENGINE ---

        // 1. Gravity
        this.velocityY -= 0.04D * (double)this.gravityStrength;

        // 2. Move
        this.move(this.velocityX, this.velocityY, this.velocityZ);

        // 3. Air Friction
        this.velocityX *= 0.98D;
        this.velocityY *= 0.98D;
        this.velocityZ *= 0.98D;

        // 4. Ground Friction
        if (this.onGround) {
            this.velocityX *= 0.7D;
            this.velocityZ *= 0.7D;
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
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            SparkParticle particle = new SparkParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}