package de.bausdorf.simracing.racecontrol.web.security;

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

import java.util.function.Supplier;

import org.springframework.security.core.GrantedAuthority;

public enum RcUserType implements GrantedAuthority, Supplier<RcUserType> {
	SYSADMIN(Constants.SYSADMIN_NAME),
	RACE_DIRECTOR(Constants.RACE_DIRECTOR_NAME),
	STEWARD(Constants.STEWARD_NAME),
	STAFF(Constants.STAFF_NAME),
	REGISTERED_USER(Constants.REGISTERED_USER_NAME),
	NEW(Constants.NEW_USER_NAME);

	private final String name;

	RcUserType(String name) {
		this.name = name;
	}

	public String toText() {
		return this.name;
	}

	public static RcUserType ofText(String text) {
		switch(text) {
			case Constants.SYSADMIN_NAME: return SYSADMIN;
			case Constants.RACE_DIRECTOR_NAME: return RACE_DIRECTOR;
			case Constants.STEWARD_NAME: return STEWARD;
			case Constants.STAFF_NAME: return STAFF;
			case Constants.REGISTERED_USER_NAME: return REGISTERED_USER;
			case Constants.NEW_USER_NAME: return NEW;
			default:
				throw new IllegalArgumentException("Unknown TtUserType: " + text);
		}
	}
	@Override
	public String getAuthority() {
		return name();
	}

	@Override
	public RcUserType get() {
		return this;
	}

	private static class Constants {

		public static final String SYSADMIN_NAME = "Sysadmin";
		public static final String RACE_DIRECTOR_NAME = "Race Director";
		public static final String STEWARD_NAME = "Race steward";
		public static final String STAFF_NAME = "Staff";
		public static final String REGISTERED_USER_NAME = "Registered user";
		public static final String NEW_USER_NAME = "New user";
	}
}
