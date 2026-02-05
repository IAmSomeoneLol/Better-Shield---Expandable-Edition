package nel.bettershield;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import java.util.List;
import java.util.ArrayList;

@Config(name = "bettershield")
public class BetterShieldConfig implements ConfigData {

    @ConfigEntry.Category("combat")
    @ConfigEntry.Gui.TransitiveObject
    public Combat combat = new Combat();

    @ConfigEntry.Category("stun_mechanics")
    @ConfigEntry.Gui.TransitiveObject
    public StunMechanics stunMechanics = new StunMechanics();

    @ConfigEntry.Category("fun_combos")
    @ConfigEntry.Gui.TransitiveObject
    public FunCombos funCombos = new FunCombos();

    @ConfigEntry.Category("cooldowns")
    @ConfigEntry.Gui.TransitiveObject
    public Cooldowns cooldowns = new Cooldowns();

    @ConfigEntry.Category("compatibility")
    @ConfigEntry.Gui.TransitiveObject
    public Compatibility compatibility = new Compatibility();

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.TransitiveObject
    public Hud hud = new Hud();

    // --- SUB-CATEGORIES ---

    public static class Combat {
        @Comment("Base damage dealt by Shield Bash. Default 4.0 (2 hearts).")
        public double bashDamage = 4.0;

        @Comment("Knockback strength of Shield Bash.")
        public double bashKnockback = 1.0;

        @Comment("Base damage dealt by Thrown Shield. Default 2.0 (1 heart).")
        public double shieldThrowDamage = 2.0;

        @Comment("Maximum range (blocks) the shield flies before returning. Default 8.0.")
        public double shieldThrowRange = 8.0;

        @Comment("The radius (in blocks) of the Slam impact damage area. Default 5.0.")
        public double slamRadius = 5.0;

        @Comment("The window of time to perform a parry (in ticks). Default 6 (0.3s).")
        public int parryWindow = 6;

        @Comment("Multiplier for arrow reflection speed. Higher = faster return.")
        public double arrowReflectSpeed = 2.0;

        @Comment("Minimum height (blocks) required to perform a Slam.")
        public double slamMinimumHeight = 3.0;
    }

    public static class StunMechanics {
        @Comment("How long the stun lasts (in ticks). 20 ticks = 1 second.")
        public int stunDuration = 70;

        @Comment("If true, Shield Bash applies Stun.")
        public boolean bashStunEnabled = true;

        @Comment("If true, Shield Slam applies Stun.")
        public boolean slamStunEnabled = true;

        @Comment("If true, a successful Melee Parry applies Stun to the attacker.")
        public boolean parryStunEnabled = true;

        @Comment("If true, deflecting a projectile applies Stun to the shooter instantly.")
        public boolean deflectStunEnabled = false;

        @Comment("If true, the Thrown Shield applies Stun to enemies it hits.")
        public boolean throwStunEnabled = true;
    }

    public static class FunCombos {
        @Comment("If true, Shield Bash follows your crosshair (launching you UP if looking up). If false, it only dashes horizontally.")
        public boolean allowVerticalBash = true;
    }

    public static class Cooldowns {
        @Comment("Cooldown for Melee Parry (in ticks). Default 70 (3.5s).")
        public int parryMeleeCooldown = 70;

        @Comment("Cooldown for Arrow Deflect (in ticks). Default 18 (0.9s).")
        public int parryProjectileCooldown = 18;

        @Comment("Cooldown for Shield Bash (in ticks). Default 50 (2.5s).")
        public int bashCooldown = 50;

        @Comment("Cooldown for Shield Slam (in ticks). Default 130 (6.5s).")
        public int slamCooldown = 130; // Changed to 6.5s (130 ticks)

        @Comment("Cooldown for Shield Throw (in ticks). Default 90 (4.5s).")
        public int throwCooldown = 90;  // CHANGED FROM 40 to 90
    }

    public static class Compatibility {
        @Comment("List of Item IDs or Mod IDs that DISABLE offhand shield abilities when held in Main Hand.\nUseful for Gun mods (tacz, pointblank) where Right Click + Left Click is used for aiming/shooting.")
        public List<String> mainHandBlacklist = new ArrayList<>(List.of("tacz", "pointblank", "vic", "gmstation"));
    }

    public static class Hud {
        public enum HudMode {
            OFF,
            ONLY_COOLDOWN,
            ALWAYS_SHOW
        }

        @Comment("Controls when the HUD appears on screen.")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        public HudMode hudMode = HudMode.ONLY_COOLDOWN;

        @Comment("Horizontal offset from the CROSSHAIR. Positive = Right, Negative = Left.")
        public int xOffset = 10;

        @Comment("Vertical offset from the CROSSHAIR. Positive = Down, Negative = Up.")
        public int yOffset = 0;

        @Comment("Scale of the icons (0.5 = half size, 2.0 = double size).")
        public float scale = 1.0f;
    }
}