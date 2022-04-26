package de.bausdorf.simracing.racecontrol.orga.api;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2021 bausdorf engineering
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum OrgaRoleType {
	SUPPORT(false, 1, null),
	PARTICIPANT(false, 2, null),
	STAFF(true, 3, "Organization"),
	STEWARD(true, 4, "RaceControl"),
	RACE_DIRECTOR(true, 5, "RaceControl");

	private final boolean racecontrol;
	private final int code;
	private final String discordRoleName;

	OrgaRoleType(boolean racecontrol, int code, String discordRoleName) {
		this.racecontrol = racecontrol;
		this.code = code;
		this.discordRoleName = discordRoleName;
	}

	public boolean isRacecontrol() {
		return racecontrol;
	}

	public boolean isParticipant() {
		return !racecontrol;
	}

	public int code() {
		return code;
	}

	public String discordRoleName() {
		return discordRoleName;
	}

	public static List<OrgaRoleType> racecontrolValues() {
		return Arrays.stream(OrgaRoleType.values()).filter(OrgaRoleType::isRacecontrol).collect(Collectors.toList());
	}

	public static List<OrgaRoleType> participantValues() {
		return Arrays.stream(OrgaRoleType.values()).filter(OrgaRoleType::isParticipant).collect(Collectors.toList());
	}

	public static OrgaRoleType ofCode(int enumCode) {
		switch (enumCode) {
			case 1: return SUPPORT;
			case 2: return PARTICIPANT;
			case 3: return STAFF;
			case 4: return STEWARD;
			case 5: return RACE_DIRECTOR;
			default: throw new IllegalArgumentException("Unknown OrgaRoleType code '" + enumCode + "'");
		}
	}
}
