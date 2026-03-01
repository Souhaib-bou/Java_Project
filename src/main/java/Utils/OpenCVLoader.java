package Utils;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;

public class OpenCVLoader {

    private static boolean loaded = false;

    public static synchronized void load() {
        if (!loaded) {
            Loader.load(opencv_core.class);
            loaded = true;
        }
    }
}
