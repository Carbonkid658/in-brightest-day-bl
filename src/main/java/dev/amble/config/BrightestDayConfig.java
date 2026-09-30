package dev.amble.config;

import dev.amble.BrightestDay;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

public class BrightestDayConfig {
    public static final ConfigClassHandler<BrightestDayConfig> HANDLER = ConfigClassHandler.createBuilder(BrightestDayConfig.class)
            .id(BrightestDay.id("config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(BrightestDay.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public double blastRange = 48.0;
    @SerialEntry
    public double blastRadius = 5.0;
    @SerialEntry
    public float blastDirectDamage = 6.0F;
    @SerialEntry
    public float blastSplashDamage = 4.0F;
    @SerialEntry
    public double blastKnockback = 1.8;
    @SerialEntry
    public float blastExplosionPower = 2.5F;
    @SerialEntry
    public boolean blastBreaksBlocks = true;
    @SerialEntry
    public double blastAssistConeDegrees = 6.0;
    @SerialEntry
    public double blastHomingConeDegrees = 22.0;

    @SerialEntry
    public float beamDamage = 4.0F;
    @SerialEntry
    public int beamMaxTicks = 140;
    @SerialEntry
    public int beamDrainPerSecond = 20;

    @SerialEntry
    public float healBeamAmount = 1.0F;
    @SerialEntry
    public int healBeamMaxTicks = 200;
    @SerialEntry
    public int healBeamDrainPerSecond = 10;

    @SerialEntry
    public int wallLifetimeTicks = 400;

    @SerialEntry
    public int sculptBlockCost = 1;
    @SerialEntry
    public int sculptMaxBlocks = 4096;
    @SerialEntry
    public int sculptMaxTotalBlocks = 16384;
    @SerialEntry
    public int sculptBlocksPerUpkeep = 128;

    @SerialEntry
    public int blastChargeTicks = 25;
    @SerialEntry
    public int beamChargeTicks = 12;
    @SerialEntry
    public int healBeamChargeTicks = 10;
    @SerialEntry
    public int entityShieldChargeTicks = 6;
    @SerialEntry
    public int areaShieldChargeTicks = 8;
    @SerialEntry
    public int wallChargeTicks = 6;

    @SerialEntry
    public double concussiveRange = 6.0;
    @SerialEntry
    public double concussiveConeDegrees = 45.0;
    @SerialEntry
    public double concussiveKnockback = 2.2;
    @SerialEntry
    public float concussiveDamage = 3.0F;
    @SerialEntry
    public int concussiveCost = 25;
    @SerialEntry
    public int concussiveCooldownTicks = 20;

    @SerialEntry
    public double acidRange = 6.0;
    @SerialEntry
    public float acidDamage = 2.0F;
    @SerialEntry
    public int acidFireSeconds = 3;
    @SerialEntry
    public int acidArmorWear = 2;
    @SerialEntry
    public int acidDrainPerSecond = 15;

    public static BrightestDayConfig get() {
        return HANDLER.instance();
    }

    public static void load() {
        HANDLER.load();
    }
}
