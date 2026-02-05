package nel.bettershield.client;

import me.shedaniel.autoconfig.AutoConfig;
import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ShieldHudOverlay implements HudRenderCallback {

    // --- PATHS ---
    private static final Identifier BASH_ICON = new Identifier("bettershield", "textures/gui/bash.png");
    private static final Identifier SLAM_ICON = new Identifier("bettershield", "textures/gui/slam.png");
    private static final Identifier PARRY_MELEE_ICON = new Identifier("bettershield", "textures/gui/parry_melee.png");
    private static final Identifier PARRY_PROJ_ICON = new Identifier("bettershield", "textures/gui/parry_proj.png");
    private static final Identifier THROW_ICON = new Identifier("bettershield", "textures/gui/shield_throw.png");

    // Timers to handle the "Flash" effect when cooldown finishes
    private long bashFinishTime = 0;
    private long slamFinishTime = 0;
    private long meleeFinishTime = 0;
    private long projFinishTime = 0;
    private long throwFinishTime = 0;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 1. GET CONFIG
        BetterShieldConfig config = AutoConfig.getConfigHolder(BetterShieldConfig.class).getConfig();

        if (config.hud.hudMode == BetterShieldConfig.Hud.HudMode.OFF) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // 2. POSITIONING
        int x = (width / 2) + config.hud.xOffset;
        int y = (height / 2) + config.hud.yOffset;
        float scale = config.hud.scale;

        UUID uuid = client.player.getUuid();
        long now = client.world.getTime();

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        // 3. COLLECT ACTIVE ICONS
        List<IconData> iconsToRender = new ArrayList<>();

        // Check BASH
        checkAndAdd(iconsToRender, uuid, now,
                Bettershield.BASH_COOLDOWN, Bettershield.BASH_MAX,
                bashFinishTime, BASH_ICON, config.hud.hudMode, "bash");

        // Check SLAM
        checkAndAdd(iconsToRender, uuid, now,
                Bettershield.SLAM_COOLDOWN, Bettershield.SLAM_MAX,
                slamFinishTime, SLAM_ICON, config.hud.hudMode, "slam");

        // Check PARRY MELEE
        checkAndAdd(iconsToRender, uuid, now,
                Bettershield.PARRY_MELEE_COOLDOWN, Bettershield.PARRY_MELEE_MAX,
                meleeFinishTime, PARRY_MELEE_ICON, config.hud.hudMode, "melee");

        // Check PARRY PROJ
        checkAndAdd(iconsToRender, uuid, now,
                Bettershield.PARRY_PROJECTILE_COOLDOWN, Bettershield.PARRY_PROJECTILE_MAX,
                projFinishTime, PARRY_PROJ_ICON, config.hud.hudMode, "proj");

        // Check THROW
        checkAndAdd(iconsToRender, uuid, now,
                Bettershield.THROW_COOLDOWN, Bettershield.THROW_MAX,
                throwFinishTime, THROW_ICON, config.hud.hudMode, "throw");

        // 4. RENDER LOOP
        // We iterate through the list and draw them side-by-side.
        // In "ONLY_COOLDOWN" mode, gaps are naturally removed because we didn't add empty items to the list.
        int spacing = 12; // Distance between icons

        for (int i = 0; i < iconsToRender.size(); i++) {
            IconData data = iconsToRender.get(i);
            drawBar(context, i * spacing, 0, data.icon, data.progress);

            // Update finish timers based on data type (To keep the flash effect working)
            if (data.progress >= 1.0f) {
                updateFinishTimer(data.type, now);
            } else {
                resetFinishTimer(data.type);
            }
        }

        context.getMatrices().pop();
    }

    // Helper to check logic and add to list if visible
    private void checkAndAdd(List<IconData> list, UUID uuid, long now,
                             java.util.HashMap<UUID, Long> cdMap,
                             java.util.HashMap<UUID, Integer> maxMap,
                             long finishTime, Identifier icon,
                             BetterShieldConfig.Hud.HudMode mode,
                             String type) {

        long expiry = cdMap.getOrDefault(uuid, 0L);
        boolean isActive = expiry > now;
        boolean justFinished = finishTime != 0 && (now - finishTime) < 20;

        // VISIBILITY LOGIC:
        // 1. If Always Show -> Add it.
        // 2. If Only Cooldown -> Add ONLY if active OR just finished (flashing).
        if (mode == BetterShieldConfig.Hud.HudMode.ALWAYS_SHOW || isActive || justFinished) {
            float progress = getProgress(expiry, maxMap.getOrDefault(uuid, 100), now);
            list.add(new IconData(icon, progress, type));
        }
    }

    private void updateFinishTimer(String type, long now) {
        if (type.equals("bash") && bashFinishTime == 0) bashFinishTime = now;
        if (type.equals("slam") && slamFinishTime == 0) slamFinishTime = now;
        if (type.equals("melee") && meleeFinishTime == 0) meleeFinishTime = now;
        if (type.equals("proj") && projFinishTime == 0) projFinishTime = now;
        if (type.equals("throw") && throwFinishTime == 0) throwFinishTime = now;
    }

    private void resetFinishTimer(String type) {
        if (type.equals("bash")) bashFinishTime = 0;
        if (type.equals("slam")) slamFinishTime = 0;
        if (type.equals("melee")) meleeFinishTime = 0;
        if (type.equals("proj")) projFinishTime = 0;
        if (type.equals("throw")) throwFinishTime = 0;
    }

    private float getProgress(long expiry, int maxTime, long now) {
        if (now >= expiry) return 1.0f;
        long remaining = expiry - now;
        return 1.0f - ((float) remaining / (float) maxTime);
    }

    private void drawBar(DrawContext context, int x, int y, Identifier icon, float progress) {
        int destW = 10;
        int destH = 10;
        int srcW = 16;
        int srcH = 16;

        // 1. BACKGROUND (Darkened)
        context.setShaderColor(0.25f, 0.25f, 0.25f, 1.0f);
        context.drawTexture(icon, x, y, destW, destH, 0, 0, srcW, srcH, srcW, srcH);

        // 2. FOREGROUND (Full Brightness)
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int filledDestH = (int) (destH * progress);
        int emptyDestH = destH - filledDestH;
        float ratio = (float) srcH / (float) destH;
        int srcStartV = (int)(emptyDestH * ratio);
        int srcFilledH = srcH - srcStartV;

        if (filledDestH > 0) {
            context.drawTexture(icon,
                    x, y + emptyDestH,
                    destW, filledDestH,
                    0, srcStartV,
                    srcW, srcFilledH,
                    srcW, srcH
            );
        }

        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // Simple Data Holder
    private static class IconData {
        Identifier icon;
        float progress;
        String type;

        public IconData(Identifier icon, float progress, String type) {
            this.icon = icon;
            this.progress = progress;
            this.type = type;
        }
    }
}