package dev.amble.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.amble.BrightestDay;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.util.Optional;

public final class BatteryTextures implements ResourceManagerReloadListener {
    public static final Identifier BASE = BrightestDay.id("dynamic/central_power_battery");
    public static final Identifier EMISSION = BrightestDay.id("dynamic/central_power_battery_emission");
    public static final Identifier BASE_SOURCE = BrightestDay.id("textures/block/green_central_power_battery.png");
    public static final Identifier EMISSION_SOURCE = BrightestDay.id("textures/block/green_central_power_battery_emission.png");

    private static volatile boolean stale = true;
    private static boolean available;

    public static void init() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(BrightestDay.id("battery_textures"), new BatteryTextures());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        stale = true;
    }

    public static boolean ready() {
        if (!stale) return available;
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        boolean base = load(manager, BASE_SOURCE, BASE);
        boolean emission = load(manager, EMISSION_SOURCE, EMISSION);
        available = base && emission;
        stale = false;
        return available;
    }

    private static boolean load(ResourceManager manager, Identifier source, Identifier target) {
        Optional<Resource> resource = manager.getResource(source);
        if (resource.isEmpty()) return false;
        try (InputStream stream = resource.get().open()) {
            NativeImage image = NativeImage.read(stream);
            neutralize(image);
            Minecraft.getInstance().getTextureManager().register(target, new DynamicTexture(target::toString, image));
            return true;
        } catch (Exception exception) {
            BrightestDay.LOGGER.warn("Failed to prepare battery texture {}", source, exception);
            return false;
        }
    }

    private static void neutralize(NativeImage image) {
        float brightest = 0.0F;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixel(x, y);
                if (ARGB.alpha(pixel) > 0) brightest = Math.max(brightest, value(pixel));
            }
        }
        if (brightest <= 0.0F) return;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixel(x, y);
                int grey = Mth.clamp(Math.round(value(pixel) / brightest * 255.0F), 0, 255);
                image.setPixel(x, y, ARGB.color(ARGB.alpha(pixel), grey, grey, grey));
            }
        }
    }

    private static float value(int pixel) {
        return Math.max(ARGB.red(pixel), Math.max(ARGB.green(pixel), ARGB.blue(pixel)));
    }

    private BatteryTextures() {}
}
