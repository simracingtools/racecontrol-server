package de.bausdorf.simracing.racecontrol.api;

public enum EventType {
	DRIVER_CHANGE("DriverChange"),
	ENTER_PITLANE("PitEnter"),
	EXIT_PITLANE("PitExit"),
	APPROACHING_PITS("AproachingPits"),
	OFF_TRACK("OffTrack"),
	OFF_WORLD("OffWorld"),
	ON_TRACK("OnTrack");

	private final String eventType;

	EventType(String trackLocation) {
		this.eventType = trackLocation;
	}

	public String getEventType() {
		return this.eventType;
	}

	public static EventType ofType(String type) {
		switch(type) {
			case "DriverChange": return DRIVER_CHANGE;
			case "PitEnter": return ENTER_PITLANE;
			case "PitExit": return EXIT_PITLANE;
			case "AproachingPits": return APPROACHING_PITS;
			case "OffTrack": return OFF_TRACK;
			case "OffWorld": return OFF_WORLD;
			case "OnTrack": return ON_TRACK;
			default: throw new IllegalArgumentException("Unknown EventType " + type);
		}
	}
}
