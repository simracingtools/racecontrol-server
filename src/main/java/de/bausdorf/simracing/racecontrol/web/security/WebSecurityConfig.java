package de.bausdorf.simracing.racecontrol.web.security;

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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import lombok.extern.slf4j.Slf4j;

@KeycloakConfiguration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@Slf4j
public class WebSecurityConfig extends KeycloakWebSecurityConfigurerAdapter implements AccessDeniedHandler {

	public static final String ROLE_PREFIX = "ROLE_";

	private final RcAuthenticationProvider authenticationProvider;

	public WebSecurityConfig(@Autowired RcAuthenticationProvider authenticationProvider) {
		super();
		this.authenticationProvider = authenticationProvider;
	}

	@Override
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(
				new SessionRegistryImpl());
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) {
		auth.authenticationProvider(authenticationProvider);
	}

	@Override
	@Bean
	protected KeycloakAuthenticationProcessingFilter keycloakAuthenticationProcessingFilter() throws Exception {
		KeycloakAuthenticationProcessingFilter filter = new KeycloakAuthenticationProcessingFilter(this.authenticationManagerBean());
		filter.setSessionAuthenticationStrategy(this.sessionAuthenticationStrategy());
		return filter;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		super.configure(http);
		http
				.csrf().disable()
				.headers()
				.frameOptions().sameOrigin()
				.and()
				.authorizeRequests()
				.antMatchers("/").permitAll()
				.antMatchers("/favicon*.*").permitAll()
				.antMatchers("/assets/**").permitAll()
				.antMatchers("/webjars/**").permitAll()
				.antMatchers("/app/**").permitAll()
				.antMatchers("/clientmessage/**").permitAll()
				.antMatchers("/rcclient/**").permitAll()
				.antMatchers("/rc/**").permitAll()
				.antMatchers("/timingclient/**").permitAll()
				.antMatchers("/timing/**").permitAll()
				.antMatchers("/session/**").permitAll()
				.antMatchers("/team/**").permitAll()
				.antMatchers("/events/**").permitAll()
				.antMatchers("/issueBulletin/**").permitAll()
				.antMatchers("/bulletins/**").permitAll()
				.anyRequest().authenticated()
				.and()
				.exceptionHandling().accessDeniedHandler(this);
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
