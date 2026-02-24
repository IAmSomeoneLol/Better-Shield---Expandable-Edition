package nel.bettershield.client;

import me.shedaniel.autoconfig.AutoConfig;
import nel.bettershield.BetterShieldConfig;
import nel.bettershield.Bettershield;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.RenderLayer;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ShieldHudOverlay {

    private static final Identifier BASH_ICON = Identifier.of("bettershield", "textures/gui/bash.png");
    private static final Identifier SLAM_ICON = Identifier.of("bettershield", "textures/gui/slam.png");
    private static final Identifier PARRY_MELEE_ICON = Identifier.of("bettershield", "textures/gui/parry_melee.png");
    private static final Identifier PARRY_PROJ_ICON = Identifier.of("bettershield", "textures/gui/parry_proj.png");
    private static final Identifier THROW_ICON = Identifier.of("bettershield", "textures/gui/shield_throw.png");

    private long bashFinishTime = 0;
    private long slamFinishTime = 0;
    private long meleeFinishTime = 0;
    private long projFinishTime = 0;
    private long throwFinishTime = 0;

    // 1.21.6 FIX: Removed HudRenderCallback interface implementation
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        BetterShieldConfig config = AutoConfig.getConfigHolder(BetterShieldConfig.class).getConfig();
        if (config.hud.hudMode == BetterShieldConfig.Hud.HudMode.OFF) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int x = (width / 2) + config.hud.xOffset;
        int y = (height / 2) + config.hud.yOffset;
        float scale = config.hud.scale;

        UUID uuid = client.player.getUuid();
        long now = client.world.getTime();

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        List<IconData> iconsToRender = new ArrayList<>();

        checkAndAdd(iconsToRender, uuid, now, Bettershield.BASH_COOLDOWN, Bettershield.BASH_MAX, bashFinishTime, BASH_ICON, config.hud.hudMode, "bash");
        checkAndAdd(iconsToRender, uuid, now, Bettershield.SLAM_COOLDOWN, Bettershield.SLAM_MAX, slamFinishTime, SLAM_ICON, config.hud.hudMode, "slam");
        checkAndAdd(iconsToRender, uuid, now, Bettershield.PARRY_MELEE_COOLDOWN, Bettershield.PARRY_MELEE_MAX, meleeFinishTime, PARRY_MELEE_ICON, config.hud.hudMode, "melee");
        checkAndAdd(iconsToRender, uuid, now, Bettershield.PARRY_PROJECTILE_COOLDOWN, Bettershield.PARRY_PROJECTILE_MAX, projFinishTime, PARRY_PROJ_ICON, config.hud.hudMode, "proj");
        checkAndAdd(iconsToRender, uuid, now, Bettershield.THROW_COOLDOWN, Bettershield.THROW_MAX, throwFinishTime, THROW_ICON, config.hud.hudMode, "throw");

        int spacing = 18;
        for (int i = 0; i < iconsToRender.size(); i++) {
            IconData data = iconsToRender.get(i);
            drawBar(context, i * spacing, 0, data.icon, data.progress);
            if (data.progress >= 1.0f) {
                updateFinishTimer(data.type, now);
            } else {
                resetFinishTimer(data.type);
            }
        }
        context.getMatrices().pop();
    }

    private void checkAndAdd(List<IconData> list, UUID uuid, long now,
                             java.util.HashMap<UUID, Long> cdMap,
                             java.util.HashMap<UUID, Integer> maxMap,
                             long finishTime, Identifier icon,
                             BetterShieldConfig.Hud.HudMode mode,
                             String type) {
        long expiry = cdMap.getOrDefault(uuid, 0L);
        boolean isActive = expiry > now;
        boolean justFinished = finishTime != 0 && (now - finishTime) < 20;

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
        int size = 16;
        int colorBg = 0xFF404040; // Dark gray ARGB
        int colorFg = 0xFFFFFFFF; // White ARGB

        context.drawTexture(RenderLayer::getGuiTextured, icon, x, y, 0f, 0f, size, size, size, size, colorBg);

        int filledH = (int) (size * progress);
        int emptyH = size - filledH;
        float ratio = (float) size / (float) size;
        int srcStartV = (int) (emptyH * ratio);

        if (filledH > 0) {
            context.drawTexture(RenderLayer::getGuiTextured, icon, x, y + emptyH, 0f, (float)srcStartV, size, filledH, size, size, colorFg);
        }
    }

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