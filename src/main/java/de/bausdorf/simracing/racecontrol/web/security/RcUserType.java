package de.bausdorf.simracing.racecontrol.web.security;

import org.springframework.security.core.GrantedAuthority;

public enum RcUserType implements GrantedAuthority {
	SYSADMIN(Constants.SYSADMIN_NAME),
	RACE_DIRECTOR(Constants.RACE_DIRECTOR_NAME),
	STEWARD(Constants.STEWARD_NAME),
	STAFF(Constants.STAFF_NAME),
	BROADCAST(Constants.BROADCAST_NAME),
	REGISTERED_USER(Constants.REGISTERED_USER_NAME),
	NEW(Constants.NEW_USER_NAME);

	private String name;

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
			case Constants.BROADCAST_NAME: return BROADCAST;
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

	private static class Constants {

		public static final String SYSADMIN_NAME = "Sysadmin";
		public static final String RACE_DIRECTOR_NAME = "Race Director";
		public static final String STEWARD_NAME = "Race steward";
		public static final String STAFF_NAME = "Staff";
		public static final String BROADCAST_NAME = "Broadcast";
		public static final String REGISTERED_USER_NAME = "Registered user";
		public static final String NEW_USER_NAME = "New user";
	}
}
