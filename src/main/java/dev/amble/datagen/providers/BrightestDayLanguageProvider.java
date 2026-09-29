package dev.amble.datagen.providers;

import dev.amble.core.BrightestDayBlocks;
import dev.amble.core.BrightestDayItems;
import dev.amble.core.ringpowers.LanternCorps;
import dev.amble.core.ringpowers.RingPowerRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class BrightestDayLanguageProvider extends FabricLanguageProvider {
    public BrightestDayLanguageProvider(FabricPackOutput packOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(packOutput, registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
        builder.add(BrightestDayItems.GREEN_POWER_RING, "Green Power Ring");
        builder.add(BrightestDayItems.YELLOW_POWER_RING, "Yellow Power Ring");
        builder.add(BrightestDayItems.RED_POWER_RING, "Red Power Ring");
        builder.add(BrightestDayItems.ORANGE_POWER_RING, "Orange Power Ring");
        builder.add(BrightestDayItems.BLUE_POWER_RING, "Blue Power Ring");
        builder.add(BrightestDayItems.INDIGO_POWER_RING, "Indigo Power Ring");
        builder.add(BrightestDayItems.STAR_SAPPHIRE_POWER_RING, "Star Sapphire Power Ring");
        builder.add(BrightestDayItems.WHITE_POWER_RING, "White Power Ring");
        builder.add(BrightestDayItems.BLACK_POWER_RING, "Black Power Ring");
        builder.add(BrightestDayBlocks.GREEN_LANTERN_BLOCK, "Green Lantern");
        builder.add(BrightestDayBlocks.YELLOW_LANTERN_BLOCK, "Yellow Lantern");
        builder.add(BrightestDayBlocks.BLACK_LANTERN_BLOCK, "Black Lantern");
        builder.add(BrightestDayBlocks.WHITE_LANTERN_BLOCK, "White Lantern");
        builder.add(BrightestDayBlocks.RED_LANTERN_BLOCK, "Red Lantern");
        builder.add(BrightestDayBlocks.ORANGE_LANTERN_BLOCK, "Orange Lantern");
        builder.add(BrightestDayBlocks.BLUE_LANTERN_BLOCK, "Blue Lantern");
        builder.add(BrightestDayBlocks.INDIGO_LANTERN_BLOCK, "Indigo Lantern");
        builder.add(BrightestDayBlocks.STAR_SAPPHIRE_LANTERN_BLOCK, "Star Sapphire Lantern");

        builder.add("itemGroup.brightestday.brightest_day", "In Brightest Day");
        builder.add("container.brightestday.lantern", "Lantern");
        builder.add("gui.brightestday.lantern", "Lantern");
        builder.add("gui.brightestday.inventory", "Inventory");
        builder.add("gui.brightestday.no_ring", "No ring equipped");
        builder.add("gui.brightestday.brightness", "Brightness: %s");
        builder.add("gui.brightestday.saturation", "Saturation: %s");

        builder.add(LanternCorps.GREEN.getTranslationKey(), "Green Lantern Corps");
        builder.add(LanternCorps.BLUE.getTranslationKey(), "Blue Lantern Corps");
        builder.add(LanternCorps.YELLOW.getTranslationKey(), "Sinestro Corps");
        builder.add(LanternCorps.ORANGE.getTranslationKey(), "Orange Lanterns");
        builder.add(LanternCorps.RED.getTranslationKey(), "Red Lantern Corps");
        builder.add(LanternCorps.INDIGO.getTranslationKey(), "Indigo Tribe");
        builder.add(LanternCorps.STAR_SAPPHIRE.getTranslationKey(), "Star Sapphires");
        builder.add(LanternCorps.WHITE.getTranslationKey(), "White Lantern Corps");
        builder.add(LanternCorps.BLACK.getTranslationKey(), "Black Lantern Corps");

        builder.add(LanternCorps.GREEN.oathKey(), """
                In brightest day,
                In blackest night,
                No evil shall escape my sight.
                Let those who worship evil's might,
                Beware my power,
                Green Lantern's light!""");
        builder.add(LanternCorps.YELLOW.oathKey(), """
                In blackest day,
                In brightest night,
                Beware your fears made into light.
                Let those who try to stop what's right,
                Burn like my power,
                Sinestro's might!""");
        builder.add(LanternCorps.RED.oathKey(), """
                With blood and rage of crimson red,
                Ripped from a corpse so freshly dead,
                Together with our hellish hate,
                We'll burn you all, that is your fate!""");
        builder.add(LanternCorps.ORANGE.oathKey(), """
                What's mine is mine and mine and mine,
                And mine and mine and mine!
                Not yours!""");
        builder.add(LanternCorps.BLUE.oathKey(), """
                In fearful day,
                In raging night,
                With strong hearts full, our souls ignite.
                When all seems lost in the War of Light,
                Look to the stars,
                For hope burns bright!""");
        builder.add(LanternCorps.INDIGO.oathKey(), """
                Tor lorek san, nok var.
                Ter lantern ker, lok tar.
                Nok formorra, sorrow lo.
                Sen ker, sen lo, sen gorro.""");
        builder.add(LanternCorps.STAR_SAPPHIRE.oathKey(), """
                For hearts that feel an empty place,
                And lonely souls adrift in space,
                Take the love I have to give,
                Star Sapphire, through you we live!""");
        builder.add(LanternCorps.WHITE.oathKey(), """
                From the dark of death,
                To the light of birth,
                All that lives shall share this earth.
                By the white light's endless worth,
                I stand for life,
                The glow of rebirth!""");
        builder.add(LanternCorps.BLACK.oathKey(), """
                The blackest night falls from the skies,
                The darkness grows as all light dies,
                We crave your hearts and your demise,
                By my black hand, the dead shall rise!""");

        builder.add("message.brightestday.wrong_lantern", "This lantern only answers to the %s.");
        builder.add("message.brightestday.arm_to_charge", "Raise your ring to the lantern to charge it.");
        builder.add("message.brightestday.no_constructs", "Your ring cannot manifest constructs.");
        builder.add("message.brightestday.ring_depleted", "Your ring is out of charge.");
        builder.add("message.brightestday.construct_selected", "Construct: %s");
        builder.add("message.brightestday.shield_radius", "Shield radius: %s");

        builder.add(RingPowerRegistry.FLIGHT.getTranslationKey(), "Flight");
        builder.add(RingPowerRegistry.ARMED.getTranslationKey(), "Armed");
        builder.add(RingPowerRegistry.LIGHT.getTranslationKey(), "Light");
        builder.add(RingPowerRegistry.BLAST.getTranslationKey(), "Blast");
        builder.add(RingPowerRegistry.ENTITY_SHIELD.getTranslationKey(), "Bubble Shield");
        builder.add(RingPowerRegistry.AREA_SHIELD.getTranslationKey(), "Dome Shield");
        builder.add(RingPowerRegistry.TRACTOR_BEAM.getTranslationKey(), "Tractor Beam");
        builder.add(RingPowerRegistry.SCAN.getTranslationKey(), "Scan");

        builder.add("message.brightestday.nothing_to_scan", "Nothing to scan.");
        builder.add("message.brightestday.scan_cooldown", "Scanner recharging: %ss");
        builder.add("message.brightestday.raise_ring_first", "Raise your ring first.");
        builder.add("key.brightestday.scan", "Scan (hold)");
        builder.add("scan.brightestday.scanning", "SCANNING");
        builder.add("scan.brightestday.type", "Type: %s");
        builder.add("scan.brightestday.health", "Health: %s / %s");
        builder.add("scan.brightestday.armor", "Armor: %s");
        builder.add("scan.brightestday.attack", "Attack: %s");
        builder.add("scan.brightestday.speed", "Speed: %s blocks/s");
        builder.add("scan.brightestday.disposition", "Disposition: %s");
        builder.add("scan.brightestday.hostile", "Hostile");
        builder.add("scan.brightestday.neutral", "Neutral");
        builder.add("scan.brightestday.passive", "Passive");
        builder.add("scan.brightestday.effects", "Effects: %s");
        builder.add("scan.brightestday.owner", "Owner: %s");
        builder.add("scan.brightestday.corps", "Corps: %s (%s%%)");
        builder.add("scan.brightestday.ringless", "No ring detected");
        builder.add("scan.brightestday.distance", "Distance: %s blocks");
        builder.add("scan.brightestday.lantern", "Lantern of the %s");
        builder.add("scan.brightestday.id", "ID: %s");
        builder.add("scan.brightestday.hardness", "Hardness: %s");
        builder.add("scan.brightestday.blast_resistance", "Blast Resistance: %s");
        builder.add("scan.brightestday.tool", "Tool: %s");
        builder.add("scan.brightestday.tool.pickaxe", "Pickaxe");
        builder.add("scan.brightestday.tool.axe", "Axe");
        builder.add("scan.brightestday.tool.shovel", "Shovel");
        builder.add("scan.brightestday.tool.hoe", "Hoe");
        builder.add("scan.brightestday.tool.hand", "Any");
        builder.add("scan.brightestday.required", " (required)");
        builder.add("scan.brightestday.light", "Light: %s");
        builder.add("scan.brightestday.position", "Position: %s, %s, %s");

        builder.add("key.category.brightestday.main", "In Brightest Day");
        builder.add("key.brightestday.power_1", "Ring Power 1");
        builder.add("key.brightestday.power_2", "Ring Power 2");
        builder.add("key.brightestday.power_3", "Ring Power 3");
        builder.add("key.brightestday.power_4", "Ring Power 4");
        builder.add("key.brightestday.toggle_light", "Toggle Ring Light");
        builder.add("key.brightestday.cycle_construct", "Cycle Construct");
    }
}
