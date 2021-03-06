package me.confuser.banmanager.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.confuser.banmanager.BanManager;
import me.confuser.banmanager.Util;
import me.confuser.banmanager.data.WarnData;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarnCommand implements CommandExecutor {

	private BanManager plugin;

	public WarnCommand(BanManager instance) {
		plugin = instance;
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender sender, Command command, String commandLabel, final String args[]) {

		if (args.length < 2)
			return false;

		Player player = null;
		String playerName = plugin.getMessage("consoleName");

		if (sender instanceof Player) {
			player = (Player) sender;
			playerName = player.getName();
			if (!player.hasPermission("bm.warn")) {
				Util.sendMessage(player, plugin.getMessage("commandPermissionError"));
				return true;
			}
		}

		if (!Util.isValidPlayerName(args[0])) {
			Util.sendMessage(sender, plugin.getMessage("invalidPlayer"));
			return true;
		}

		List<Player> list = plugin.getServer().matchPlayer(args[0]);
		if (list.size() == 1) {
			final Player target = list.get(0);
			if (target.getName().equals(playerName)) {
				Util.sendMessage(sender, plugin.getMessage("warnSelfError"));
			} else if (!sender.hasPermission("bm.exempt.override.warn") && target.hasPermission("bm.exempt.warn")) {
				Util.sendMessage(sender, plugin.getMessage("warnExemptError"));
			} else {

				final String pName = playerName;

				plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {

					public void run() {

						if (plugin.enableWarningCooldown()) {
							ArrayList<WarnData> warnings = plugin.dbLogger.getWarnings(target.getName());
							if (warnings.size() > 0) {
								WarnData data = warnings.get(warnings.size() - 1);
								long last = data.getTime();
								long now = System.currentTimeMillis() / 1000L;

								if ((now - last) <= plugin.getWarningCooldown()) {
									Util.sendMessage(sender, plugin.getMessage("warnCooldown"));
									return;
								}
							}
						}

						String reason = Util.getReason(args, 1);
						String viewReason = Util.viewReason(reason);

						plugin.dbLogger.logWarning(target.getName(), pName, reason);

						if (plugin.enableWarningActions()) {
							Map<Integer, String> actions = plugin.getWarningActions();
							if (actions.size() > 0) {
								int number = plugin.dbLogger.getWarningCount(target.getName()) + 1;
								if (actions.containsKey(number)) {
									final String actionCommand = actions.get(number).replace("[displayName]", target.getDisplayName()).replace("[name]", target.getName()).replace("[reason]", viewReason).replace("[by]", pName);

									plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

										public void run() {

											plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), actionCommand);

										}
									});
								}
							}
						}

						String infoMessage = plugin.getMessage("playerWarned").replace("[displayName]", target.getDisplayName()).replace("[name]", target.getName()).replace("[reason]", viewReason).replace("[by]", pName);

						plugin.getServer().getConsoleSender().sendMessage(infoMessage);

						if (!sender.hasPermission("bm.notify.warn"))
							Util.sendMessage(sender, infoMessage);

						String message = plugin.getMessage("playerWarned").replace("[displayName]", target.getDisplayName()).replace("[name]", target.getName()).replace("[reason]", viewReason).replace("[by]", pName);
						Util.sendMessageWithPerm(message, "bm.notify.warn");

						Util.sendMessage(target, plugin.getMessage("warned").replace("[displayName]", target.getDisplayName()).replace("[name]", target.getName()).replace("[reason]", viewReason).replace("[by]", pName));
					}
				});
			}
		} else if (list.size() > 1) {
			Util.sendMessage(sender, plugin.getMessage("multiplePlayersFoundError"));
			return false;
		} else {
			Util.sendMessage(sender, plugin.getMessage("playerNotOnline"));
			return false;
		}

		return true;
	}
}
