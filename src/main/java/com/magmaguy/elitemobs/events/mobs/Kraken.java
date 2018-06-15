package com.magmaguy.elitemobs.events.mobs;

import com.magmaguy.elitemobs.ChatColorConverter;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.config.ConfigValues;
import com.magmaguy.elitemobs.config.EventsConfig;
import com.magmaguy.elitemobs.elitedrops.UniqueItemConstructor;
import com.magmaguy.elitemobs.mobpowers.ProjectileLocationGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Kraken implements Listener {

    public static void spawnKraken(Location location) {

        krakenSpawnEffect(location);

    }

    private static void krakenSpawnEffect(Location location) {

        new BukkitRunnable() {

            int i = 0;

            @Override
            public void run() {

                if (i > 20 * 3) {

                    initializeKraken(location);
                    cancel();
                    return;

                }

                location.getWorld().spawnParticle(Particle.PORTAL, location, 20, 1, 1, 1, 0.1);

                i++;

            }


        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);

    }

    private static void initializeKraken(Location location) {

        Squid kraken = (Squid) location.getWorld().spawnEntity(location, EntityType.SQUID);

        kraken.setMaxHealth(1000);
        kraken.setHealth(1000);

        kraken.setCustomName(ChatColorConverter.chatColorConverter(ConfigValues.eventsConfig.getString(EventsConfig.KRAKEN_NAME)));
        kraken.setCustomNameVisible(true);

        kraken.setMetadata(MetadataHandler.KRAKEN, new FixedMetadataValue(MetadataHandler.PLUGIN, true));
        kraken.setMetadata(MetadataHandler.ELITE_MOB_MD, new FixedMetadataValue(MetadataHandler.PLUGIN, 500));
        kraken.setMetadata(MetadataHandler.EVENT_CREATURE, new FixedMetadataValue(MetadataHandler.PLUGIN, true));

        krakenDamageLoop(kraken);
        krakenVisualEffectLoop(kraken);

        BossMobDeathCountdown.startDeathCountdown(kraken);

    }

    private static void krakenDamageLoop(Squid kraken) {

        new BukkitRunnable() {

            @Override
            public void run() {

                if (kraken.isDead() || !kraken.isValid()) {

                    cancel();
                    return;

                }

                for (Entity entity : kraken.getNearbyEntities(16, 16, 16)) {

                    if (entity instanceof Player) {

                        fireballInitializer(kraken, (Player) entity);

                    }

                }

            }

        }.runTaskTimer(MetadataHandler.PLUGIN, 20 * 3, 20 * 3);

    }

    private static void fireballInitializer(Squid kraken, Player target) {

        Location offsetLocation = ProjectileLocationGenerator.generateLocation(kraken, target);
        Fireball repeatingFireball = (Fireball) kraken.getWorld().spawnEntity(offsetLocation, EntityType.FIREBALL);
        Vector targetterToTargetted = target.getEyeLocation().subtract(repeatingFireball.getLocation()).toVector()
                .normalize().multiply(1);

        repeatingFireball.setDirection(targetterToTargetted);
        repeatingFireball.setIsIncendiary(true);
        repeatingFireball.setYield(2F);

        repeatingFireball.setMetadata(MetadataHandler.KRAKEN_FIREBALL, new FixedMetadataValue(MetadataHandler.PLUGIN, true));

//        repeatingFireball.setShooter(kraken);

    }

    private static void krakenVisualEffectLoop(Squid kraken) {

        new BukkitRunnable() {

            @Override
            public void run() {

                if (kraken.isDead() || !kraken.isValid()) {

                    cancel();
                    return;

                }

                kraken.getWorld().spawnParticle(Particle.FLAME, kraken.getLocation(), 10, 0.1, 0.1, 0.1, 0.05);

            }

        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);

    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {

        if (event.isCancelled()) return;

        if (event.getEntity().hasMetadata(MetadataHandler.KRAKEN) && event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FIRE)) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void onSelfDamage(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Fireball && ((Fireball) event.getDamager()).getShooter() != null &&
                ((Fireball) event.getDamager()).getShooter() instanceof Squid) {

            event.setCancelled(true);

        }

    }

    @EventHandler
    public void fireballDamage(EntityDamageByEntityEvent event) {

        if (event.isCancelled()) return;

        if (event.getEntity() instanceof  Player && event.getDamager().getType().equals(EntityType.FIREBALL) &&
                event.getDamager().hasMetadata(MetadataHandler.KRAKEN_FIREBALL)) {

            for (EntityDamageByEntityEvent.DamageModifier modifier : EntityDamageByEntityEvent.DamageModifier.values())
                if (event.isApplicable(modifier))
                    event.setDamage(modifier, 0);
            event.setDamage(EntityDamageEvent.DamageModifier.BASE, 2);

            event.getDamager().removeMetadata(MetadataHandler.KRAKEN_FIREBALL, MetadataHandler.PLUGIN);

        }

    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {

        if (event.getEntity().hasMetadata(MetadataHandler.KRAKEN)) {

            event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), UniqueItemConstructor.uniqueItems.get(UniqueItemConstructor.DEPTHS_SEEKER));

        }

    }

}
