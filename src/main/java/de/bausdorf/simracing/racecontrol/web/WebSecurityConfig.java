package de.bausdorf.simracing.racecontrol.web;

/*-
 * #%L
 * tt-cloud-server
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

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.transaction.annotation.Transactional;

import de.bausdorf.simracing.racecontrol.web.security.GoogleUserService;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import de.bausdorf.simracing.racecontrol.web.security.RcUserRepository;
import de.bausdorf.simracing.racecontrol.web.security.RcUserType;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@Slf4j
public class WebSecurityConfig extends WebSecurityConfigurerAdapter implements AccessDeniedHandler {

	public static final String ROLE_PREFIX = "ROLE_";

	private final GoogleUserService userService;
	private final RcUserRepository registrationRepository;

	public WebSecurityConfig(@Autowired GoogleUserService userService,
			@Autowired	RcUserRepository registrationRepository) {
		super(false);
		this.userService = userService;
		this.registrationRepository = registrationRepository;
	}

	@Override
	public void configure(WebSecurity web) {
		web.ignoring().antMatchers("/_ah/**", "/clientmessage",
				"/rcclient/**", "/timingclient/**", "/app/**", "/timing/**", "/rc/**",
				"/", "/index", "/session/**", "/team**", "/events/**", "/issueBulletin", "/bulletins",
				"/assets/**", "/webjars/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("!/_ah", "!/clientmessage",
						"!/rcclient/**", "!/timingclient/**", "!/app/**", "!/timing/**", "!/rc/**",
						"!/", "!/index", "!/session/**", "!/team/**", "!/events/**", "!/issueBulletin", "!/bulletins/**",
						"!/assets/**", "!/webjars/**")
					.permitAll()
					.anyRequest().authenticated()
				.and()
				.rememberMe()
					.key("ir-race-control")
					.tokenValiditySeconds(90000)
				.and()
				.oauth2Login()
					.authorizationEndpoint()
//					.authorizationRequestResolver(
//						new CustomAuthorizationRequestResolver(
//								registrationRepository))
				.and()
				.defaultSuccessUrl("/index.html")
				.userInfoEndpoint()
					.userAuthoritiesMapper(this.userAuthoritiesMapper())
						.oidcUserService(userService)
					.and()
				.and()
					.logout()
						.logoutSuccessUrl("/")
				.and()
			.exceptionHandling().accessDeniedHandler(this);


	}

	private GrantedAuthoritiesMapper userAuthoritiesMapper() {
		return authorities -> {
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

			authorities.forEach(authority -> {
				if (authority instanceof OidcUserAuthority) {
					OidcUserAuthority oidcUserAuthority = (OidcUserAuthority)authority;

					determineUserRoles(oidcUserAuthority.getIdToken().getSubject())
							.forEach(mappedAuthorities::add);
				} else if (authority instanceof OAuth2UserAuthority) {
					OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority)authority;

					determineUserRoles(oauth2UserAuthority.getAttributes().get("sub").toString())
							.forEach(mappedAuthorities::add);
				}
			});

			return mappedAuthorities;
		};
	}

	@Transactional
	public List<SimpleGrantedAuthority> determineUserRoles(String userId) {
		RcUser rcUser = registrationRepository.findById(userId).orElse(null);
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		if (rcUser != null) {
			rcUser.setLastAccess(ZonedDateTime.now());
			if (!rcUser.isEnabled()) {
				authorities.clear();
				authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + RcUserType.NEW.name()));
				rcUser.setUserType(RcUserType.NEW);
			}
			authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + rcUser.getUserType().name()));
		}
		return authorities;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
			throws IOException {

		Authentication auth
				= SecurityContextHolder.getContext().getAuthentication();
		if (auth != null) {
			log.warn("User: {}({}) attempted to access the protected URL: {}",
					auth.getName(), auth.getAuthorities(), request.getRequestURI());
		}

		response.sendRedirect(request.getContextPath() + "/index?error=Access%20denied%20to%20" + request.getRequestURI());
	}

	public static void updateCurrentUserRole(RcUserType newRole) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		List<GrantedAuthority> updatedAuthorities = new ArrayList<>(auth.getAuthorities());
		updatedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + newRole.name()));
		Authentication newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), updatedAuthorities);
		SecurityContextHolder.getContext().setAuthentication(newAuth);
	}
}
