package io.papermc.paper.lab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Lab instrumentation (enabled with -Dlab.census=true): how parallel could the entity phase be?
 * Every entity tick is timed and tagged with its chunk. Every ANALYSE_EVERY ticks the entities are grouped
 * into connected components (chunks within D chunks, Chebyshev, belong to the same component: that is the
 * assumed interaction radius) and the best makespan on 2, 3 and 4 threads (longest-processing-time first) is
 * computed. The ratio total / makespan is the ideal speedup of the entity phase for that radius.
 */
public final class EntityTickCensus {
    public static final boolean ON = Boolean.getBoolean("lab.census");
    private static final int ANALYSE_EVERY = 20, REPORT_EVERY = 600;
    private static final int[] RADII = {3, 6, 12};

    private final String name;
    private int n;
    private int[] cx = new int[4096], cz = new int[4096];
    private long[] ns = new long[4096];
    private long phaseStart, tickCount, analysed;
    private long sumTotalNs, sumPhaseNs, sumEntities;
    private final double[][] sums = new double[RADII.length][7]; // comps, top1 share, top2 share, speedup2, speedup3, speedup4, chunks
    private final Map<String, long[]> byType = new HashMap<>();

    public EntityTickCensus(final String name) { this.name = name; }

    public void begin() { n = 0; phaseStart = System.nanoTime(); }

    public void record(final String type, final int chunkX, final int chunkZ, final long nanos) {
        if (n == cx.length) { cx = Arrays.copyOf(cx, n * 2); cz = Arrays.copyOf(cz, n * 2); ns = Arrays.copyOf(ns, n * 2); }
        cx[n] = chunkX; cz[n] = chunkZ; ns[n++] = nanos;
        final long[] t = this.byType.computeIfAbsent(type, k -> new long[2]);
        t[0] += nanos; t[1]++;
    }

    public void end() {
        tickCount++;
        final long phase = System.nanoTime() - phaseStart;
        if (tickCount % ANALYSE_EVERY == 0 && n > 0) {
            long total = 0; for (int i = 0; i < n; i++) total += ns[i];
            sumTotalNs += total; sumPhaseNs += phase; sumEntities += n; analysed++;
            for (int r = 0; r < RADII.length; r++) analyse(r, total);
        }
        if (tickCount % REPORT_EVERY == 0 && analysed > 0) report();
    }

    private static long key(final int x, final int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    private void analyse(final int ri, final long total) {
        final int d = RADII[ri];
        final HashMap<Long, Integer> idx = new HashMap<>();
        final ArrayList<int[]> pos = new ArrayList<>();
        final ArrayList<long[]> w = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final Integer id = idx.get(key(cx[i], cz[i]));
            if (id == null) { idx.put(key(cx[i], cz[i]), pos.size()); pos.add(new int[]{cx[i], cz[i]}); w.add(new long[]{ns[i]}); }
            else w.get(id)[0] += ns[i];
        }
        final int m = pos.size();
        final int[] parent = new int[m];
        for (int i = 0; i < m; i++) parent[i] = i;
        for (int i = 0; i < m; i++) {
            final int[] p = pos.get(i);
            for (int dx = -d; dx <= d; dx++) for (int dz = -d; dz <= d; dz++) {
                if (dx < 0 || (dx == 0 && dz <= 0)) continue; // half window, symmetric
                final Integer j = idx.get(key(p[0] + dx, p[1] + dz));
                if (j != null) union(parent, i, j);
            }
        }
        final HashMap<Integer, long[]> comp = new HashMap<>();
        for (int i = 0; i < m; i++) comp.computeIfAbsent(find(parent, i), k -> new long[1])[0] += w.get(i)[0];
        final long[] c = comp.values().stream().mapToLong(a -> a[0]).sorted().toArray();
        final double[] s = sums[ri];
        s[0] += c.length;
        s[1] += (double) c[c.length - 1] / total;
        s[2] += c.length > 1 ? (double) c[c.length - 2] / total : 0;
        for (int p = 2; p <= 4; p++) s[p + 1] += (double) total / makespan(c, p);
        s[6] += m;
    }

    private static long makespan(final long[] ascending, final int threads) {
        final long[] load = new long[threads];
        for (int i = ascending.length - 1; i >= 0; i--) {
            int min = 0; for (int t = 1; t < threads; t++) if (load[t] < load[min]) min = t;
            load[min] += ascending[i];
        }
        long mx = 0; for (final long l : load) mx = Math.max(mx, l);
        return mx;
    }

    private static int find(final int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }
    private static void union(final int[] p, final int a, final int b) { final int ra = find(p, a), rb = find(p, b); if (ra != rb) p[ra] = rb; }

    private void report() {
        final double a = analysed;
        final StringBuilder sb = new StringBuilder("CENSUS ").append(name).append(" ticks=").append(tickCount)
            .append(String.format(" entities=%.0f entityMs=%.2f phaseMs=%.2f", sumEntities / a, sumTotalNs / a / 1e6, sumPhaseNs / a / 1e6));
        for (int r = 0; r < RADII.length; r++) {
            final double[] s = sums[r];
            sb.append(String.format(" | D=%d comps=%.1f chunks=%.0f top1=%.0f%% top2=%.0f%% ideal2=%.2fx ideal3=%.2fx ideal4=%.2fx",
                RADII[r], s[0] / a, s[6] / a, 100 * s[1] / a, 100 * s[2] / a, s[3] / a, s[4] / a, s[5] / a));
        }
        final ArrayList<Map.Entry<String, long[]>> types = new ArrayList<>(this.byType.entrySet());
        types.sort((x, y) -> Long.compare(y.getValue()[0], x.getValue()[0]));
        final StringBuilder tb = new StringBuilder("CENSUS_TYPES ").append(name);
        for (int i = 0; i < Math.min(12, types.size()); i++) {
            final var e = types.get(i);
            tb.append(String.format(" %s=%.2fms/tick(n=%.0f)", e.getKey(), e.getValue()[0] / (double) tickCount / 1e6, e.getValue()[1] / (double) tickCount));
        }
        net.minecraft.server.MinecraftServer.LOGGER.info(sb.toString());
        net.minecraft.server.MinecraftServer.LOGGER.info(tb.toString());
    }
}
