package org.dreamexposure.discal.client.calendar;

import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.calendar.model.Calendar;
import org.dreamexposure.discal.client.message.CalendarMessageFormatter;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.calendar.CalendarAuth;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.logger.Logger;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.calendar.CalendarCreatorResponse;
import org.dreamexposure.discal.core.object.calendar.CalendarData;
import org.dreamexposure.discal.core.object.calendar.PreCalendar;
import org.dreamexposure.discal.core.utils.PermissionChecker;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;

/**
 * Created by Nova Fox on 1/4/2017.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
@SuppressWarnings({"ConstantConditions", "Duplicates"})
public class CalendarCreator {
	private static CalendarCreator instance;

	private ArrayList<PreCalendar> calendars = new ArrayList<>();

	private CalendarCreator() {
	} //Prevent initialization

	/**
	 * Gets the instance of the CalendarCreator.
	 *
	 * @return The instance of the CalendarCreator.
	 */
	public static CalendarCreator getCreator() {
		if (instance == null)
			instance = new CalendarCreator();
		return instance;
	}

	//Functionals

	/**
	 * Initiates the CalendarCreator for the guild involved in the event.
	 *
	 * @param e            The event received upon creation start.
	 * @param calendarName The name of the calendar to create.
	 * @return The PreCalendar object created.
	 */
	public PreCalendar init(MessageReceivedEvent e, String calendarName, GuildSettings settings, boolean handleCreatorMessage) {
		if (!hasPreCalendar(settings.getGuildID())) {
			PreCalendar calendar = new PreCalendar(settings.getGuildID(), calendarName);

			if (handleCreatorMessage) {
				if (PermissionChecker.botHasMessageManagePerms(e)) {
					IMessage msg = MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Create.Init", settings), CalendarMessageFormatter.getPreCalendarEmbed(calendar, settings), e);
					calendar.setCreatorMessage(msg);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Notif.MANAGE_MESSAGES", settings), e);
				}
			}
			calendars.add(calendar);
			return calendar;
		}
		return getPreCalendar(settings.getGuildID());
	}

	@SuppressWarnings("SameParameterValue")
	public PreCalendar edit(MessageReceivedEvent event, GuildSettings settings, boolean handleCreatorMessage) {
		if (!hasPreCalendar(settings.getGuildID())) {
			//TODO: Support multiple calendars
			CalendarData data = DatabaseManager.getManager().getMainCalendar(settings.getGuildID());

			try {
				com.google.api.services.calendar.Calendar service = CalendarAuth.getCalendarService(settings);

				Calendar calendar = service.calendars().get(data.getCalendarAddress()).execute();

				PreCalendar preCalendar = new PreCalendar(settings.getGuildID(), calendar);
				preCalendar.setEditing(true);
				preCalendar.setCalendarId(data.getCalendarAddress());

				if (handleCreatorMessage) {
					if (PermissionChecker.botHasMessageManagePerms(event)) {
						IMessage msg = MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Calendar.Edit.Init", settings), CalendarMessageFormatter.getPreCalendarEmbed(preCalendar, settings), event);
						preCalendar.setCreatorMessage(msg);
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Notif.MANAGE_MESSAGES", settings), event);
					}
				}

				calendars.add(preCalendar);
				return preCalendar;
			} catch (Exception e) {
				Logger.getLogger().exception(event.getAuthor(), "Failed to init calendar editor", e, this.getClass());
				return null;
			}
		} else {
			return getPreCalendar(settings.getGuildID());
		}
	}

	public boolean terminate(long guildId) {
		if (hasPreCalendar(guildId)) {
			calendars.remove(getPreCalendar(guildId));
			return true;
		}
		return false;
	}

	/**
	 * Confirms the calendar and creates it within Google Calendar.
	 *
	 * @param e The event received upon confirmation.
	 * @return A CalendarCreatorResponse Object with detailed info about the confirmation.
	 */
	public CalendarCreatorResponse confirmCalendar(MessageReceivedEvent e, GuildSettings settings) {
		if (hasPreCalendar(settings.getGuildID())) {
			PreCalendar preCalendar = getPreCalendar(settings.getGuildID());
			if (preCalendar.hasRequiredValues()) {
				if (!preCalendar.isEditing()) {
					Calendar calendar = new Calendar();
					calendar.setSummary(preCalendar.getSummary());
					calendar.setDescription(preCalendar.getDescription());
					calendar.setTimeZone(preCalendar.getTimezone());
					try {
						com.google.api.services.calendar.Calendar service = CalendarAuth.getCalendarService(settings);

						Calendar confirmed = service.calendars().insert(calendar).execute();
						AclRule rule = new AclRule();
						AclRule.Scope scope = new AclRule.Scope();
						scope.setType("default");
						rule.setScope(scope).setRole("reader");
						service.acl().insert(confirmed.getId(), rule).execute();
						CalendarData calendarData = new CalendarData(settings.getGuildID(), 1);
						calendarData.setCalendarId(confirmed.getId());
						calendarData.setCalendarAddress(confirmed.getId());
						DatabaseManager.getManager().updateCalendar(calendarData);
						terminate(settings.getGuildID());
						CalendarCreatorResponse response = new CalendarCreatorResponse(true, confirmed);
						response.setEdited(false);
						response.setCreatorMessage(preCalendar.getCreatorMessage());
						return response;
					} catch (Exception ex) {
						Logger.getLogger().exception(e.getAuthor(), "Failed to confirm calendar.", ex, this.getClass());
						CalendarCreatorResponse response = new CalendarCreatorResponse(false);
						response.setEdited(false);
						response.setCreatorMessage(preCalendar.getCreatorMessage());
						return response;
					}
				} else {
					//Editing calendar...
					Calendar calendar = new Calendar();
					calendar.setSummary(preCalendar.getSummary());
					calendar.setDescription(preCalendar.getDescription());
					calendar.setTimeZone(preCalendar.getTimezone());

					try {
						com.google.api.services.calendar.Calendar service = CalendarAuth.getCalendarService(settings);

						Calendar confirmed = service.calendars().update(preCalendar.getCalendarId(), calendar).execute();
						AclRule rule = new AclRule();
						AclRule.Scope scope = new AclRule.Scope();
						scope.setType("default");
						rule.setScope(scope).setRole("reader");
						service.acl().insert(confirmed.getId(), rule).execute();
						terminate(settings.getGuildID());
						CalendarCreatorResponse response = new CalendarCreatorResponse(true, confirmed);
						response.setEdited(true);
						response.setCreatorMessage(preCalendar.getCreatorMessage());
						return response;
					} catch (Exception ex) {
						Logger.getLogger().exception(e.getAuthor(), "Failed to update calendar.", ex, this.getClass());
						CalendarCreatorResponse response = new CalendarCreatorResponse(false);
						response.setEdited(true);
						response.setCreatorMessage(preCalendar.getCreatorMessage());
						return response;
					}
				}
			}
		}
		return new CalendarCreatorResponse(false);
	}

	//Getters

	/**
	 * Gets the PreCalendar for the guild in the creator.
	 *
	 * @param guildId The ID of the guild whose PreCalendar is to be returned.
	 * @return The PreCalendar belonging to the guild.
	 */
	public PreCalendar getPreCalendar(long guildId) {
		for (PreCalendar c: calendars) {
			if (c.getGuildId() == guildId) {
				c.setLastEdit(System.currentTimeMillis());
				return c;
			}
		}
		return null;
	}

	public IMessage getCreatorMessage(long guildId) {
		if (hasPreCalendar(guildId))
			return getPreCalendar(guildId).getCreatorMessage();
		return null;
	}

	public ArrayList<PreCalendar> getAllPreCalendars() {
		return calendars;
	}

	//Booleans/Checkers

	/**
	 * Checks whether or not the specified Guild has a PreCalendar in the creator.
	 *
	 * @param guildId The ID of the guild to check for.
	 * @return <code>true</code> if a PreCalendar exists, else <code>false</code>.
	 */
	public Boolean hasPreCalendar(long guildId) {
		for (PreCalendar c: calendars) {
			if (c.getGuildId() == guildId)
				return true;
		}
		return false;
	}

	public boolean hasCreatorMessage(long guildId) {
		return hasPreCalendar(guildId) && getPreCalendar(guildId).getCreatorMessage() != null;
	}

	//Setters
	public void setCreatorMessage(IMessage msg) {
		if (msg != null && hasPreCalendar(msg.getGuild().getLongID()))
			getPreCalendar(msg.getGuild().getLongID()).setCreatorMessage(msg);
	}
}