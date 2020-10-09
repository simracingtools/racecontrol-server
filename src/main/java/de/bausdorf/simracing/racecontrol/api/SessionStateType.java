package de.bausdorf.simracing.racecontrol.api;

import lombok.Getter;

public enum SessionStateType {
	INVALID(0),
	GRIDING(1),
	WARMUP(2),
	PARADE_LAPS(3),
	RACING(4),
	CHECKERED(5),
	COOL_DOWN(6);

	@Getter
	private final int typeCode;

	SessionStateType(int typeCode) {
		this.typeCode = typeCode;
	}

	public static SessionStateType ofTypeCode(int code) {
		switch(code) {
			case 0: return INVALID;
			case 1: return GRIDING;
			case 2: return WARMUP;
			case 3: return PARADE_LAPS;
			case 4: return RACING;
			case 5: return CHECKERED;
			case 6: return COOL_DOWN;
			default: throw new IllegalArgumentException("Invalid session state type code: " + code);
		}
	}
}
