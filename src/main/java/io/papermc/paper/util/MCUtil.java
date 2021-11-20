/*
 * Licensed under https://github.com/PaperMC/Paper/blob/master/licenses/MIT.md
 */

package io.papermc.paper.util;

import net.minecraft.util.math.ChunkPos;

public class MCUtil {

    public static long getCoordinateKey(final int x, final int z) {
        return ((long)z << 32) | (x & 0xFFFFFFFFL);
    }

    public static long getCoordinateKey(final ChunkPos pair) {
        return ((long)pair.z << 32) | (pair.x & 0xFFFFFFFFL);
    }

    public static int getCoordinateX(final long key) {
        return (int)key;
    }

    public static int getCoordinateZ(final long key) {
        return (int)(key >>> 32);
    }

}
