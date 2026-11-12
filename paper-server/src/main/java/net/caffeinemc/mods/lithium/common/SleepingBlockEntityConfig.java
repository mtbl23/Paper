package net.caffeinemc.mods.lithium.common;

/**
 * Master switch for the Lithium-derived sleeping block entity, hopper caching and inventory change tracking code
 * (LGPL-3.0, CaffeineMC/lithium-fabric, ported to Paper/Moonrise by Leaves/Leaf).
 *
 * <p>Enabled by default. Start the server with {@code -Dpaper.sleepingBlockEntity=false} to run the vanilla/Paper code paths
 * (used by the differential tests as the oracle).</p>
 */
public final class SleepingBlockEntityConfig {
    public static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("paper.sleepingBlockEntity"));

    private SleepingBlockEntityConfig() {
    }
}
