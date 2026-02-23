package nel.bettershield.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class SparkParticle extends SpriteBillboardParticle {

    protected SparkParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);
        this.velocityX = vx + (Math.random() * 0.2 - 0.1);
        this.velocityY = vy + (Math.random() * 0.2);
        this.velocityZ = vz + (Math.random() * 0.2 - 0.1);
        this.scale = 0.075f + (float)(Math.random() * 0.05);
        this.maxAge = 10 + this.random.nextInt(10);
        this.collidesWithWorld = true;
        this.gravityStrength = 1.0f;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880;
    }

    @Override
    public void tick() {
        // 1.21.5 FIX: prevPosX/Y/Z is now lastX/Y/Z
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        this.velocityY -= 0.04D * (double)this.gravityStrength;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= 0.98D;
        this.velocityY *= 0.98D;
        this.velocityZ *= 0.98D;

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
    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            SparkParticle particle = new SparkParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}