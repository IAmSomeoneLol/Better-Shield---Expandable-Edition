package nel.bettershield.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class CloudParticle extends SpriteBillboardParticle {

    protected CloudParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);
        this.velocityX = vx + (Math.random() * 0.05 - 0.025);
        this.velocityY = vy + (Math.random() * 0.05);
        this.velocityZ = vz + (Math.random() * 0.05 - 0.025);
        this.scale = 0.5f + (float)(Math.random() * 0.2);
        this.maxAge = 20 + this.random.nextInt(10);
        this.collidesWithWorld = false;
        this.gravityStrength = 0.0f;
        float grey = 0.9f + (this.random.nextFloat() * 0.1f);
        this.red = grey;
        this.green = grey;
        this.blue = grey;
        this.setAlpha(0.8f);
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

        this.scale += 0.01f;
        if (this.age > this.maxAge / 2) {
            float fade = 1.0f - ((float)(this.age - (this.maxAge/2)) / (float)(this.maxAge/2));
            this.setAlpha(Math.max(0.0f, fade));
        }

        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= 0.95D;
        this.velocityY *= 0.95D;
        this.velocityZ *= 0.95D;
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
            CloudParticle particle = new CloudParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.spriteProvider);
            return particle;
        }
    }
}