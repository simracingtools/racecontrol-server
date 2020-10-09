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
