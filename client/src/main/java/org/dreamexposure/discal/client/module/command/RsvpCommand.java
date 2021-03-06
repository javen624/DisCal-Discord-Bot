package org.dreamexposure.discal.client.module.command;

import org.dreamexposure.discal.client.DisCalClient;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.command.CommandInfo;
import org.dreamexposure.discal.core.object.event.RsvpData;
import org.dreamexposure.discal.core.utils.EventUtils;
import org.dreamexposure.discal.core.utils.GlobalConst;
import org.dreamexposure.discal.core.utils.TimeUtils;
import org.dreamexposure.discal.core.utils.UserUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;

/**
 * Created by Nova Fox on 8/31/17.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
public class RsvpCommand implements ICommand {
	/**
	 * Gets the command this Object is responsible for.
	 *
	 * @return The command this Object is responsible for.
	 */
	@Override
	public String getCommand() {
		return "rsvp";
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
		CommandInfo info = new CommandInfo("rsvp");
		info.setDescription("Confirms attendance to an event");
		info.setExample("!rsvp <subCommand> <eventId>");

		info.getSubCommands().put("onTime", "Marks you are going to event");
		info.getSubCommands().put("late", "Marks that you will be late to event");
		info.getSubCommands().put("not", "Marks that you are NOT going to event");
		info.getSubCommands().put("unsure", "Marks that you may or may not go to event");
		info.getSubCommands().put("remove", "Removes your RSVP from the event");
		info.getSubCommands().put("list", "Lists who has RSVPed to event");

		return info;
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
		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
				case "ontime":
					moduleGoing(args, event, settings);
					break;
				case "late":
					moduleGoingLate(args, event, settings);
					break;
				case "not":
					moduleNotGoing(args, event, settings);
					break;
				case "unsure":
					moduleUnsure(args, event, settings);
					break;
				case "remove":
					moduleRemove(args, event, settings);
					break;
				case "list":
					moduleList(args, event, settings);
					break;
				default:
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.InvalidSubCmd", settings), event);
					break;
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Few", settings), event);
		}
		return false;
	}

	private void moduleGoing(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (args.length == 2) {
			String eventId = args[1];
			if (EventUtils.eventExists(settings, eventId)) {
				if (!TimeUtils.inPast(eventId, settings)) {
					RsvpData data = DatabaseManager.getManager().getRsvpData(settings.getGuildID(), eventId);
					data.removeCompletely(event.getAuthor().getStringID());
					data.getGoingOnTime().add(event.getAuthor().getStringID());

					DatabaseManager.getManager().updateRsvpData(data);
					MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.going.success", settings), getRsvpEmbed(data, settings), event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.InPast", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.NotExist", settings), event);
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.going.specify", settings), event);
		}
	}

	private void moduleGoingLate(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (args.length == 2) {
			String eventId = args[1];
			if (EventUtils.eventExists(settings, eventId)) {
				if (!TimeUtils.inPast(eventId, settings)) {
					RsvpData data = DatabaseManager.getManager().getRsvpData(settings.getGuildID(), eventId);
					data.removeCompletely(event.getAuthor().getStringID());
					data.getGoingLate().add(event.getAuthor().getStringID());

					DatabaseManager.getManager().updateRsvpData(data);
					MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.late.success", settings), getRsvpEmbed(data, settings), event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.InPast", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.NotExist", settings), event);
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.late.specify", settings), event);
		}
	}

	private void moduleNotGoing(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (args.length == 2) {
			String eventId = args[1];
			if (EventUtils.eventExists(settings, eventId)) {
				if (!TimeUtils.inPast(eventId, settings)) {
					RsvpData data = DatabaseManager.getManager().getRsvpData(settings.getGuildID(), eventId);
					data.removeCompletely(event.getAuthor().getStringID());
					data.getNotGoing().add(event.getAuthor().getStringID());

					DatabaseManager.getManager().updateRsvpData(data);
					MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.not.success", settings), getRsvpEmbed(data, settings), event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.InPast", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.NotExist", settings), event);
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.not.specify", settings), event);
		}
	}

	private void moduleRemove(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (args.length == 2) {
			String eventId = args[1];
			if (EventUtils.eventExists(settings, eventId)) {
				if (!TimeUtils.inPast(eventId, settings)) {
					RsvpData data = DatabaseManager.getManager().getRsvpData(settings.getGuildID(), eventId);
					data.removeCompletely(event.getAuthor().getStringID());

					DatabaseManager.getManager().updateRsvpData(data);
					MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.remove.success", settings), getRsvpEmbed(data, settings), event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.InPast", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.NotExist", settings), event);
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.remove.specify", settings), event);
		}
	}

	private void moduleUnsure(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (args.length == 2) {
			String eventId = args[1];
			if (EventUtils.eventExists(settings, eventId)) {
				if (!TimeUtils.inPast(eventId, settings)) {
					RsvpData data = DatabaseManager.getManager().getRsvpData(settings.getGuildID(), eventId);
					data.removeCompletely(event.getAuthor().getStringID());
					data.getUndecided().add(event.getAuthor().getStringID());

					DatabaseManager.getManager().updateRsvpData(data);
					MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.unsure.success", settings), getRsvpEmbed(data, settings), event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.InPast", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.NotExist", settings), event);
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.unsure.specify", settings), event);
		}
	}

	private void moduleList(String[] args, MessageReceivedEvent event, GuildSettings settings) {
		if (args.length == 2) {
			String eventId = args[1];
			if (EventUtils.eventExists(settings, eventId)) {
				RsvpData data = DatabaseManager.getManager().getRsvpData(settings.getGuildID(), eventId);

				MessageManager.sendMessageAsync(getRsvpEmbed(data, settings), event);
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Notifications.Event.NoExist", settings), event);
			}
		} else {
			MessageManager.sendMessageAsync(MessageManager.getMessage("RSVP.list.specify", settings), event);
		}
	}


	private EmbedObject getRsvpEmbed(RsvpData data, GuildSettings settings) {
		EmbedBuilder em = new EmbedBuilder();
		em.withAuthorIcon(GlobalConst.iconUrl);
		em.withAuthorName("DisCal");
		em.withAuthorUrl(GlobalConst.discalSite);
		em.withTitle(MessageManager.getMessage("Embed.RSVP.List.Title", settings));
		em.appendField("Event ID", data.getEventId(), false);

		IGuild g = DisCalClient.getClient().getGuildByID(settings.getGuildID());

		StringBuilder onTime = new StringBuilder();
		for (IUser u : UserUtils.getUsers(data.getGoingOnTime(), g)) {
			onTime.append(u.getDisplayName(g)).append(", ");
		}

		StringBuilder late = new StringBuilder();
		for (IUser u : UserUtils.getUsers(data.getGoingLate(), g)) {
			late.append(u.getDisplayName(g)).append(", ");
		}

		StringBuilder unsure = new StringBuilder();
		for (IUser u : UserUtils.getUsers(data.getUndecided(), g)) {
			unsure.append(u.getDisplayName(g)).append(", ");
		}

		StringBuilder notGoing = new StringBuilder();
		for (IUser u : UserUtils.getUsers(data.getNotGoing(), g)) {
			notGoing.append(u.getDisplayName(g)).append(", ");
		}

		if (onTime.toString().isEmpty())
			em.appendField("On time", "N/a", true);
		else
			em.appendField("On Time", onTime.toString(), true);

		if (late.toString().isEmpty())
			em.appendField("Late", "N/a", true);
		else
			em.appendField("Late", late.toString(), true);

		if (unsure.toString().isEmpty())
			em.appendField("Unsure", "N/a", true);
		else
			em.appendField("Unsure", unsure.toString(), true);

		if (notGoing.toString().isEmpty())
			em.appendField("Not Going", "N/a", true);
		else
			em.appendField("Not Going", notGoing.toString(), true);

		em.withFooterText(MessageManager.getMessage("Embed.RSVP.List.Footer", settings));
		em.withColor(GlobalConst.discalColor);

		return em.build();
	}
}