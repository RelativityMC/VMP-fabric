package io.papermc.paper.util.misc;

public class MCUtil {

    public static long getCoordinateKey(final int x, final int z) {
        return ((long)z << 32) | (x & 0xFFFFFFFFL);
    }

}
