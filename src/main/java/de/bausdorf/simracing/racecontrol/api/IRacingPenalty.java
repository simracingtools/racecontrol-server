package de.bausdorf.simracing.racecontrol.api;

public enum IRacingPenalty {
	MESSAGE("/all <driver> <message>"),
	DRIVE_THROUGH("!black <driver> D"),
	STOP_AND_HOLD("!black <driver> <sec>"),
	DISQUALIFICATION("!dq <driver> <message>"),
	EXCLUSION("!remove <driver> <message>");

	private final String command;

	private IRacingPenalty(String command) {
		this.command = command;
	}

	public String issue(int carNo) {
		return command.replaceFirst(Constants.DRIVER_MARKER, "#" + carNo)
				.replaceFirst(Constants.TIME_MARKER, "")
				.replaceFirst(Constants.MESSAGE_MARKER, "").trim();
	}

	public String issue(int carNo, int seconds) {
		return command.replaceFirst(Constants.DRIVER_MARKER, "#" + carNo)
				.replaceFirst(Constants.TIME_MARKER, String.valueOf(seconds))
				.replaceFirst(Constants.MESSAGE_MARKER, "").trim();

	}

	public String clear(int carNo, String message) {
		return "!clear #" + carNo + " " + message;
	}

	public String issue(int carNo, String message) {
		return command.replaceFirst(Constants.DRIVER_MARKER, "#" + carNo)
				.replaceFirst(Constants.TIME_MARKER, "")
				.replaceFirst(Constants.MESSAGE_MARKER, message).trim();
	}

	public static class Constants {
		public static final String DRIVER_MARKER = "<driver>";
		public static final String TIME_MARKER = "<sec>";
		public static final String MESSAGE_MARKER = "<message>";

		private Constants() {}
	}
}
