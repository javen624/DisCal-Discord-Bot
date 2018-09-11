package org.dreamexposure.discal.client.module.command;

import com.google.api.services.calendar.model.Calendar;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import org.dreamexposure.discal.client.message.CalendarMessageFormatter;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.calendar.CalendarAuth;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.logger.Logger;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.calendar.CalendarData;
import org.dreamexposure.discal.core.object.command.CommandInfo;
import org.dreamexposure.discal.core.utils.GlobalConst;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Created by Nova Fox on 6/16/17.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
public class TimeCommand implements ICommand {

	/**
	 * Gets the command this Object is responsible for.
	 *
	 * @return The command this Object is responsible for.
	 */
	@Override
	public String getCommand() {
		return "time";
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
		CommandInfo info = new CommandInfo("time");
		info.setDescription("Displays the current time for the calendar in its respective TimeZone.");
		info.setExample("!time");
		return info;
	}

	/**
	 * Issues the command this Object is responsible for.
	 *
	 * @param args     The command arguments.
	 * @param event    The event received.
	 * @param settings The guild settings.
	 * @return <code>true</code> if successful, else <code>false</code>.
	 */
	@Override
	public boolean issueCommand(String[] args, MessageCreateEvent event, GuildSettings settings) {
		calendarTime(event, settings);
		return false;
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	private void calendarTime(MessageCreateEvent event, GuildSettings settings) {
		try {
			//TODO: Handle multiple calendars...
			CalendarData data = DatabaseManager.getManager().getMainCalendar(settings.getGuildID());

			if (data.getCalendarAddress().equalsIgnoreCase("primary")) {
				//Does not have a calendar.
				MessageManager.sendMessageAsync(MessageManager.getMessage("Creator.Calendar.NoCalendar", settings), event);
			} else {
				Calendar cal = CalendarAuth.getCalendarService(settings).calendars().get(data.getCalendarAddress()).execute();

				LocalDateTime ldt = LocalDateTime.now(ZoneId.of(cal.getTimeZone()));

				//Okay... format and then we can go from there...
				DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a");
				String thisIsTheCorrectTime = format.format(ldt);

				//Build embed and send.
				EmbedCreateSpec em = new EmbedCreateSpec();
				em.setAuthor("DisCal", GlobalConst.discalSite, GlobalConst.iconUrl);
				em.setTitle(MessageManager.getMessage("Embed.Time.Title", settings));
				em.addField(MessageManager.getMessage("Embed.Time.Time", settings), thisIsTheCorrectTime, false);
				em.addField(MessageManager.getMessage("Embed.Time.TimeZone", settings), cal.getTimeZone(), false);

				em.setFooter(MessageManager.getMessage("Embed.Time.Footer", settings), null);
				em.setUrl(CalendarMessageFormatter.getCalendarLink(settings.getGuildID()));
				em.setColor(GlobalConst.discalColor);
				MessageManager.sendMessageAsync(em, event);
			}
		} catch (Exception e) {
			Logger.getLogger().exception(event.getMember().get(), "Failed to connect to Google Cal.", e, this.getClass());
			MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Error.Unknown", settings), event);
		}
	}
}