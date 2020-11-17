package de.bausdorf.simracing.racecontrol.api;

public enum IRacingPenalty {
	MESSAGE("/all <driver> <message>", false),
	DRIVE_THROUGH("!black <driver> D", false),
	STOP_AND_HOLD("!black <driver> <sec>", true),
	DISQUALIFICATION("!dq <driver> <message>", false),
	EXCLUSION("!remove <driver> <message>", false);

	private final String command;
	private final boolean timeParamNeeded;

	private IRacingPenalty(String command, boolean timeParam) {
		this.timeParamNeeded = timeParam;
		this.command = command;
	}

	public boolean isTimeParamNeeded() {
		return timeParamNeeded;
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
