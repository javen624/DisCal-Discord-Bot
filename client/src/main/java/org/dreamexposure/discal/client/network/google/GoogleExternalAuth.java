package org.dreamexposure.discal.client.network.google;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.dreamexposure.discal.client.message.CalendarMessageFormatter;
import org.dreamexposure.discal.client.message.MessageManager;
import org.dreamexposure.discal.core.calendar.CalendarAuth;
import org.dreamexposure.discal.core.crypto.AESEncryption;
import org.dreamexposure.discal.core.database.DatabaseManager;
import org.dreamexposure.discal.core.logger.Logger;
import org.dreamexposure.discal.core.network.google.Authorization;
import org.dreamexposure.discal.core.object.GuildSettings;
import org.dreamexposure.discal.core.object.network.google.Poll;
import org.dreamexposure.discal.core.utils.GlobalConst;
import org.json.JSONObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.io.IOException;
import java.util.List;

/**
 * @author NovaFox161
 * Date Created: 9/9/2018
 * For Project: DisCal-Discord-Bot
 * Author Website: https://www.novamaday.com
 * Company Website: https://www.dreamexposure.org
 * Contact: nova@dreamexposure.org
 */
@SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
public class GoogleExternalAuth {
	private static GoogleExternalAuth auth;

	private GoogleExternalAuth() {
	}

	public static GoogleExternalAuth getAuth() {
		if (auth == null)
			auth = new GoogleExternalAuth();

		return auth;
	}

	public void requestCode(MessageReceivedEvent event, GuildSettings settings) {
		try {
			RequestBody body = new FormBody.Builder()
					.addEncoded("client_id", Authorization.getAuth().getClientData().getClientId())
					.addEncoded("scope", CalendarScopes.CALENDAR)
					.build();

			Request httpRequest = new okhttp3.Request.Builder()
					.url("https://accounts.google.com/o/oauth2/device/code")
					.post(body)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();

			Response response = Authorization.getAuth().getClient().newCall(httpRequest).execute();

			if (response.code() == HttpStatusCodes.STATUS_CODE_OK) {
				JSONObject codeResponse = new JSONObject(response.body().string());

				//Send DM to user with code.
				EmbedBuilder em = new EmbedBuilder();
				em.withAuthorIcon(GlobalConst.iconUrl);
				em.withAuthorName("DisCal");
				em.withAuthorUrl(GlobalConst.discalSite);
				em.withTitle(MessageManager.getMessage("Embed.AddCalendar.Code.Title", settings));
				em.appendField(MessageManager.getMessage("Embed.AddCalendar.Code.Code", settings), codeResponse.getString("user_code"), true);
				em.withFooterText(MessageManager.getMessage("Embed.AddCalendar.Code.Footer", settings));

				em.withUrl(codeResponse.getString("verification_url"));
				em.withColor(GlobalConst.discalColor);

				IUser user = event.getAuthor();
				MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Code.Request.Success", settings), em.build(), user);

				//Start timer to poll Google Cal for auth
				Poll poll = new Poll(user, event.getGuild());

				poll.setDevice_code(codeResponse.getString("device_code"));
				poll.setRemainingSeconds(codeResponse.getInt("expires_in"));
				poll.setExpires_in(codeResponse.getInt("expires_in"));
				poll.setInterval(codeResponse.getInt("interval"));
				pollForAuth(poll);
			} else {
				MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Code.Request.Failure.NotOkay", settings), event.getAuthor());

				Logger.getLogger().debug(event.getAuthor(), "Error requesting access token.", "Status code: " + response.code() + " | " + response.message() + " | " + response.body().string(), this.getClass());
			}
		} catch (Exception e) {
			//Failed, report issue to dev.
			Logger.getLogger().exception(event.getAuthor(), "Failed to request Google Access Code", e, this.getClass());
			IUser u = event.getAuthor();
			MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Code.Request.Failure.Unknown", settings), u);
		}
	}

