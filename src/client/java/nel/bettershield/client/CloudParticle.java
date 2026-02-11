package nel.bettershield.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

@Environment(EnvType.CLIENT)
public class CloudParticle extends SpriteBillboardParticle {

    protected CloudParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);

        // 1. VELOCITY
        // Small random drift + whatever velocity was passed
        this.velocityX = vx + (Math.random() * 0.05 - 0.025);
        this.velocityY = vy + (Math.random() * 0.05);
        this.velocityZ = vz + (Math.random() * 0.05 - 0.025);

        // 2. SCALE
        // Poof particles are usually somewhat large.
        // We start around 0.5 (half block) and it will grow in tick().
        this.scale = 0.5f + (float)(Math.random() * 0.2);

        // 3. LIFESPAN
        // Lasts about 1 second (20 ticks)
        this.maxAge = 20 + this.random.nextInt(10);

        // 4. PHYSICS
        this.collidesWithWorld = false; // Clouds drift through blocks usually
        this.gravityStrength = 0.0f;    // No gravity, they float

        // 5. COLOR
        // Slight variation in greyscale to look like dust
        float grey = 0.9f + (this.random.nextFloat() * 0.1f);
        this.red = grey;
        this.green = grey;
        this.blue = grey;
        this.alpha = 0.8f; // Start slightly transparent
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

        // --- ANIMATION ---

        // 1. Grow slightly as it ages (puffing out)
        this.scale += 0.01f;

        // 2. Fade out near the end of life
        if (this.age > this.maxAge / 2) {
            this.alpha = 1.0f - ((float)(this.age - (this.maxAge/2)) / (float)(this.maxAge/2));
            if (this.alpha < 0) this.alpha = 0;
        }

        // 3. Move
        this.move(this.velocityX, this.velocityY, this.velocityZ);

        // 4. Air Drag (Slow down)
        this.velocityX *= 0.95D;
        this.velocityY *= 0.95D;
        this.velocityZ *= 0.95D;
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
            CloudParticle particle = new CloudParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}