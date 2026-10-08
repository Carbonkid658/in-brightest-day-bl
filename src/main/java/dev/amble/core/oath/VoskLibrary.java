package dev.amble.core.oath;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

interface VoskLibrary extends Library {
    int LOG_WARNINGS = -1;

    void vosk_set_log_level(int level);

    Pointer vosk_model_new(String path);

    void vosk_model_free(Pointer model);

    Pointer vosk_recognizer_new_grm(Pointer model, float sampleRate, String grammar);

    boolean vosk_recognizer_accept_waveform_s(Pointer recognizer, short[] data, int length);

    String vosk_recognizer_result(Pointer recognizer);

    String vosk_recognizer_partial_result(Pointer recognizer);

    void vosk_recognizer_free(Pointer recognizer);

    static VoskLibrary load() throws ClassNotFoundException {
        if (Platform.isWindows()) {
            Class.forName("org.vosk.LibVosk", true, VoskLibrary.class.getClassLoader());
            return Native.load("libvosk", VoskLibrary.class);
        }
        return Native.load("vosk", VoskLibrary.class);
    }
}
