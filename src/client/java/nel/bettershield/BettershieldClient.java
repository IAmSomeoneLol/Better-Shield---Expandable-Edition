package nel.bettershield;

import nel.bettershield.client.ShieldHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BettershieldClient implements ClientModInitializer {

	public static final HashMap<Integer, Long> STUNNED_ENTITIES = new HashMap<>();
	private static final HashMap<Integer, Integer> BASH_TRAILS = new HashMap<>();
	private boolean wasAttackPressed = false;
	private final Random random = new Random();

	public static int chargeTicks = 0;
	public static boolean isChargingThrow = false;

	private Item lastMainHandItem = null;
	private boolean isBlacklistedCached = false;

	public static final KeyBinding.Category SHIELD_CATEGORY = KeyBinding.Category.create(Identifier.of(Bettershield.MOD_ID, "main"));
	public static final KeyBinding THROW_SHIELD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.bettershield.throw",
			InputUtil.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_3,
			SHIELD_CATEGORY
	));

	@Override
	public void onInitializeClient() {
		ShieldHudOverlay hudOverlay = new ShieldHudOverlay();
		HudElementRegistry.addLast(Identifier.of(Bettershield.MOD_ID, "shield_hud"), hudOverlay::render);

		// 1.21.9 FIX: Fallback to the native Vanilla item renderer for thrown shields!
		EntityRendererRegistry.register(Bettershield.THROWN_SHIELD_ENTITY_TYPE, FlyingItemEntityRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(Bettershield.SyncCooldownPayload.ID, (payload, context) -> {
			int type = payload.type();
			int duration = payload.duration();
			context.client().execute(() -> {
				if (context.client().player != null) {
					long expiry = context.client().world.getTime() + duration;
					UUID uuid = context.client().player.getUuid();
					if (type == 1) { Bettershield.BASH_COOLDOWN.put(uuid, expiry); Bettershield.BASH_MAX.put(uuid, duration); }
					if (type == 2) { Bettershield.SLAM_COOLDOWN.put(uuid, expiry); Bettershield.SLAM_MAX.put(uuid, duration); }
					if (type == 3) { Bettershield.PARRY_MELEE_COOLDOWN.put(uuid, expiry); Bettershield.PARRY_MELEE_MAX.put(uuid, duration); }
					if (type == 4) { Bettershield.PARRY_PROJECTILE_COOLDOWN.put(uuid, expiry); Bettershield.PARRY_PROJECTILE_MAX.put(uuid, duration); }
					if (type == 5) { Bettershield.THROW_COOLDOWN.put(uuid, expiry); Bettershield.THROW_MAX.put(uuid, duration); }
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Bettershield.StunMobsPayload.ID, (payload, context) -> {
			int entityId = payload.entityId();
			int duration = payload.duration();
			context.client().execute(() -> {
				if (context.client().world != null) STUNNED_ENTITIES.put(entityId, context.client().world.getTime() + duration);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Bettershield.SlamEffectPayload.ID, (payload, context) -> {
			double x = payload.x();
			double y = payload.y();
			double z = payload.z();
			String blockId = payload.blockId();
			context.client().execute(() -> {
				if (context.client().world != null) {
					ItemStack debrisStack = new ItemStack(Registries.ITEM.get(Identifier.of(blockId)));
					if (debrisStack.isEmpty()) debrisStack = new ItemStack(Registries.ITEM.get(Identifier.of("minecraft", "cobblestone")));
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
						context.client().particleManager.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, debrisStack), ox, oy, oz, vx, vy, vz);
					}
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Bettershield.BashTrailPayload.ID, (payload, context) -> {
			int entityId = payload.entityId();
			int duration = payload.duration();
			context.client().execute(() -> { BASH_TRAILS.put(entityId, duration); });
		});

		// Halo rendering removed temporarily due to Fabric 1.21.9 deleting WorldRenderEvents

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null) return;

			long now = client.world.getTime();
			Iterator<Map.Entry<Integer, Long>> iterator = STUNNED_ENTITIES.entrySet().iterator();
			while (iterator.hasNext()) { if (now > iterator.next().getValue()) iterator.remove(); }

			Iterator<Map.Entry<Integer, Integer>> trailIterator = BASH_TRAILS.entrySet().iterator();
			while (trailIterator.hasNext()) {
				Map.Entry<Integer, Integer> entry = trailIterator.next();
				int id = entry.getKey();
				int ticks = entry.getValue();
				Entity entity = client.world.getEntityById(id);
				if (entity != null) {
					for (int i = 0; i < 3; i++) {
						double offsetX = (random.nextGaussian() * 0.2);
						double offsetZ = (random.nextGaussian() * 0.2);
						double offsetY = (random.nextDouble() * 0.2);
						client.particleManager.addParticle(ParticleTypes.CLOUD, entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ, 0.0, 0.01, 0.0);
					}
				}
				if (ticks <= 1) trailIterator.remove(); else entry.setValue(ticks - 1);
			}

			ItemStack main = client.player.getMainHandStack();
			ItemStack off = client.player.getOffHandStack();

			boolean mainIsShield = main.getItem() instanceof net.minecraft.item.ShieldItem;
			boolean offIsShield = off.getItem() instanceof net.minecraft.item.ShieldItem;
			boolean blacklisted = false;

			if (offIsShield && !mainIsShield) {
				if (main.getItem() != lastMainHandItem) {
					lastMainHandItem = main.getItem();
					isBlacklistedCached = false;

					BetterShieldConfig config = Bettershield.getConfig();
					String itemId = Registries.ITEM.getId(main.getItem()).toString();
					for (String blocked : config.compatibility.mainHandBlacklist) {
						if (itemId.contains(blocked)) {
							isBlacklistedCached = true;
							break;
						}
					}
				}
				blacklisted = isBlacklistedCached;
			} else {
				lastMainHandItem = null;
			}

			if (blacklisted) { isChargingThrow = false; chargeTicks = 0; wasAttackPressed = false; return; }

			boolean isAttacking = client.options.attackKey.isPressed();
			boolean isBlockingInput = client.options.useKey.isPressed();
			if (isAttacking) {
				if (!wasAttackPressed) {
					boolean hasShield = mainIsShield || offIsShield;
					if (hasShield && isBlockingInput) {
						ClientPlayNetworking.send(new Bettershield.ShieldAttackPayload());
						client.player.swingHand(client.player.getActiveHand());
					}
					wasAttackPressed = true;
				}
			} else { wasAttackPressed = false; }

			if (THROW_SHIELD_KEY.isPressed()) {
				boolean hasLoyalShield = (mainIsShield && main.hasEnchantments()) || (offIsShield && off.hasEnchantments());
				if (hasLoyalShield) {
					isChargingThrow = true;
					if (chargeTicks < 20) chargeTicks++;
				} else { isChargingThrow = false; chargeTicks = 0; }
			} else {
				if (isChargingThrow) {
					ClientPlayNetworking.send(new Bettershield.ShieldThrowPayload(chargeTicks));
					isChargingThrow = false; chargeTicks = 0;
				}
			}
		});
	}
}