package io.papermc.paper.entity.activation;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

/**
 * Lab option (off by default, behaviour changing on purpose): entities that are farther than a configured horizontal distance from every
 * player run their full tick only on one of every N ticks. The other ticks take the same inactive path Paper uses for entities outside the
 * activation range. Everything within the distance of a player is untouched.
 */
public final class FarEntityThrottle {
    public static final byte NONE = 0, MONSTER = 1, ANIMAL = 2, VILLAGER = 3, OTHER_MOB = 4, XP = 5, VEHICLE = 6;
    private static final int MIN_AGE = 200; // the same grace Paper's activation range gives new entities

    private final ServerLevel level;
    private final Long2BooleanOpenHashMap far = new Long2BooleanOpenHashMap();
    private final boolean[] categoryOn = new boolean[7];
    private boolean enabled;
    private int interval = 4;
    private long distanceSq;
    private long nowTick = Long.MIN_VALUE;
    /** Throttled (skipped) entity ticks since start, for lab statistics. */
    public long skipped;
    /** Lab statistics: set by the last shouldThrottle call that returned true, cleared by the caller. */
    public boolean lastThrottled;

    public FarEntityThrottle(final ServerLevel level) {
        this.level = level;
    }

    public static byte categoryOf(final Entity entity) {
        if (entity instanceof Villager) {
            return VILLAGER;
        }
        if (entity instanceof ExperienceOrb) {
            return XP;
        }
        if (entity instanceof Mob) {
            if (entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof Raider) {
                return NONE; // bosses and raiders are driven by events elsewhere
            }
            if (entity instanceof Animal) {
                return ANIMAL;
            }
            return entity instanceof Enemy ? MONSTER : OTHER_MOB;
        }
        if (entity instanceof AbstractMinecart || entity instanceof AbstractBoat) {
            return VEHICLE;
        }
        return NONE;
    }

    /** Reads the config once per server tick. Call before the entity loop. */
    public void beginTick() {
        final io.papermc.paper.configuration.WorldConfiguration.Entities.FarEntityThrottle cfg = this.level.paperConfig().entities.farEntityThrottle;
        this.enabled = cfg.enabled;
        if (!this.enabled) {
            return;
        }
        this.interval = Math.max(2, cfg.interval);
        final long d = Math.max(0, cfg.distance);
        this.distanceSq = d * d;
        this.categoryOn[MONSTER] = cfg.monsters;
        this.categoryOn[ANIMAL] = cfg.animals;
        this.categoryOn[VILLAGER] = cfg.villagers;
        this.categoryOn[OTHER_MOB] = cfg.otherMobs;
        this.categoryOn[XP] = cfg.experienceOrbs;
        this.categoryOn[VEHICLE] = cfg.vehicles;
        this.far.clear();
        this.nowTick = MinecraftServer.currentTick;
    }

    public boolean enabled() {
        return this.enabled;
    }

    /** True when this entity, which Paper's activation range says is active, should only take the inactive path this tick. */
    public boolean shouldThrottle(final Entity entity) {
        final byte category = entity.farThrottleCategory;
        if (category == NONE || !this.categoryOn[category]) {
            return false;
        }
        final int tickCount = entity.tickCount;
        if (tickCount < MIN_AGE || (tickCount + entity.getId()) % this.interval == 0) {
            return false;
        }
        if (category == XP && tickCount % 20 == 1) {
            return false; // the merge scan tick
        }
        if (entity.isVehicle() || entity.isPassenger() || entity.portalProcess != null || entity.getPortalCooldown() > 0 || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof final Mob mob && (mob.getTarget() != null || mob.isLeashed())) {
            return false;
        }
        if (!this.isFar(entity)) {
            return false;
        }
        this.skipped++;
        this.lastThrottled = true;
        return true;
    }

    private boolean isFar(final Entity entity) {
        final int cx = net.minecraft.util.Mth.floor(entity.getX()) >> 4;
        final int cz = net.minecraft.util.Mth.floor(entity.getZ()) >> 4;
        final long key = (cx & 0xFFFFFFFFL) | ((long)cz << 32);
        if (this.far.containsKey(key)) {
            return this.far.get(key);
        }
        final long minX = (long)cx << 4, maxX = minX + 15, minZ = (long)cz << 4, maxZ = minZ + 15;
        boolean isFar = true;
        for (final ServerPlayer player : this.level.players()) {
            final double px = player.getX(), pz = player.getZ();
            final double dx = Math.max(Math.max(minX - px, px - maxX), 0.0);
            final double dz = Math.max(Math.max(minZ - pz, pz - maxZ), 0.0);
            if (dx * dx + dz * dz <= this.distanceSq) {
                isFar = false;
                break;
            }
        }
        this.far.put(key, isFar);
        return isFar;
    }
}
