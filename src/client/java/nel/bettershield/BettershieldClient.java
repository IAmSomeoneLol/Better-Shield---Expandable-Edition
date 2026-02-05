package nel.bettershield;

import nel.bettershield.client.ShieldHudOverlay;
import nel.bettershield.client.ThrownShieldEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class BettershieldClient implements ClientModInitializer {

	private static final Identifier STAR_TEXTURE = new Identifier("bettershield", "textures/particle/stun_star.png");
	private static final HashMap<Integer, Long> STUNNED_ENTITIES = new HashMap<>();
	private boolean wasAttackPressed = false;
	private final Random random = new Random();

	// PUBLIC STATIC ACCESS FOR RENDER MIXIN
	public static int chargeTicks = 0;
	public static boolean isChargingThrow = false;

	public static final KeyBinding THROW_SHIELD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.bettershield.throw",
			InputUtil.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_3,
			"category.bettershield.title"
	));

	@Override
	public void onInitializeClient() {

		HudRenderCallback.EVENT.register(new ShieldHudOverlay());
		EntityRendererRegistry.register(Bettershield.THROWN_SHIELD_ENTITY_TYPE, ThrownShieldEntityRenderer::new);

		// 1. RECEIVE COOLDOWN SYNC
		ClientPlayNetworking.registerGlobalReceiver(Bettershield.PACKET_SYNC_COOLDOWN, (client, handler, buf, responseSender) -> {
			int type = buf.readInt();
			int duration = buf.readInt();
			client.execute(() -> {
				if (client.player != null) {
					long expiry = client.world.getTime() + duration;
					if (type == 1) { Bettershield.BASH_COOLDOWN.put(client.player.getUuid(), expiry); Bettershield.BASH_MAX.put(client.player.getUuid(), duration); }
					if (type == 2) { Bettershield.SLAM_COOLDOWN.put(client.player.getUuid(), expiry); Bettershield.SLAM_MAX.put(client.player.getUuid(), duration); }
					if (type == 3) { Bettershield.PARRY_MELEE_COOLDOWN.put(client.player.getUuid(), expiry); Bettershield.PARRY_MELEE_MAX.put(client.player.getUuid(), duration); }
					if (type == 4) { Bettershield.PARRY_PROJECTILE_COOLDOWN.put(client.player.getUuid(), expiry); Bettershield.PARRY_PROJECTILE_MAX.put(client.player.getUuid(), duration); }
					if (type == 5) { Bettershield.THROW_COOLDOWN.put(client.player.getUuid(), expiry); Bettershield.THROW_MAX.put(client.player.getUuid(), duration); }
				}
			});
		});

		// 2. RECEIVE STUN
		ClientPlayNetworking.registerGlobalReceiver(Bettershield.PACKET_STUN_MOBS, (client, handler, buf, responseSender) -> {
			int entityId = buf.readInt();
			int duration = buf.readInt();
			client.execute(() -> {
				if (client.world != null) {
					STUNNED_ENTITIES.put(entityId, client.world.getTime() + duration);
				}
			});
		});

		// 3. RECEIVE SLAM PARTICLE EFFECT
		ClientPlayNetworking.registerGlobalReceiver(Bettershield.PACKET_SLAM_EFFECT, (client, handler, buf, responseSender) -> {
			double x = buf.readDouble();
			double y = buf.readDouble();
			double z = buf.readDouble();
			String blockId = buf.readString();

			client.execute(() -> {
				if (client.world != null) {
					ItemStack debrisStack = new ItemStack(Registries.ITEM.get(new Identifier(blockId)));
					if (debrisStack.isEmpty()) debrisStack = new ItemStack(Items.COBBLESTONE);

					for (int i = 0; i < 600; i++) {
						double r = 0.5 + (random.nextDouble() * 1.5);
						double angle = random.nextDouble() * Math.PI * 2;
						double ox = x + Math.cos(angle) * r;
						double oz = z + Math.sin(angle) * r;
						double oy = y + 0.2;
						double vy = 0.8 + (random.nextDouble() * 1.2);
						double speed = 0.1 + (random.nextDouble() * 0.3);
						double vx = Math.cos(angle) * speed;
						double vz = Math.sin(angle) * speed;

						client.world.addParticle(
								new ItemStackParticleEffect(ParticleTypes.ITEM, debrisStack),
								ox, oy, oz, vx, vy, vz
						);
					}
				}
			});
		});

		// 4. VISUALS
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null || client.player == null) return;
			long now = client.world.getTime();
			Iterator<Map.Entry<Integer, Long>> iterator = STUNNED_ENTITIES.entrySet().iterator();
			while (iterator.hasNext()) { if (now > iterator.next().getValue()) iterator.remove(); }
			for (Entity entity : client.world.getEntities()) {
				if (!(entity instanceof LivingEntity living)) continue;
				if (STUNNED_ENTITIES.containsKey(entity.getId()) || living.hasStatusEffect(Bettershield.STUN_EFFECT)) renderHalo(context, living);
			}
		});

		// 5. INPUTS
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			// --- BLACKLIST CHECK ---
			// If Offhand Shield + Main Hand Gun -> DISABLE LOGIC
			ItemStack main = client.player.getMainHandStack();
			ItemStack off = client.player.getOffHandStack();

			boolean mainIsShield = main.isOf(Items.SHIELD);
			boolean offIsShield = off.isOf(Items.SHIELD);
			boolean blacklisted = false;

			if (offIsShield && !mainIsShield) {
				BetterShieldConfig config = Bettershield.getConfig();
				String itemId = Registries.ITEM.getId(main.getItem()).toString();
				// Check if Main Hand item contains any blacklisted string (e.g. "tacz", "pointblank")
				for (String blocked : config.compatibility.mainHandBlacklist) {
					if (itemId.contains(blocked)) {
						blacklisted = true;
						break;
					}
				}
			}

			if (blacklisted) {
				// Reset states just in case
				isChargingThrow = false;
				chargeTicks = 0;
				wasAttackPressed = false;
				return; // STOP HERE
			}
			// -----------------------

			// --- BASH LOGIC ---
			boolean isAttacking = client.options.attackKey.isPressed();
			boolean isBlockingInput = client.options.useKey.isPressed();
			if (isAttacking) {
				if (!wasAttackPressed) {
					boolean hasShield = mainIsShield || offIsShield;
					if (hasShield && isBlockingInput) {
						ClientPlayNetworking.send(Bettershield.PACKET_SHIELD_ATTACK, PacketByteBufs.create());
						client.player.swingHand(client.player.getActiveHand());
					}
					wasAttackPressed = true;
				}
			} else { wasAttackPressed = false; }

			// --- THROW CHARGE LOGIC ---
			if (THROW_SHIELD_KEY.isPressed()) {
				boolean hasLoyalShield = (mainIsShield && main.hasEnchantments()) || (offIsShield && off.hasEnchantments());

				if (hasLoyalShield) {
					isChargingThrow = true;
					if (chargeTicks < 20) {
						chargeTicks++;
					}
				} else {
					isChargingThrow = false;
					chargeTicks = 0;
				}
			} else {
				// BUTTON RELEASED
				if (isChargingThrow) {
					PacketByteBuf buf = PacketByteBufs.create();
					buf.writeInt(chargeTicks);
					ClientPlayNetworking.send(Bettershield.PACKET_SHIELD_THROW, buf);

					isChargingThrow = false;
					chargeTicks = 0;
				}
			}
		});
	}

	private void renderHalo(WorldRenderContext context, LivingEntity entity) {
		MatrixStack matrices = context.matrixStack();
		Vec3d cameraPos = context.camera().getPos();
		VertexConsumerProvider consumers = context.consumers();
		double x = net.minecraft.util.math.MathHelper.lerp(context.tickDelta(), entity.prevX, entity.getX());
		double y = net.minecraft.util.math.MathHelper.lerp(context.tickDelta(), entity.prevY, entity.getY());
		double z = net.minecraft.util.math.MathHelper.lerp(context.tickDelta(), entity.prevZ, entity.getZ());
		double height = entity.getEyeHeight(entity.getPose()) + 0.5;
		float time = entity.age + context.tickDelta();
		int starCount = 3;
		double radius = 0.65;
		double speed = 0.3;
		VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(STAR_TEXTURE));
		Quaternionf cameraRot = context.camera().getRotation();
		matrices.push();
		matrices.translate(x - cameraPos.x, (y - cameraPos.y) + height, z - cameraPos.z);
		for (int i = 0; i < starCount; i++) {
			matrices.push();
			double angle = (time * speed) + (i * (Math.PI * 2 / starCount));
			double oX = Math.cos(angle) * radius;
			double oZ = Math.sin(angle) * radius;
			matrices.translate(oX, 0, oZ);
			matrices.multiply(cameraRot);
			drawQuad(matrices, buffer, 0.25f);
			matrices.pop();
		}
		matrices.pop();
	}

	private void drawQuad(MatrixStack matrices, VertexConsumer buffer, float size) {
		MatrixStack.Entry entry = matrices.peek();
		int light = 15728880;
		buffer.vertex(entry.getPositionMatrix(), -size, -size, 0).color(255, 255, 255, 255).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry.getNormalMatrix(), 0, 1, 0).next();
		buffer.vertex(entry.getPositionMatrix(), size, -size, 0).color(255, 255, 255, 255).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry.getNormalMatrix(), 0, 1, 0).next();
		buffer.vertex(entry.getPositionMatrix(), size, size, 0).color(255, 255, 255, 255).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry.getNormalMatrix(), 0, 1, 0).next();
		buffer.vertex(entry.getPositionMatrix(), -size, size, 0).color(255, 255, 255, 255).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry.getNormalMatrix(), 0, 1, 0).next();
	}
}