package dev.amble.client.poses;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import dev.amble.BrightestDay;
import dev.amble.core.ringpowers.LanternCorps;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PoseLibrary implements ResourceManagerReloadListener {
    private static final String ROOT = "player_animations/poses";
    private static final String GROUND = "ground_";
    private static final String HOVER = "hover_";
    private static final Map<String, Vec3f> RIG_PIVOTS = Map.of("waist", new Vec3f(0.0F, 12.0F, 0.0F));
    private static final Map<String, String> RIG_PARENTS = Map.of(
            "head", "waist",
            "torso", "waist",
            "right_arm", "torso",
            "left_arm", "torso");
    private static final Map<String, LanternCorps> GROUPS = Map.ofEntries(
            Map.entry("green", LanternCorps.GREEN), Map.entry("will", LanternCorps.GREEN), Map.entry("willpower", LanternCorps.GREEN),
            Map.entry("yellow", LanternCorps.YELLOW), Map.entry("fear", LanternCorps.YELLOW), Map.entry("sinestro", LanternCorps.YELLOW),
            Map.entry("red", LanternCorps.RED), Map.entry("rage", LanternCorps.RED),
            Map.entry("orange", LanternCorps.ORANGE), Map.entry("greed", LanternCorps.ORANGE), Map.entry("avarice", LanternCorps.ORANGE),
            Map.entry("blue", LanternCorps.BLUE), Map.entry("hope", LanternCorps.BLUE),
            Map.entry("indigo", LanternCorps.INDIGO), Map.entry("compassion", LanternCorps.INDIGO),
            Map.entry("star_sapphire", LanternCorps.STAR_SAPPHIRE), Map.entry("sapphire", LanternCorps.STAR_SAPPHIRE), Map.entry("pink", LanternCorps.STAR_SAPPHIRE), Map.entry("love", LanternCorps.STAR_SAPPHIRE),
            Map.entry("white", LanternCorps.WHITE), Map.entry("life", LanternCorps.WHITE),
            Map.entry("black", LanternCorps.BLACK), Map.entry("death", LanternCorps.BLACK));
    private static final Set<String> SHARED = Set.of("shared", "all", "any");

    public record Pose(String key, String name, Animation animation, @Nullable LanternCorps corps) {}

    private static List<Pose> ground = List.of();
    private static List<Pose> hover = List.of();

    public static void init() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(BrightestDay.id("poses"), new PoseLibrary());
    }

    public static List<Pose> poses(@Nullable LanternCorps corps, boolean airborne) {
        List<Pose> own = new ArrayList<>();
        List<Pose> shared = new ArrayList<>();
        for (Pose pose : airborne ? hover : ground) {
            if (pose.corps() == null) shared.add(pose);
            else if (pose.corps() == corps) own.add(pose);
        }
        own.addAll(shared);
        return own;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<Pose> loadedGround = new ArrayList<>();
        List<Pose> loadedHover = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(ROOT, id -> id.getPath().endsWith(".json")).entrySet()) {
            String relative = entry.getKey().getPath().substring(ROOT.length() + 1);
            int slash = relative.indexOf('/');
            String group = slash < 0 ? "shared" : relative.substring(0, slash).toLowerCase();
            LanternCorps corps = GROUPS.get(group);
            if (corps == null && !SHARED.contains(group)) {
                BrightestDay.LOGGER.warn("Ignoring pose file {}: unknown corps folder '{}'", entry.getKey(), group);
                continue;
            }
            try (InputStream stream = entry.getValue().open()) {
                for (Map.Entry<String, Animation> animation : UniversalAnimLoader.loadAnimations(stream).entrySet()) {
                    String name = animation.getKey();
                    Animation rigged = animation.getValue();
                    if (rigged.bones().isEmpty()) rigged.bones().putAll(RIG_PIVOTS);
                    if (rigged.parents().isEmpty()) rigged.parents().putAll(RIG_PARENTS);
                    Pose pose = new Pose(group + "/" + name, name, rigged, corps);
                    if (!name.startsWith(GROUND)) loadedHover.add(pose);
                    if (!name.startsWith(HOVER)) loadedGround.add(pose);
                }
            } catch (Exception exception) {
                BrightestDay.LOGGER.warn("Failed to load pose file {}", entry.getKey(), exception);
            }
        }
        loadedGround.sort(Comparator.comparing(Pose::name));
        loadedHover.sort(Comparator.comparing(Pose::name));
        ground = List.copyOf(loadedGround);
        hover = List.copyOf(loadedHover);
    }
}
