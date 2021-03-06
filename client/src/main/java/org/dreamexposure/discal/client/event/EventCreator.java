package org.dreamexposure.discal.client.event;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import org.dreamexposure.discal.client.message.EventMessageFormatter;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.calendar.CalendarAuth;
import org.dreamexposure.discal.core.crypto.KeyGenerator;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.enums.event.EventColor;
import org.dreamexposure.discal.core.logger.Logger;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.event.EventCreatorResponse;
import org.dreamexposure.discal.core.object.event.PreEvent;
import org.dreamexposure.discal.core.utils.EventUtils;
import org.dreamexposure.discal.core.utils.PermissionChecker;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Nova Fox on 1/3/2017.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
@SuppressWarnings({"Duplicates", "ConstantConditions", "OptionalGetWithoutIsPresent"})
public class EventCreator {
	private static EventCreator instance;

	private ArrayList<PreEvent> events = new ArrayList<>();

	private EventCreator() {
	} //Prevent initialization.

	/**
	 * Gets the instance of the EventCreator.
	 *
	 * @return The instance of the EventCreator
	 */
	public static EventCreator getCreator() {
		if (instance == null)
			instance = new EventCreator();
		return instance;
	}

	//Functionals

	/**
	 * Initiates the EventCreator for a specific guild.
	 *
	 * @param e The event received upon initialization.
	 * @return The PreEvent for the guild.
	 */
	public PreEvent init(MessageReceivedEvent e, GuildSettings settings, boolean handleMessage) {
		if (!hasPreEvent(settings.getGuildID())) {
			PreEvent event = new PreEvent(settings.getGuildID());
			try {
				//TODO: Handle multiple calendars...
				String calId = DatabaseManager.getManager().getMainCalendar(settings.getGuildID()).getCalendarAddress();
				event.setTimeZone(CalendarAuth.getCalendarService(settings).calendars().get(calId).execute().getTimeZone());
			} catch (Exception exc) {
				//Failed to get timezone, ignore safely.
			}
			if (handleMessage) {
				if (PermissionChecker.botHasMessageManagePerms(e)) {
					IMessage message = MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Event.Create.Init", settings), EventMessageFormatter.getPreEventEmbed(event, settings), e);
					event.setCreatorMessage(message);
					MessageManager.deleteMessage(e);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Notif.MANAGE_MESSAGES", settings), e);
				}
			}

			events.add(event);
			return event;
		}
		return getPreEvent(settings.getGuildID());
	}

	public PreEvent init(MessageReceivedEvent e, GuildSettings settings, String summary, boolean handleMessage) {
		if (!hasPreEvent(settings.getGuildID())) {
			PreEvent event = new PreEvent(settings.getGuildID());
			event.setSummary(summary);
			try {
				//TODO: Handle multiple calendars...
				String calId = DatabaseManager.getManager().getMainCalendar(settings.getGuildID()).getCalendarAddress();

				event.setTimeZone(CalendarAuth.getCalendarService(settings).calendars().get(calId).execute().getTimeZone());
			} catch (Exception exc) {
				//Failed to get timezone, ignore safely.
			}
			if (handleMessage) {
				if (PermissionChecker.botHasMessageManagePerms(e)) {
					IMessage message = MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Event.Create.Init", settings), EventMessageFormatter.getPreEventEmbed(event, settings), e);
					event.setCreatorMessage(message);
					MessageManager.deleteMessage(e);
				} else {
					MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Notif.MANAGE_MESSAGES", settings), e);
				}
			}

			events.add(event);
			return event;
		}
		return getPreEvent(settings.getGuildID());
	}

	//Copy event
	public PreEvent init(MessageReceivedEvent e, String eventId, GuildSettings settings, boolean handleMessage) {
		if (!hasPreEvent(settings.getGuildID())) {
			//TODO: Handle multiple calendars...
			try {
				String calId = DatabaseManager.getManager().getMainCalendar(settings.getGuildID()).getCalendarAddress();
				Calendar service = CalendarAuth.getCalendarService(settings);

				Event calEvent = service.events().get(calId, eventId).execute();

				PreEvent event = EventUtils.copyEvent(settings.getGuildID(), calEvent);

				try {
					event.setTimeZone(service.calendars().get(calId).execute().getTimeZone());
				} catch (IOException e1) {
					//Failed to get tz, ignore safely.
				}

				if (handleMessage) {
					if (PermissionChecker.botHasMessageManagePerms(e)) {
						IMessage message = MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Event.Copy.Init", settings), EventMessageFormatter.getPreEventEmbed(event, settings), e);
						event.setCreatorMessage(message);
						MessageManager.deleteMessage(e);
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Notif.MANAGE_MESSAGES", settings), e);
					}
				}

				events.add(event);
				return event;
			} catch (Exception exc) {
				//Something failed...
			}
			return null;
		}
		return getPreEvent(settings.getGuildID());
	}

	public PreEvent edit(MessageReceivedEvent e, String eventId, GuildSettings settings, boolean handleMessage) {
		if (!hasPreEvent(settings.getGuildID())) {
			//TODO: Handle multiple calendars...
			try {
				String calId = DatabaseManager.getManager().getMainCalendar(settings.getGuildID()).getCalendarAddress();
				Calendar service = CalendarAuth.getCalendarService(settings);

				Event calEvent = service.events().get(calId, eventId).execute();

				PreEvent event = new PreEvent(settings.getGuildID(), calEvent);
				event.setEditing(true);

				try {
					event.setTimeZone(service.calendars().get(calId).execute().getTimeZone());
				} catch (IOException ignore) {
					//Failed to get tz, ignore safely.
				}

				if (handleMessage) {
					if (PermissionChecker.botHasMessageManagePerms(e)) {
						IMessage message = MessageManager.sendMessageSync(MessageManager.getMessage("Creator.Event.Edit.Init", settings), EventMessageFormatter.getPreEventEmbed(event, settings), e);
						event.setCreatorMessage(message);
						MessageManager.deleteMessage(e);
					} else {
						MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Notif.MANAGE_MESSAGES", settings), e);
					}
				}

				events.add(event);
				return event;
			} catch (Exception exc) {
				//Oops
			}
			return null;
		}
		return getPreEvent(settings.getGuildID());
	}

