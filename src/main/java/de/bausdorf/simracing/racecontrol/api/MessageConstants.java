package de.bausdorf.simracing.racecontrol.api;

public class MessageConstants {


	public static class MessageType {

		public static final String PING_NAME = "ping";
		public static final String EVENTDATA_NAME = "event";

		private MessageType() {}
	}

	public static class Message {
		public static final String MESSAGE_TYPE = "MesssageType";
		public static final String VERSION = "Version";
		public static final String SESSION_ID = "SessionId";
		public static final String TEAM_ID = "TeamID";
		public static final String CLIENT_ID = "DriverId";
		public static final String SESSION_TIME = "SessionTime";

		private Message() {}
	}

	public static class EventData {
		public static final String TRACK_LOCATION = "trackLocation";
		public static final String DRIVER_NAME = "CurrentDriver";
		public static final String I_RATING = "IRating";
		public static final String TEAM_NAME = "TeamName";
		public static final String CAR_NUMBER = "CarNumber";
		public static final String LAP = "Lap";
		public static final String EVENT_TYPE = "Type";
		public static final String EVENT_NO = "IncNo";

		private EventData() {}
	}

	private MessageConstants() {
		super();
	}
}
