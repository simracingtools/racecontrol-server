package de.bausdorf.simracing.racecontrol.api;

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
