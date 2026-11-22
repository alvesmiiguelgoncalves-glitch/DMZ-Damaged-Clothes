package com.dmzdamagedclothes;

import net.minecraftforge.common.ForgeConfigSpec;

/** Config de cliente: config/dmz_damaged_clothes-client.toml. */
public final class DamagedConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PARTICLES;
    public static final ForgeConfigSpec.IntValue MAX_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue SSJ4_CUT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PARTICLES = b.comment("Particulas de 1 pixel que caem da roupa quando um pixel troca (rasga).").define("particles", true);
        MAX_PARTICLES = b.comment("Maximo de particulas por mudanca de estagio de uma peca.").defineInRange("maxParticlesPerChange", 60, 1, 400);
        SSJ4_CUT = b.comment("Em formas SSJ4 (modelo ssj4gt/ssj4d) a roupa danificada perde a camisa e as mangas, como a roupa normal do mod de formas.").define("ssj4Cut", true);
        SPEC = b.build();
    }

    private DamagedConfig() {
    }
}
