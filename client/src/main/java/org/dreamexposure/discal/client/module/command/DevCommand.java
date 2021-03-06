package org.dreamexposure.discal.client.module.command;

import org.dreamexposure.discal.client.DisCalClient;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.crypto.KeyGenerator;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.enums.network.CrossTalkReason;
import org.dreamexposure.discal.core.enums.network.DisCalRealm;
import org.dreamexposure.discal.core.object.BotSettings;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.command.CommandInfo;
import org.dreamexposure.discal.core.object.web.UserAPIAccount;
import org.dreamexposure.discal.core.utils.GlobalConst;
import org.dreamexposure.novautils.network.crosstalk.ClientSocketHandler;
import org.json.JSONObject;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;

/**
 * Created by Nova Fox on 4/4/2017.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "Duplicates"})
public class DevCommand implements ICommand {

	private ScriptEngine factory = new ScriptEngineManager().getEngineByName("nashorn");

	/**
	 * Gets the command this Object is responsible for.
	 *
	 * @return The command this Object is responsible for.
	 */
	@Override
	public String getCommand() {
		return "dev";
	}

	/**
	 * Gets the short aliases of the command this object is responsible for.
	 * </br>
	 * This will return an empty ArrayList if none are present
	 *
	 * @return The aliases of the command.
	 */
	@Override
	public ArrayList<String> getAliases() {
		return new ArrayList<>();
	}

	/**
	 * Gets the info on the command (not sub command) to be used in help menus.
	 *
	 * @return The command info.
	 */
	@Override
	public CommandInfo getCommandInfo() {
		CommandInfo ci = new CommandInfo("dev");
		ci.setDescription("Used for developer commands. Only able to be used by registered developers");
		ci.setExample("!dev <function> (value)");
		ci.getSubCommands().put("reloadLangs", "Reloads the lang files across the network.");
		ci.getSubCommands().put("patron", "Sets a guild as a patron.");
		ci.getSubCommands().put("dev", "Sets a guild as a test/dev guild.");
		ci.getSubCommands().put("maxcal", "Sets the max amount of calendars a guild may have.");
		ci.getSubCommands().put("leave", "Leaves the specified guild.");
		ci.getSubCommands().put("eval", "Evaluates the given code.");
		ci.getSubCommands().put("api-register", "Register new API key");
		ci.getSubCommands().put("api-block", "Block API usage by key");
		ci.getSubCommands().put("settings", "Checks the settings of the specified Guild.");

		return ci;
	}

	/**
	 * Issues the command this Object is responsible for.
	 *
	 * @param args  The command arguments.
	 * @param event The event received.
	 * @return <code>true</code> if successful, else <code>false</code>.
	 */
	@Override
	public boolean issueCommand(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (event.getAuthor().getLongID() == GlobalConst.novaId || event.getAuthor().getLongID() == GlobalConst.xaanitId || event.getAuthor().getLongID() == GlobalConst.calId || event.getAuthor().getLongID() == GlobalConst.dreamId) {
			if (args.length < 1) {
				MessageManager.sendMessageAsync("Please specify the function you would like to execute. To view valid functions use `!help dev`", event);
			} else {
				switch (args[0].toLowerCase()) {
					case "reloadlangs":
						moduleReloadLangs(event);
						break;
					case "patron":
						modulePatron(args, event);
						break;
					case "dev":
						moduleDevGuild(args, event);
						break;
					case "maxcal":
						moduleMaxCalendars(args, event);
						break;
					case "leave":
						moduleLeaveGuild(args, event);
						break;
					case "eval":
						moduleEval(event);
						break;
					case "api-register":
						registerApiKey(args, event);
						break;
					case "api-block":
						blockAPIKey(args, event);
						break;
					case "settings":
						moduleCheckSettings(args, event);
						break;
					default:
						MessageManager.sendMessageAsync("Invalid sub command! Use `!help dev` to view valid sub commands!", event);
						break;
				}
			}
		} else {
			MessageManager.sendMessageAsync("You are not a registered DisCal developer! If this is a mistake please contact Nova!", event);
		}
		return false;
	}

	private void modulePatron(String[] args, MessageReceivedEvent event) {
		if (args.length == 2) {
			try {
				Long.valueOf(args[1]);
			} catch (NumberFormatException ignore) {
				MessageManager.sendMessageAsync("Specified ID is not a valid LONG", event);
				return;
			}

			//Check if its on this shard...
			if (DisCalClient.getClient().getGuildByID(Long.valueOf(args[1])) != null) {
				GuildSettings settings = DatabaseManager.getManager().getSettings(Long.valueOf(args[1]));
				settings.setPatronGuild(!settings.isPatronGuild());
				DatabaseManager.getManager().updateSettings(settings);

				MessageManager.sendMessageAsync("Guild connected to this shard. isPatronGuild value updated!", event);
				return;
			}

			//Just send this across the network with CrossTalk... and let the changes propagate
			JSONObject request = new JSONObject();

			request.put("Reason", CrossTalkReason.HANDLE.name());
			request.put("Realm", DisCalRealm.GUILD_IS_PATRON);
			request.put("Guild-Id", args[1]);

			ClientSocketHandler.sendToServer(Integer.valueOf(BotSettings.SHARD_INDEX.get()), request);

			MessageManager.sendMessageAsync("DisCal will update the isPatron status of the guild (if connected). Please allow some time for this to propagate across the network!", event);
		} else {
			MessageManager.sendMessageAsync("Please specify the ID of the guild to set as a patron guild with `!dev patron <ID>`", event);
		}
	}

	@SuppressWarnings("all")
	private void moduleEval(MessageReceivedEvent event) {
		IGuild guild = event.getGuild();
		IUser user = event.getAuthor();
		IMessage message = event.getMessage();
		IDiscordClient client = event.getClient();
		IChannel channel = event.getMessage().getChannel();
		String input = message.getContent().substring(message.getContent().indexOf("eval") + 5).replaceAll("`", "");
		Object o = null;
		factory.put("guild", guild);
		factory.put("channel", channel);
		factory.put("user", user);
		factory.put("message", message);
		factory.put("command", this);
		factory.put("client", client);
		factory.put("builder", new EmbedBuilder());
		factory.put("cUser", client.getOurUser());

		try {
			o = factory.eval(input);
		} catch (Exception ex) {
			EmbedBuilder em = new EmbedBuilder();
			em.withAuthorName("DisCal");
			em.withAuthorUrl(GlobalConst.discalSite);
			em.withAuthorIcon(GlobalConst.iconUrl);
			em.withTitle("Error");
			em.appendDesc(ex.getMessage());
			em.withFooterText("Eval failed");
			em.withColor(GlobalConst.discalColor);
			MessageManager.sendMessageAsync(em.build(), event);
			return;
		}

		EmbedBuilder em = new EmbedBuilder();
		em.withAuthorName("DisCal");
		em.withAuthorUrl(GlobalConst.discalSite);
		em.withAuthorIcon(GlobalConst.iconUrl);
		em.withTitle("Success! -- Eval Output.");
		em.withColor(GlobalConst.discalColor);
		em.appendDesc(o == null ? "No output, object is null" : o.toString());
		em.appendField("Input", "```java\n" + input + "\n```", false);
		em.withFooterText("Eval successful!");
		MessageManager.sendMessageAsync(em.build(), event);
	}

	private void moduleDevGuild(String[] args, MessageReceivedEvent event) {
		if (args.length == 2) {
			try {
				Long.valueOf(args[1]);
			} catch (NumberFormatException ignore) {
				MessageManager.sendMessageAsync("Specified ID is not a valid LONG", event);
				return;
			}
			//Check if its on this shard...
			if (DisCalClient.getClient().getGuildByID(Long.valueOf(args[1])) != null) {
				GuildSettings settings = DatabaseManager.getManager().getSettings(Long.valueOf(args[1]));
				settings.setDevGuild(!settings.isDevGuild());
				DatabaseManager.getManager().updateSettings(settings);

				MessageManager.sendMessageAsync("Guild connected to this shard. isDevGuild value updated!", event);
				return;
			}

			//Just send this across the network with CrossTalk... and let the changes propagate
			JSONObject request = new JSONObject();

			request.put("Reason", CrossTalkReason.HANDLE.name());
			request.put("Realm", DisCalRealm.GUILD_IS_DEV);
			request.put("Guild-Id", args[1]);

			ClientSocketHandler.sendToServer(Integer.valueOf(BotSettings.SHARD_INDEX.get()), request);

			MessageManager.sendMessageAsync("DisCal will update the isDevGuild status of the guild (if connected). Please allow some time for this to propagate across the network!", event);
		} else {
			MessageManager.sendMessageAsync("Please specify the ID of the guild to set as a dev guild with `!dev dev <ID>`", event);
		}
	}

	private void moduleMaxCalendars(String[] args, MessageReceivedEvent event) {
		if (args.length == 3) {
			try {
				int mc = Integer.valueOf(args[2]);
				mc = Math.abs(mc);

				try {
					Long.valueOf(args[1]);
				} catch (NumberFormatException ignore) {
					MessageManager.sendMessageAsync("Specified ID is not a valid LONG", event);
					return;
				}

				//Check if its on this shard...
				if (DisCalClient.getClient().getGuildByID(Long.valueOf(args[1])) != null) {
					GuildSettings settings = DatabaseManager.getManager().getSettings(Long.valueOf(args[1]));
					settings.setMaxCalendars(mc);
					DatabaseManager.getManager().updateSettings(settings);

					MessageManager.sendMessageAsync("Guild connected to this shard. Max calendar value has been updated!", event);
					return;
				}

				//Just send this across the network with CrossTalk... and let the changes propagate
				JSONObject request = new JSONObject();

				request.put("Reason", CrossTalkReason.HANDLE.name());
				request.put("Realm", DisCalRealm.GUILD_MAX_CALENDARS);
				request.put("Guild-Id", args[1]);
				request.put("Max-Calendars", mc);

				ClientSocketHandler.sendToServer(Integer.valueOf(BotSettings.SHARD_INDEX.get()), request);

				MessageManager.sendMessageAsync("DisCal will update the max calendar limit of the specified guild (if connected). Please allow some time for this to propagate across the network!", event);
			} catch (NumberFormatException e) {
				MessageManager.sendMessageAsync("Max Calendar amount must be a valid Integer!", event);
			}
		} else {
			MessageManager.sendMessageAsync("Please specify the ID of the guild and calendar amount with `!dev maxcal <ID> <amount>`", event);
		}
	}

	private void moduleLeaveGuild(String[] args, MessageReceivedEvent event) {
		if (args.length == 2) {
			try {
				Long.valueOf(args[1]);
			} catch (NumberFormatException ignore) {
				MessageManager.sendMessageAsync("Specified ID is not a valid LONG", event);
				return;
			}

			//Check if its on this shard...
			IGuild g = DisCalClient.getClient().getGuildByID(Long.valueOf(args[1]));
			if (g != null) {
				g.leave();

				MessageManager.sendMessageAsync("Guild connected to this shard has been left!", event);
				return;
			}

			//Just send this across the network with CrossTalk... and let the changes propagate
			JSONObject request = new JSONObject();

			request.put("Reason", CrossTalkReason.HANDLE.name());
			request.put("Realm", DisCalRealm.GUILD_LEAVE);
			request.put("Guild-Id", args[1]);

			ClientSocketHandler.sendToServer(Integer.valueOf(BotSettings.SHARD_INDEX.get()), request);

			MessageManager.sendMessageAsync("DisCal will leave the specified guild (if connected). Please allow some time for this to propagate across the network!", event);
		} else {
			MessageManager.sendMessageAsync("Please specify the ID of the guild to leave with `!dev leave <ID>`", event);
		}
	}

	private void moduleReloadLangs(MessageReceivedEvent event) {
		MessageManager.reloadLangs();

		//Just send this across the network with CrossTalk... and let the changes propagate
		JSONObject request = new JSONObject();

		request.put("Reason", CrossTalkReason.HANDLE.name());
		request.put("Realm", DisCalRealm.BOT_LANGS);

		ClientSocketHandler.sendToServer(Integer.valueOf(BotSettings.SHARD_INDEX.get()), request);

		MessageManager.sendMessageAsync("Reloading lang files! Please give this time to propagate across the network.", event);
	}


	private void registerApiKey(String[] args, MessageReceivedEvent event) {
		if (args.length == 2) {
			MessageManager.sendMessageAsync("Registering new API key...", event);

			String userId = args[1];

			UserAPIAccount account = new UserAPIAccount();
			account.setUserId(userId);
			account.setAPIKey(KeyGenerator.csRandomAlphaNumericString(64));
			account.setTimeIssued(System.currentTimeMillis());
			account.setBlocked(false);
			account.setUses(0);

			if (DatabaseManager.getManager().updateAPIAccount(account)) {
				MessageManager.sendMessageAsync("Check your DMs for the new API Key!", event);
				MessageManager.sendDirectMessageAsync(account.getAPIKey(), event.getAuthor());
			} else {
				MessageManager.sendMessageAsync("Error occurred! Could not register new API key!", event);
			}
		} else {
			MessageManager.sendMessageAsync("Please specify the USER ID linked to the key!", event);
		}
	}

	private void blockAPIKey(String[] args, MessageReceivedEvent event) {
		if (args.length == 2) {
			MessageManager.sendMessageAsync("Blocking API key...", event);

			String key = args[1];

			UserAPIAccount account = DatabaseManager.getManager().getAPIAccount(key);
			account.setBlocked(true);

			if (DatabaseManager.getManager().updateAPIAccount(account))
				MessageManager.sendMessageAsync("Successfully blocked API key!", event);
			else
				MessageManager.sendMessageAsync("Error occurred! Could not block API key!", event);
		} else {
			MessageManager.sendMessageAsync("Please specify the API KEY!", event);
		}
	}

	private void moduleCheckSettings(String[] args, MessageReceivedEvent event) {
		if (args.length == 2) {
			//String id = args[1];

			MessageManager.sendMessageAsync("HEY! This command is being redone cuz of networking!", event);

			//TODO: Send/Receive from crosstalk.
			/*
			try {

				IGuild guild = DisCalAPI.getAPI().getClient().getGuildByID(Long.valueOf(id));

				if (guild != null) {
					GuildSettings settings = DatabaseManager.getManager().getSettings(guild.getLongID());

					EmbedBuilder em = new EmbedBuilder();
					em.withAuthorIcon(DisCalAPI.getAPI().iconUrl);
					em.withAuthorName("DisCal");
					em.withTitle(MessageManager.getMessage("Embed.DisCal.Settings.Title", settings));
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.ExternalCal", settings), String.valueOf(settings.useExternalCalendar()), true);
					if (RoleUtils.roleExists(settings.getControlRole(), guild)) {
						em.appendField(MessageManager.getMessage("Embed.Discal.Settings.Role", settings), RoleUtils.getRoleNameFromID(settings.getControlRole(), guild), true);
					} else {
						em.appendField(MessageManager.getMessage("Embed.Discal.Settings.Role", settings), "everyone", true);
					}
					if (ChannelUtils.channelExists(settings.getDiscalChannel(), guild)) {
						em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.Channel", settings), ChannelUtils.getChannelNameFromNameOrId(settings.getDiscalChannel(), guild.getLongID()), false);
					} else {
						em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.Channel", settings), "All Channels", true);
					}
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.SimpleAnn", settings), String.valueOf(settings.usingSimpleAnnouncements()), true);
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.Patron", settings), String.valueOf(settings.isPatronGuild()), true);
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.Dev", settings), String.valueOf(settings.isDevGuild()), true);
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.MaxCal", settings), String.valueOf(settings.getMaxCalendars()), true);
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.Language", settings), settings.getLang(), true);
					em.appendField(MessageManager.getMessage("Embed.DisCal.Settings.Prefix", settings), settings.getPrefix(), true);
					//TODO: Add translations...
					em.appendField("Using Branding", settings.isBranded() + "", true);
					em.withFooterText(MessageManager.getMessage("Embed.DisCal.Info.Patron", settings) + ": https://www.patreon.com/Novafox");
					em.withUrl("https://www.discalbot.com/");
					em.withColor(56, 138, 237);
					MessageManager.sendMessage(em.build(), event);
				} else {
					MessageManager.sendMessage("The specified guild is not connected to DisCal or does not Exist", event);
				}
			} catch (Exception e) {
				MessageManager.sendMessage("Guild ID must be of type long!", event);
			}
			*/
		} else {
			MessageManager.sendMessageAsync("Please specify the ID of the guild to check settings for!", event);
		}
	}
}