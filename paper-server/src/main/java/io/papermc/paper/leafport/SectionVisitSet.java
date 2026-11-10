package io.papermc.paper.leafport;

import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * A "visited" set for chunk sections inside a fixed bounding box, backed by a bit set.
 * <p>
 * Idea ported from Leaf "Optimize PoiAccess visited section tracking" (HaHaWTH, co-authored by MrlingXD; GPL-3.0-only with
 * section 7(b) attribution term: Original patch by HaHaWTH &lt;102713261+HaHaWTH@users.noreply.github.com&gt;).
 * This class is an independent implementation. Boxes too large for a reasonable bit set fall back to a hash set.
 */
public final class SectionVisitSet {
    private static final long MAX_BITS = 1L << 20;

    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final long[] bits;
    private final LongOpenHashSet fallback;

    public SectionVisitSet(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        final long sizeX = (long)maxX - (long)minX + 1L;
        final long sizeY = (long)maxY - (long)minY + 1L;
        final long sizeZ = (long)maxZ - (long)minZ + 1L;
        final long count = sizeX * sizeY * sizeZ;
        if (sizeX <= 0L || sizeY <= 0L || sizeZ <= 0L || count > MAX_BITS) {
            this.sizeX = 0;
            this.sizeY = 0;
            this.bits = null;
            this.fallback = new LongOpenHashSet();
        } else {
            this.sizeX = (int)sizeX;
            this.sizeY = (int)sizeY;
            this.bits = new long[(int)((count + 63L) >>> 6)];
            this.fallback = null;
        }
    }

    /**
     * @return true if the section was not marked before (it is marked now). The section must be inside the box.
     */
    public boolean mark(final int x, final int y, final int z) {
        if (this.bits == null) {
            return this.fallback.add(CoordinateUtils.getChunkSectionKey(x, y, z));
        }

        final int index = ((z - this.minZ) * this.sizeY + (y - this.minY)) * this.sizeX + (x - this.minX);
        final int word = index >>> 6;
        final long bit = 1L << index;
        final long value = this.bits[word];
        if ((value & bit) != 0L) {
            return false;
        }

        this.bits[word] = value | bit;
        return true;
    }
}
