package de.bausdorf.simracing.racecontrol.web.security;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class RcUser implements UserDetails {
	@Id
	String oauthId;

	private String name;
	private String email;
	private String imageUrl;
	private String iRacingId;
	private String clientMessageAccessToken;
	@ElementCollection(targetClass = RcUserType.class)
	@CollectionTable
	@Enumerated(EnumType.STRING)
	private Collection<RcUserType> userType;
	private ZoneId timezone;
	private ZonedDateTime created;
	private ZonedDateTime lastAccess;
	private ZonedDateTime lastSubscription;
	private SubscriptionType subscriptionType;
	private boolean enabled;
	private boolean locked;
	private boolean expired;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return userType;
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
