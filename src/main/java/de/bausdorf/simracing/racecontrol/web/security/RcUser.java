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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import de.bausdorf.simracing.racecontrol.live.api.EventType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class RcUser implements UserDetails {
	@Id
	String oauthId;

	private String name;
	private String email;
	private String discordHandle;
	private String imageUrl;
	private long iRacingId;
	private String clientMessageAccessToken;
	private RcUserType userType;
	@Convert(converter = ZoneIdConverter.class)
	private ZoneId timezone;
	private String localeTag;
	private String irClubName;
	private LocalDateTime created;
	private LocalDateTime lastAccess;
	private LocalDateTime lastSubscription;
	private SubscriptionType subscriptionType;
	@ElementCollection(targetClass=EventType.class)
	private Set<EventType> eventFilter;
	private boolean enabled;
	private boolean locked;
	private boolean expired;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(userType);
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return !isExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return !isLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return !isEnabled();
	}
}
