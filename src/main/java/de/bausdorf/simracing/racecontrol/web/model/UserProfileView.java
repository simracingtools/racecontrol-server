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

import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserType;
import de.bausdorf.simracing.racecontrol.web.security.SubscriptionType;
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
	private String iRacingName;
	private String clientMessageAccessToken;
	private String userType;
	private SubscriptionType subscriptionType;
	private String subscriptionExpiration;
	private String timezone;
	private Boolean enabled;
	private Boolean locked;
	private Boolean expired;

	// Read-only
	private String username;

	public UserProfileView(RcUser user) {
		this.id = user.getOauthId();
		this.name = user.getName();
		this.email = user.getEmail();
		this.imageUrl = user.getImageUrl();
		this.iRacingId = user.getIRacingId();
		this.userType = user.getUserType().name();
		this.clientMessageAccessToken = user.getClientMessageAccessToken();
		this.enabled = user.isEnabled();
		this.locked = user.isLocked();
		this.expired = user.isExpired();
		this.username = user.getUsername();
		this.timezone = user.getTimezone() != null ? TimeTools.toShortZoneId(user.getTimezone()) : "";
		this.subscriptionType = user.getSubscriptionType();
//		this.subscriptionExpiration = subscriptionType != SubscriptionType.NONE
//				? user.getLastSubscription().plus(this.subscriptionType.getDuration()).toLocalDate().toString()
//				: "Not relevant";
	}

	public RcUser getUser(RcUser merge) {
		return RcUser.builder()
				.name(name != null ? name : merge.getName())
				.oauthId(id != null ? id : merge.getOauthId())
				.email(email != null ? email : merge.getEmail())
				.imageUrl(imageUrl != null ? imageUrl : merge.getImageUrl())
				.clientMessageAccessToken(clientMessageAccessToken != null ? clientMessageAccessToken : merge.getClientMessageAccessToken())
				.enabled(enabled != null ? enabled : merge.isEnabled())
				.locked(locked != null ? locked : merge.isLocked())
				.expired(expired != null ? expired : merge.isExpired())
				.iRacingId(iRacingId != 0 ? iRacingId : merge.getIRacingId())
				.userType(userType != null ? RcUserType.ofText(userType) : merge.getUserType())
				.timezone(timezone != null ? ZoneId.of(timezone) : merge.getTimezone())
				.created(merge.getCreated())
				.lastAccess(merge.getLastAccess())
				.lastSubscription(merge.getLastSubscription())
				.subscriptionType(merge.getSubscriptionType())
				.build();
	}
}