	void pollForAuth(Poll poll) {
		GuildSettings settings = DatabaseManager.getManager().getSettings(poll.getGuild().getLongID());
		try {
			RequestBody body = new FormBody.Builder()
					.addEncoded("client_id", Authorization.getAuth().getClientData().getClientId())
					.addEncoded("client_secret", Authorization.getAuth().getClientData().getClientSecret())
					.addEncoded("code", poll.getDevice_code())
					.addEncoded("grant_type", "http://oauth.net/grant_type/device/1.0")
					.build();

			Request httpRequest = new okhttp3.Request.Builder()
					.url("https://www.googleapis.com/oauth2/v4/token")
					.post(body)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();

			//Execute
			Response response = Authorization.getAuth().getClient().newCall(httpRequest).execute();


			//Handle response.
			if (response.code() == 403) {
				//Handle access denied
				MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Poll.Failure.Deny", settings), poll.getUser());
			} else if (response.code() == 400 || response.code() == 428) {
				try {
					//See if auth is pending, if so, just reschedule.
					JSONObject aprError = new JSONObject(response.body().string());

					if (aprError.getString("error").equalsIgnoreCase("authorization_pending")) {
						//Response pending
						PollManager.getManager().scheduleNextPoll(poll);
					} else if (aprError.getString("error").equalsIgnoreCase("expired_token")) {
						MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Poll.Failure.Expired", settings), poll.getUser());
					} else {
						MessageManager.sendDirectMessageAsync(MessageManager.getMessage("Notification.Error.Network", settings), poll.getUser());
						Logger.getLogger().debug(poll.getUser(), "Poll Failure!", "Status code: " + response.code() + " | " + response.message() + " | " + response.body().string(), this.getClass());
					}
				} catch (Exception e) {
					//Auth is not pending, error occurred.
					Logger.getLogger().exception(poll.getUser(), "Failed to poll for authorization to google account.", e, this.getClass());
					Logger.getLogger().debug(poll.getUser(), "More info on failure", "Status code: " + response.code() + " | " + response.message() + " | " + response.body().string(), this.getClass());
					MessageManager.sendDirectMessageAsync(MessageManager.getMessage("Notification.Error.Network", settings), poll.getUser());
				}
			} else if (response.code() == 429) {
				//We got rate limited... oops. Let's just poll half as often.
				poll.setInterval(poll.getInterval() * 2);
				PollManager.getManager().scheduleNextPoll(poll);
			} else if (response.code() == HttpStatusCodes.STATUS_CODE_OK) {
				//Access granted
				JSONObject aprGrant = new JSONObject(response.body().string());

				//Save credentials securely.
				GuildSettings gs = DatabaseManager.getManager().getSettings(poll.getGuild().getLongID());
				AESEncryption encryption = new AESEncryption(gs);
				gs.setEncryptedAccessToken(encryption.encrypt(aprGrant.getString("access_token")));
				gs.setEncryptedRefreshToken(encryption.encrypt(aprGrant.getString("refresh_token")));
				gs.setUseExternalCalendar(true);
				DatabaseManager.getManager().updateSettings(gs);

				try {
					Calendar service = CalendarAuth.getCalendarService(gs);
					List<CalendarListEntry> items = service.calendarList().list().setMinAccessRole("writer").execute().getItems();
					MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Poll.Success", settings), poll.getUser());
					for (CalendarListEntry i : items) {
						if (!i.isDeleted()) {
							EmbedBuilder em = new EmbedBuilder();
							em.withAuthorIcon(GlobalConst.iconUrl);
							em.withAuthorName("DisCal");
							em.withAuthorUrl(GlobalConst.discalSite);
							em.withTitle(MessageManager.getMessage("Embed.AddCalendar.List.Title", settings));
							em.appendField(MessageManager.getMessage("Embed.AddCalendar.List.Name", settings), i.getSummary(), false);
							em.appendField(MessageManager.getMessage("Embed.AddCalendar.List.TimeZone", settings), i.getTimeZone(), false);
							em.appendField(MessageManager.getMessage("Embed.AddCalendar.List.ID", settings), i.getId(), false);

							em.withUrl(CalendarMessageFormatter.getCalendarLink(settings.getGuildID()));
							em.withColor(GlobalConst.discalColor);
							MessageManager.sendDirectMessageAsync(em.build(), poll.getUser());
						}
					}
					//Response will be handled in guild, and will check. We already saved the tokens anyway.
				} catch (IOException e1) {
					//Failed to get calendars list and check for calendars.
					Logger.getLogger().exception(poll.getUser(), "Failed to list calendars from external account!", e1, this.getClass());

					MessageManager.sendDirectMessageAsync(MessageManager.getMessage("AddCalendar.Auth.Poll.Failure.ListCalendars", settings), poll.getUser());
				}
			} else {
				//Unknown network error...
				MessageManager.sendDirectMessageAsync(MessageManager.getMessage("Notification.Error.Network", settings), poll.getUser());
				Logger.getLogger().debug(poll.getUser(), "Network error; poll failure", "Status code: " + response.code() + " | " + response.message() + " | " + response.body().string(), this.getClass());
			}
		} catch (Exception e) {
			//Handle exception.
			Logger.getLogger().exception(poll.getUser(), "Failed to poll for authorization to google account", e, this.getClass());
			MessageManager.sendDirectMessageAsync(MessageManager.getMessage("Notification.Error.Unknown", settings), poll.getUser());
		}
	}
}