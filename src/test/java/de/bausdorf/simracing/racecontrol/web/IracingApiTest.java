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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ActiveProfiles("test")
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

	//@Test
	void testIracingLogin() {
		try {
			String urltext = URL_MEMBERS_HOMEPAGE_BASE_SECURE + URI_ABSOLUTE_PATH_LOGIN_TARGET;

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
			map.add("username", props.getIRacingUsername());
			map.add("password", props.getIRacingPassword());
			map.add("utcoffset", "-60");
			map.add("todaysdate", "");

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(urltext, request, String.class);
			if(response.getStatusCode() == HttpStatus.FOUND) {
				ResponseEntity<String> memberPage = restTemplate.getForEntity(response.getHeaders().get("Location").get(0).replaceFirst("http", "https"), String.class);
				log.info(memberPage.toString());
			}

			String searchUrl = "https://members.iracing.com/membersite/member/GetDriverStatus?searchTerms=Robert Bausdorf";


			ResponseEntity<String> memberData = restTemplate.getForEntity(searchUrl, String.class);

			log.info(memberData.toString());

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
