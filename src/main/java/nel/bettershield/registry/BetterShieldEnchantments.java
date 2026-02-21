package nel.bettershield.registry;

import nel.bettershield.Bettershield;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class BetterShieldEnchantments {

    // --- 1.21 FIX: We now just create "Keys" that point to your future JSON files! ---
    public static final RegistryKey<Enchantment> SHIELD_DENSITY = of("shield_density");
    public static final RegistryKey<Enchantment> PARRYFUL = of("parryful");
    public static final RegistryKey<Enchantment> DEFLECTOR = of("deflector");
    public static final RegistryKey<Enchantment> PARRY_DOCTRINE = of("parry_doctrine");
    public static final RegistryKey<Enchantment> SLAM_FOAM = of("slam_foam");
    public static final RegistryKey<Enchantment> MASTERINE = of("masterine");
    public static final RegistryKey<Enchantment> ACTIVE_ARMOR = of("active_armor");

    private static RegistryKey<Enchantment> of(String name) {
        // In 1.21, Mojang changed "new Identifier()" to "Identifier.of()"
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Bettershield.MOD_ID, name));
    }
}