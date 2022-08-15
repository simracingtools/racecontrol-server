package de.bausdorf.simracing.racecontrol.web.model;

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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import de.bausdorf.simracing.racecontrol.live.api.EventType;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.security.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileView {
	private String id;
	private String name;
	private String email;
	private String imageUrl;
	private long iRacingId;
	private String irClubName;
	private String clientMessageAccessToken;
	private String userType;
	private SubscriptionType subscriptionType;
	private String subscriptionExpiration;
	private String timezone;
	private String localeTag;
	private String created;
	private List<String> eventFilter;
	private Boolean enabled;
	private Boolean locked;
	private Boolean expired;
	private Boolean racecontrol;
	private Boolean racedirection;

	// Read-only
	private String username;

	public boolean isKnown() {
		return !"Unknown".equalsIgnoreCase(name);
	}

	public String getInitials() {
		if(name == null) {
			return "--";
		}
		String[] nameParts = name.split(" ");
		StringBuilder initials = new StringBuilder();
		for (String namePart : nameParts) {
			initials.append(namePart.isEmpty() ? "" : namePart.charAt(0));
		}
		return initials.toString();
	}

	public UserProfileView(RcUser user) {
		this.id = user.getOauthId();
		this.name = user.getName();
		this.email = user.getEmail();
		this.imageUrl = user.getImageUrl();
		this.iRacingId = user.getIRacingId();
		this.irClubName = user.getIrClubName();
		this.userType = user.getUserType().name();
		this.clientMessageAccessToken = user.getClientMessageAccessToken();
		this.enabled = user.isEnabled();
		this.locked = user.isLocked();
		this.expired = user.isExpired();
		this.username = user.getUsername();
		this.timezone = user.getTimezone() != null ? TimeTools.toShortZoneId(user.getTimezone()) : "";
		this.localeTag = user.getLocaleTag() != null ? user.getLocaleTag() : "";
		this.subscriptionType = user.getSubscriptionType();
		this.created = user.getCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); //2020-10-28T20:02
		this.eventFilter = user.getEventFilter() != null ? user.getEventFilter().stream()
						.map(EventType::name)
						.collect(Collectors.toList())
				: RcAuthenticationProvider.defaultEventFilter().stream()
						.map(EventType::name)
						.collect(Collectors.toList());
		this.racecontrol = user.getUserType() == RcUserType.SYSADMIN
				|| user.getUserType() == RcUserType.RACE_DIRECTOR
				|| user.getUserType() == RcUserType.STEWARD;
		this.racedirection = user.getUserType() == RcUserType.SYSADMIN
				|| user.getUserType() == RcUserType.RACE_DIRECTOR;
	}

	public RcUser apply(RcUser merge) {
		merge.setClientMessageAccessToken(clientMessageAccessToken != null ? clientMessageAccessToken : merge.getClientMessageAccessToken());
		merge.setName(name != null ? name : merge.getName());
		merge.setIRacingId(iRacingId != 0 ? iRacingId : merge.getIRacingId());
		merge.setIrClubName(irClubName != null ? irClubName : merge.getIrClubName());
		merge.setUserType(userType != null ? RcUserType.valueOf(userType) : merge.getUserType());
		merge.setTimezone(timezone != null ? ZoneId.of(timezone) : merge.getTimezone());
		merge.setLocaleTag(localeTag != null ? localeTag : merge.getLocaleTag());
		return merge;
	}
}
