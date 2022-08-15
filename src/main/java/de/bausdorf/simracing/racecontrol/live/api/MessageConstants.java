package de.bausdorf.simracing.racecontrol.live.api;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

public class MessageConstants {


	public static class MessageType {

		public static final String PING_NAME = "ping";
		public static final String EVENTDATA_NAME = "event";
		public static final String SESSIONDATA_NAME = "sessionInfo";

		private MessageType() {}
	}

	public static class Message {
		public static final String MESSAGE_TYPE = "type";
		public static final String VERSION = "version";
		public static final String SESSION_ID = "sessionId";
		public static final String TEAM_ID = "teamId";
		public static final String CLIENT_ID = "clientId";
		public static final String PAYLOAD = "payload";
		public static final String SESSION_LAP = "lap";

		private Message() {}
	}

	public static class EventData {
		public static final String SESSION_TIME = "SessionTime";
		public static final String TRACK_LOCATION = "trackLocation";
		public static final String DRIVER_NAME = "CurrentDriver";
		public static final String I_RATING = "IRating";
		public static final String TEAM_NAME = "TeamName";
		public static final String CAR_NUMBER = "CarNumber";
		public static final String LAP = "CarLap";
		public static final String EVENT_TYPE = "Type";
		public static final String EVENT_NO = "IncNo";
		public static final String CAR_NAME = "CarName";
		public static final String CAR_CLASS = "CarClass";
		public static final String CAR_CLASS_ID = "CarClassId";
		public static final String CAR_CLASS_COLOR = "CarClassColor";
		public static final String LAP_PCT = "LapPct";

		private EventData() {}
	}

	public static class SessionData {
		public static final String TRACK_NAME = "TrackName";
		public static final String SESSION_DURATION = "SessionDuration";
		public static final String SESSION_TYPE = "SessionType";
		public static final String SESSION_STATE = "SessionState";
		public static final String SESSION_TIME = "SessionTime";

		private SessionData() {}
	}
	private MessageConstants() {
		super();
	}
}
