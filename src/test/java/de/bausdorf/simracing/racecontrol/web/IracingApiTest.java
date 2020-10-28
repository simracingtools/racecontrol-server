package de.bausdorf.simracing.racecontrol.web;

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

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.ClientHttpRequestFactorySupplier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class IracingApiTest {

	private static String URL_MEMBERS_HOMEPAGE_BASE = "http://members.iracing.com";
	private static String URL_MEMBERS_HOMEPAGE_BASE_SECURE = "https://members.iracing.com";
	private static String URI_ABSOLUTE_PATH_LOGIN = "/membersite/login.jsp";
	private static String URI_ABSOLUTE_PATH_LOGIN_TARGET = "/membersite/Login";
	private static String URI_ABSOLUTE_PATH_FAILED_LOGIN = "/membersite/failedlogin.jsp";
	private static String URI_ABSOLUTE_PATH_HOME = "/membersite/member/Home.do";
	private static String URI_ABSOLUTE_PATH_STATS = "/membersite/member/results.jsp";

	@Autowired
	CookieHandlingRestTemplate restTemplate;

	@Autowired
	RacecontrolServerProperties props;

//	@Test
	void testIracingLogin() {
		try {
			String encodedUsername = URLEncoder.encode(props.getIRacingUsername(), "UTF-8");
			String encodedPW = URLEncoder.encode(props.getIRacingPassword(), "UTF-8");

			String urltext = URL_MEMBERS_HOMEPAGE_BASE_SECURE + URI_ABSOLUTE_PATH_LOGIN + "?username=" + encodedUsername +
					"&password=" + encodedPW; // + "&utcoffset=-60&todaysdate=";

			String searchUrl = "https://members.iracing.com/membersite/member/GetDriverStatus?searchTerms=Robert Bausdorf";

			ResponseEntity<String> response = restTemplate.getForEntity(urltext, String.class);
			log.info(response.toString());

			ResponseEntity<String> memberData = restTemplate.getForEntity(urltext, String.class);

			log.info(memberData.toString());

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