	public boolean terminate(long guildId) {
		if (hasPreEvent(guildId)) {
			events.remove(getPreEvent(guildId));
			return true;
		}
		return false;
	}

	/**
	 * Confirms the event in the creator for the specific guild.
	 *
	 * @param e The event received upon confirmation.
	 * @return The response containing detailed info about the confirmation.
	 */
	public EventCreatorResponse confirmEvent(MessageReceivedEvent e, GuildSettings settings) {
		if (hasPreEvent(settings.getGuildID())) {
			PreEvent preEvent = getPreEvent(settings.getGuildID());
			if (preEvent.hasRequiredValues()) {
				Event event = new Event();
				event.setSummary(preEvent.getSummary());
				event.setDescription(preEvent.getDescription());
				event.setStart(preEvent.getStartDateTime().setTimeZone(preEvent.getTimeZone()));
				event.setEnd(preEvent.getEndDateTime().setTimeZone(preEvent.getTimeZone()));
				event.setVisibility("public");
				if (!preEvent.getColor().equals(EventColor.NONE))
					event.setColorId(String.valueOf(preEvent.getColor().getId()));

				if (preEvent.getLocation() != null && !preEvent.getLocation().equalsIgnoreCase(""))
					event.setLocation(preEvent.getLocation());


				//Set recurrence
				if (preEvent.shouldRecur()) {
					String[] recurrence = new String[]{preEvent.getRecurrence().toRRule()};
					event.setRecurrence(Arrays.asList(recurrence));
				}

				//TODO handle multiple calendars...
				String calendarId = DatabaseManager.getManager().getMainCalendar(settings.getGuildID()).getCalendarAddress();

				if (!preEvent.isEditing()) {
					event.setId(KeyGenerator.generateEventId());
					try {
						Event confirmed = CalendarAuth.getCalendarService(settings).events().insert(calendarId, event).execute();

						if (preEvent.getEventData().shouldBeSaved()) {
							preEvent.getEventData().setEventId(confirmed.getId());
							preEvent.getEventData().setEventEnd(confirmed.getEnd().getDateTime().getValue());
							DatabaseManager.getManager().updateEventData(preEvent.getEventData());
						}
						terminate(settings.getGuildID());
						EventCreatorResponse response = new EventCreatorResponse(true, confirmed);
						response.setEdited(false);
						return response;
					} catch (Exception ex) {
						Logger.getLogger().exception(e.getAuthor(), "Failed to create event.", ex, this.getClass());
						EventCreatorResponse response = new EventCreatorResponse(false);
						response.setEdited(false);
						return response;
					}
				} else {
					try {
						Event confirmed = CalendarAuth.getCalendarService(settings).events().update(calendarId, preEvent.getEventId(), event).execute();

						if (preEvent.getEventData().shouldBeSaved()) {
							preEvent.getEventData().setEventId(confirmed.getId());
							preEvent.getEventData().setEventEnd(confirmed.getEnd().getDateTime().getValue());
							DatabaseManager.getManager().updateEventData(preEvent.getEventData());
						}
						terminate(settings.getGuildID());

						EventCreatorResponse response = new EventCreatorResponse(true, confirmed);
						response.setEdited(true);
						return response;
					} catch (Exception ex) {
						Logger.getLogger().exception(e.getAuthor(), "Failed to update event.", ex, this.getClass());
						EventCreatorResponse response = new EventCreatorResponse(false);
						response.setEdited(true);
						return response;
					}
				}
			}
		}
		return new EventCreatorResponse(false);
	}

	//Getters

	/**
	 * gets the PreEvent for the specified guild.
	 *
	 * @param guildId The ID of the guild.
	 * @return The PreEvent belonging to the guild.
	 */
	public PreEvent getPreEvent(long guildId) {
		for (PreEvent e: events) {
			if (e.getGuildId() == guildId) {
				e.setLastEdit(System.currentTimeMillis());
				return e;
			}
		}
		return null;
	}

	public IMessage getCreatorMessage(long guildId) {
		if (hasPreEvent(guildId))
			return getPreEvent(guildId).getCreatorMessage();
		return null;
	}

	public ArrayList<PreEvent> getAllPreEvents() {
		return events;
	}

	//Booleans/Checkers

	/**
	 * Checks if the specified guild has a PreEvent in the creator.
	 *
	 * @param guildId The ID of the guild.
	 * @return <code>true</code> if a PreEvent exists, otherwise <code>false</code>.
	 */
	public boolean hasPreEvent(long guildId) {
		for (PreEvent e: events) {
			if (e.getGuildId() == guildId)
				return true;
		}
		return false;
	}

	public boolean hasCreatorMessage(long guildId) {
		return hasPreEvent(guildId) && getPreEvent(guildId).getCreatorMessage() != null;
	}

	//Setters
	public void setCreatorMessage(IMessage msg) {
		if (msg != null && hasPreEvent(msg.getGuild().getLongID()))
			getPreEvent(msg.getGuild().getLongID()).setCreatorMessage(msg);
	}
}