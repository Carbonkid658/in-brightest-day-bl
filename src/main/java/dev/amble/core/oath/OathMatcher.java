package dev.amble.core.oath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OathMatcher {
    private static final int SKIP = 2;
    private static final int FUZZY_MIN_LENGTH = 4;

    private final List<String> words;
    private volatile int committed;
    private volatile int shown;

    public OathMatcher(List<String> words) {
        this.words = words;
    }

    public static List<String> tokenize(String oath) {
        List<String> words = new ArrayList<>();
        for (String token : oath.trim().split("\\s+")) words.add(normalize(token));
        return words;
    }

    public static String normalize(String token) {
        return token.toLowerCase(Locale.ROOT).replaceAll("[^a-z']", "");
    }

    public void partial(String[] heard) {
        this.shown = Math.max(this.shown, this.advance(this.committed, heard));
    }

    public void result(String[] heard) {
        this.committed = Math.max(this.committed, this.advance(this.committed, heard));
        this.shown = Math.max(this.shown, this.committed);
    }

    public int reached() {
        return this.shown;
    }

    public float progress() {
        return this.words.isEmpty() ? 1.0F : Math.min(1.0F, this.shown / (float) (this.words.size() - 1));
    }

    public boolean complete() {
        return this.shown >= this.words.size() - 1;
    }

    private int advance(int from, String[] heard) {
        int position = from;
        for (String word : heard) {
            if (word.isEmpty() || word.equals("[unk]")) continue;
            int limit = Math.min(this.words.size(), position + SKIP + 1);
            for (int j = position; j < limit; j++) {
                if (matches(this.words.get(j), word)) {
                    position = j + 1;
                    break;
                }
            }
        }
        return position;
    }

    private static boolean matches(String expected, String heard) {
        if (expected.isEmpty() || expected.equals(heard)) return true;
        return expected.length() >= FUZZY_MIN_LENGTH && distance(expected, heard) <= 1;
    }

    private static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
