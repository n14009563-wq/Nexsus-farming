package com.nexuscraft.nexusfarming;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexusFarmingCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final FarmingConfig config;
    private final FertilityService fertility;
    private final ScarecrowService scarecrow;

    public NexusFarmingCommand(JavaPlugin plugin, FarmingConfig config, FertilityService fertility, ScarecrowService scarecrow) {
        this.plugin = plugin;
        this.config = config;
        this.fertility = fertility;
        this.scarecrow = scarecrow;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            return status(sender);
        }
        if (args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        sender.sendMessage("§cUsage: /nexusfarming <status|reload>");
        return true;
    }

    private boolean status(CommandSender sender) {
        sender.sendMessage("§6[NexusFarming] §fHold a golden hoe: right-click a mature crop to auto-replant it, "
                + "sneak + right-click to reap a whole patch, right-click an unripe one to nudge its growth.");
        sender.sendMessage("§7Bonus yield: §f" + percent(config.bonusYieldBaseChance) + " base, +"
                + percent(config.fortuneBonusPerLevel) + " per Fortune level, capped at " + percent(config.bonusYieldChanceCap) + ".");
        if (config.fertilityEnabled) {
            sender.sendMessage("§7Fertility: §f" + fertility.trackedTileCount() + " tile(s) tracked, up to +"
                    + percent(config.fertilityBonusPerLevel) + " each at level " + config.fertilityMaxLevel + ".");
        }
        if (config.goldenCropsEnabled) {
            sender.sendMessage("§7Golden Crops: §f" + percent(config.goldenCropChance) + " chance per harvest.");
        }
        if (config.scarecrowEnabled) {
            sender.sendMessage("§7Scarecrows: §f" + scarecrow.anchorCount() + " active, radius " + config.scarecrowRadius + ".");
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("nexusfarming.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        plugin.reloadConfig();
        config.load();
        sender.sendMessage("§a[NexusFarming] §fConfig reloaded.");
        return true;
    }

    private static String percent(double fraction) {
        return Math.round(fraction * 1000.0) / 10.0 + "%";
    }
}
