package org.dreamexposure.discal.client.module.command;

import org.dreamexposure.discal.client.calendar.CalendarCreator;
import org.dreamexposure.discal.client.message.CalendarMessageFormatter;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.calendar.CalendarCreatorResponse;
import org.dreamexposure.discal.core.object.calendar.CalendarData;
import org.dreamexposure.discal.core.object.calendar.PreCalendar;
import org.dreamexposure.discal.core.object.command.CommandInfo;
import org.dreamexposure.discal.core.utils.CalendarUtils;
import org.dreamexposure.discal.core.utils.GeneralUtils;
import org.dreamexposure.discal.core.utils.PermissionChecker;
import org.dreamexposure.discal.core.utils.TimeZoneUtils;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;

import java.util.ArrayList;

/**
 * Created by Nova Fox on 1/4/2017.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
@SuppressWarnings({"FieldCanBeLocal", "Duplicates"})
public class CalendarCommand implements ICommand {
	private String TIME_ZONE_DB = "http://www.joda.org/joda-time/timezones.html";

	/**
	 * Gets the command this Object is responsible for.
	 *
	 * @return The command this Object is responsible for.
	 */
	@Override
	public String getCommand() {
		return "calendar";
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
		ArrayList<String> aliases = new ArrayList<>();
		aliases.add("cal");
		aliases.add("callador");
		return aliases;
	}

	/**
	 * Gets the info on the command (not sub command) to be used in help menus.
	 *
	 * @return The command info.
	 */
	@Override
	public CommandInfo getCommandInfo() {
		CommandInfo info = new CommandInfo("calendar");
		info.setDescription("Used for direct interaction with your DisCal Calendar.");
		info.setExample("!calendar <subCommand> (value)");

		info.getSubCommands().put("create", "Starts the creation of a new calendar.");
		info.getSubCommands().put("cancel", "Cancels the creator/editor");
		info.getSubCommands().put("view", "Views the calendar in the creator/editor");
		info.getSubCommands().put("review", "Views the calendar in the creator/editor");
		info.getSubCommands().put("confirm", "Confirms and creates/edits the calendar.");
		info.getSubCommands().put("delete", "Deletes the calendar");
		info.getSubCommands().put("remove", "Deletes the calendar");
		info.getSubCommands().put("name", "Sets the calendar's name/summary");
		info.getSubCommands().put("summary", "Sets the calendar's name/summary");
		info.getSubCommands().put("description", "Sets the calendar's description");
		info.getSubCommands().put("timezone", "Sets teh calendar's timezone.");
		info.getSubCommands().put("edit", "Edits the calendar.");

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
		if (args.length < 1) {
			MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Few", settings), event);
		} else {
			long guildId = event.getGuild().getLongID();
			//TODO: Add support for multiple calendars...
			CalendarData calendarData = DatabaseManager.getManager().getMainCalendar(guildId);

			switch (args[0].toLowerCase()) {
				case "create":
					if (PermissionChecker.hasSufficientRole(event))
						moduleCreate(args, event, calendarData, settings);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
					break;
				case "cancel":
					if (PermissionChecker.hasSufficientRole(event))
						moduleCancel(event, calendarData, settings);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
					break;
				case "view":
					moduleView(event, calendarData, settings);
					break;
				case "review":
					moduleView(event, calendarData, settings);
					break;
				case "confirm":
					if (PermissionChecker.hasSufficientRole(event))
						moduleConfirm(event, calendarData, settings);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
					break;
				case "delete":
					if (PermissionChecker.hasSufficientRole(event))
						moduleDelete(event, calendarData, settings);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
					break;
				case "remove":
					if (PermissionChecker.hasSufficientRole(event))
						moduleDelete(event, calendarData, settings);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
					break;
				case "name":
					moduleSummary(args, event, calendarData, settings);
					break;
				case "summary":
					moduleSummary(args, event, calendarData, settings);
					break;
				case "description":
					moduleDescription(args, event, calendarData, settings);
					break;
				case "timezone":
					moduleTimezone(args, event, calendarData, settings);
					break;
				case "edit":
					if (PermissionChecker.hasSufficientRole(event))
						moduleEdit(event, calendarData, settings);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
					break;
				default:
					MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Invalid", settings), event);
					break;
			}
		}
		return false;
	}

	private void moduleCreate(String[] args, MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
			if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
				MessageManager.deleteMessage(event);
				MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
				CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.AlreadyInit", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.AlreadyInit", settings), event);
			}
		} else {
			if (calendarData.getCalendarId().equalsIgnoreCase("primary")) {
				if (args.length > 1) {
					String name = GeneralUtils.getContent(args, 1);
					PreCalendar calendar = CalendarCreator.getCreator().init(event, name, settings, true);
					if (calendar.getCreatorMessage() != null)
						MessageManager.deleteMessage(event);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Create.Init", settings), event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Create.Name", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
			}
		}
	}

	private void moduleCancel(MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
			IMessage message = CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage();
			boolean editing = CalendarCreator.getCreator().getPreCalendar(guildId).isEditing();
			if (CalendarCreator.getCreator().terminate(guildId)) {
				if (message != null) {
					if (!editing) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
					} else {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Cancel.Edit.Success", settings), event));
					}
				} else {
					if (!editing)
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Cancel.Success", settings), event);
					else
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Cancel.Edit.Success", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Cancel.Failure", settings), event);
				MessageManager.deleteMessage(event);
			}
		} else {
			if (calendarData.getCalendarId().equalsIgnoreCase("primary"))
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NotInit", settings), event);
			else
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
		}
	}

	private void moduleView(MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
			PreCalendar preCalendar = CalendarCreator.getCreator().getPreCalendar(guildId);
			if (preCalendar.getCreatorMessage() != null) {
				MessageManager.deleteMessage(event);
				MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
				CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Review", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Review", settings), CalendarMessageFormatter.getPreCalendarEmbed(preCalendar, settings), event);
			}
		} else {
			if (calendarData.getCalendarId().equalsIgnoreCase("primary"))
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
			else
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
		}
	}

	private void moduleConfirm(MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
			CalendarCreatorResponse response = CalendarCreator.getCreator().confirmCalendar(event, settings);
			if (response.isSuccessful()) {
				if (response.isEdited()) {
					if (response.getCreatorMessage() != null) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Confirm.Edit.Success", settings), CalendarMessageFormatter.getCalendarLinkEmbed(response.getCalendar(), settings), event));
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Confirm.Edit.Success", settings), CalendarMessageFormatter.getCalendarLinkEmbed(response.getCalendar(), settings), event);
					}
				} else {
					if (response.getCalendar() != null) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Confirm.Create.Success", settings), CalendarMessageFormatter.getCalendarLinkEmbed(response.getCalendar(), settings), event));
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Confirm.Create.Success", settings), CalendarMessageFormatter.getCalendarLinkEmbed(response.getCalendar(), settings), event);
					}
				}
			} else {
				if (response.isEdited()) {
					if (response.getCreatorMessage() != null) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Confirm.Edit.Failure", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Confirm.Edit.Failure", settings), event);
					}
				} else {
					if (response.getCreatorMessage() != null) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Confirm.Create.Failure", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Confirm.Create.Failure", settings), event);
					}
				}
			}
		} else {
			if (calendarData.getCalendarId().equalsIgnoreCase("primary"))
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
			else
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
		}
	}

	private void moduleDelete(MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getMessage().getGuild().getLongID();
		if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
			if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
				MessageManager.deleteMessage(event);
				MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
				CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Delete.Failure.InCreator", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Delete.Failure.InCreator", settings), event);
			}
			return;
		}
		if (!event.getMessage().getAuthor().getPermissionsForGuild(event.getMessage().getGuild()).contains(Permissions.MANAGE_SERVER)) {
			MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.MANAGE_SERVER", settings), event);
			return;
		}
		if (!calendarData.getCalendarId().equalsIgnoreCase("primary")) {
			//Delete calendar
			if (CalendarUtils.deleteCalendar(calendarData, settings))
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Delete.Success", settings), event);
			else
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Delete.Failure.Unknown", settings), event);
		} else {
			//No calendar to delete
			MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Delete.Failure.NoCalendar", settings), event);
		}
	}

	private void moduleSummary(String[] args, MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (args.length > 1) {
			if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
				if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
					CalendarCreator.getCreator().getPreCalendar(guildId).setSummary(GeneralUtils.getContent(args, 1));
					MessageManager.deleteMessage(event);
					MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
					CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Summary.N.Success", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
				} else {
					String msg = MessageManager.getMessage("Creator.Calendar.Summary.O.Success", "%summary%", GeneralUtils.getContent(args, 1), settings);
					MessageManager.sendMessageAsync(msg, event);
				}
			} else {
				if (calendarData.getCalendarId().equalsIgnoreCase("primary"))
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
				else
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
			}
		} else {
			if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
				if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
					MessageManager.deleteMessage(event);
					MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
					CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Summary.Specify", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Summary.Specify", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Summary.Specify", settings), event);
			}
		}
	}

	private void moduleDescription(String[] args, MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (args.length > 1) {
			if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
				CalendarCreator.getCreator().getPreCalendar(guildId).setDescription(GeneralUtils.getContent(args, 1));
				if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
					MessageManager.deleteMessage(event);
					MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
					CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Description.N.Success", settings) + TIME_ZONE_DB, CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Description.O.Success", "%desc%", GeneralUtils.getContent(args, 1), settings) + TIME_ZONE_DB, event);
				}
			} else {
				if (calendarData.getCalendarId().equalsIgnoreCase("primary"))
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
				else
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
			}
		} else {
			if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
				if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
					MessageManager.deleteMessage(event);
					MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
					CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Description.Specify", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Description.Specify", settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Description.Specify", settings), event);
			}
		}
	}

	private void moduleTimezone(String[] args, MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (args.length == 2) {
			String value = args[1];
			if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
				if (TimeZoneUtils.isValid(value)) {
					CalendarCreator.getCreator().getPreCalendar(guildId).setTimezone(value);

					if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.TimeZone.N.Success", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.TimeZone.O.Success", "%tz%", value, settings), event);
					}
				} else {
					if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
						MessageManager.deleteMessage(event);
						MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
						CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.TimeZone.Invalid", "%tz_db%", TIME_ZONE_DB, settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.TimeZone.Invalid", "%tz_db%", TIME_ZONE_DB, settings), event);
					}
				}
			} else {
				if (calendarData.getCalendarId().equalsIgnoreCase("primary"))
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
				else
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.HasCalendar", settings), event);
			}
		} else {
			if (CalendarCreator.getCreator().hasPreCalendar(guildId)) {
				MessageManager.deleteMessage(event);
				MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
				CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.TimeZone.Specify", settings) + TIME_ZONE_DB, CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.TimeZone.Specify", settings) + TIME_ZONE_DB, event);
			}
		}
	}

	private void moduleEdit(MessageReceivedEvent event, CalendarData calendarData, GuildSettings settings) {
		long guildId = event.getGuild().getLongID();
		if (!CalendarCreator.getCreator().hasPreCalendar(guildId)) {
			if (!calendarData.getCalendarAddress().equalsIgnoreCase("primary")) {
				PreCalendar calendar = CalendarCreator.getCreator().edit(event, settings, true);
				if (calendar.getCreatorMessage() != null) {
					MessageManager.deleteMessage(event);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.Edit.Init", settings), CalendarMessageFormatter.getPreCalendarEmbed(calendar, settings), event);
				}
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
			}
		} else {
			if (CalendarCreator.getCreator().getPreCalendar(guildId).getCreatorMessage() != null) {
				MessageManager.deleteMessage(event);
				MessageManager.deleteMessage(CalendarCreator.getCreator().getCreatorMessage(guildId));
				CalendarCreator.getCreator().setCreatorMessage(MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.AlreadyInit", settings), CalendarMessageFormatter.getPreCalendarEmbed(CalendarCreator.getCreator().getPreCalendar(guildId), settings), event));
			} else {
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.AlreadyInit", settings), event);
			}
		}
	}
}