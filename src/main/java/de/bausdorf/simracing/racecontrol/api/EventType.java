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

public enum EventType {
	DRIVER_CHANGE("DriverChange"),
	ENTER_PITLANE("PitEnter"),
	EXIT_PITLANE("PitExit"),
	IN_PIT_STALL("InPitStall"),
	APPROACHING_PITS("AproachingPits"),
	OFF_TRACK("OffTrack"),
	OFF_WORLD("OffWorld"),
	ON_TRACK("OnTrack");

	private final String eventString;

	EventType(String trackLocation) {
		this.eventString = trackLocation;
	}

	public String getEventString() {
		return this.eventString;
	}

	public static EventType ofType(String type) {
		switch(type) {
			case "DriverChange": return DRIVER_CHANGE;
			case "PitEnter": return ENTER_PITLANE;
			case "PitExit": return EXIT_PITLANE;
			case "AproachingPits": return APPROACHING_PITS;
			case "InPitStall": return IN_PIT_STALL;
			case "OffTrack": return OFF_TRACK;
			case "OffWorld": return OFF_WORLD;
			case "OnTrack": return ON_TRACK;
			default: throw new IllegalArgumentException("Unknown EventType " + type);
		}
	}
}
