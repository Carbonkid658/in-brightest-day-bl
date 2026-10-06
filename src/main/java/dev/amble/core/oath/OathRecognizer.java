package dev.amble.core.oath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.amble.BrightestDay;
import dev.amble.config.BrightestDayConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class OathRecognizer {
    private static final float SAMPLE_RATE = 16000.0F;
    private static final int DOWNSAMPLE = 3;

    enum State {
        IDLE,
        LOADING,
        READY,
        FAILED
    }

    private static volatile State state = State.IDLE;
    private static volatile @Nullable Model model;

    static State state() {
        return state;
    }

    static synchronized void ensureLoading() {
        if (state != State.IDLE) return;
        state = State.LOADING;
        Thread loader = new Thread(OathRecognizer::load, "Brightest Day Oath Model");
        loader.setDaemon(true);
        loader.start();
    }

    private static void load() {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            Path directory = modelDirectory();
            if (!Files.isDirectory(directory)) download(directory);
            model = new Model(directory.toString());
            state = State.READY;
            BrightestDay.LOGGER.info("Oath recognition ready ({})", directory.getFileName());
        } catch (Throwable throwable) {
            state = State.FAILED;
            BrightestDay.LOGGER.warn("Oath recognition unavailable, handheld charging stays timed", throwable);
        }
    }

    private static Path modelDirectory() {
        String url = BrightestDayConfig.get().oathModelUrl;
        String name = url.substring(url.lastIndexOf('/') + 1).replace(".zip", "");
        return FabricLoader.getInstance().getConfigDir().resolve(BrightestDay.MOD_ID).resolve(name);
    }

    private static void download(Path directory) throws IOException, InterruptedException {
        String url = BrightestDayConfig.get().oathModelUrl;
        BrightestDay.LOGGER.info("Downloading oath recognition model from {}", url);
        Path root = directory.getParent();
        Files.createDirectories(root);
        Path archive = Files.createTempFile(root, "oath-model", ".zip");
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
            HttpResponse<Path> response = client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofFile(archive));
            if (response.statusCode() != 200) throw new IOException("Model download failed with HTTP " + response.statusCode());
            unzip(archive, root);
        } finally {
            Files.deleteIfExists(archive);
        }
        if (!Files.isDirectory(directory)) throw new IOException("Model archive did not contain " + directory.getFileName());
    }

    private static void unzip(Path archive, Path root) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(input)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                Path target = base.resolve(entry.getName()).normalize();
                if (!target.startsWith(base)) throw new IOException("Unsafe entry in model archive: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static @Nullable Listener listen(List<String> words) {
        if (state != State.READY) return null;
        Model loaded = model;
        if (loaded == null) return null;
        JsonArray grammar = new JsonArray();
        OathMatcher.grammar(words).forEach(grammar::add);
        grammar.add("[unk]");
        try {
            return new Listener(new Recognizer(loaded, SAMPLE_RATE, grammar.toString()));
        } catch (Throwable throwable) {
            BrightestDay.LOGGER.warn("Could not start oath recognizer", throwable);
            return null;
        }
    }

    static final class Listener {
        private final Recognizer recognizer;

        private Listener(Recognizer recognizer) {
            this.recognizer = recognizer;
        }

        void accept(short[] samples48k, OathMatcher matcher) {
            int length = samples48k.length / DOWNSAMPLE;
            short[] samples = new short[length];
            for (int i = 0; i < length; i++) {
                int base = i * DOWNSAMPLE;
                samples[i] = (short) ((samples48k[base] + samples48k[base + 1] + samples48k[base + 2]) / DOWNSAMPLE);
            }
            if (this.recognizer.acceptWaveForm(samples, length)) {
                String[] heard = words(this.recognizer.getResult(), "text");
                if (heard.length > 0) BrightestDay.LOGGER.debug("Oath heard: {}", String.join(" ", heard));
                matcher.result(heard);
            } else {
                matcher.partial(words(this.recognizer.getPartialResult(), "partial"));
            }
        }

        void close() {
            this.recognizer.close();
        }

        private static String[] words(String json, String field) {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            String text = object.has(field) ? object.get(field).getAsString().trim() : "";
            return text.isEmpty() ? new String[0] : text.split("\\s+");
        }
    }

    private OathRecognizer() {}
}
