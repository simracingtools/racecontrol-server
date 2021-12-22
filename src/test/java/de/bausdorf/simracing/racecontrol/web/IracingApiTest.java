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
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Disabled;
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
import org.testcontainers.shaded.okhttp3.OkHttpClient;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class IracingApiTest {

	private static String URL_MEMBERS_HOMEPAGE_BASE = "http://members.iracing.com";
	private static String URL_MEMBERS_HOMEPAGE_BASE_SECURE = "https://members.iracing.com";
	private static String URI_ABSOLUTE_PATH_LOGIN = "/membersite/login.jsp";
	private static String URI_ABSOLUTE_PATH_LOGIN_TARGET = "/membersite/Login";
	private static String URI_ABSOLUTE_PATH_FAILED_LOGIN = "/membersite/failedlogin.jsp";
	private static String URI_ABSOLUTE_PATH_HOME = "/membersite/member/Home.do";
	private static String URI_ABSOLUTE_PATH_STATS = "/membersite/member/results.jsp";

	static final String SET_COOKIE = "Set-Cookie";
	static final String COOKIE = "Cookie";

	@Autowired
	RacecontrolServerProperties props;

	private List<HttpCookie> cookies;
	@Test
	@Disabled
	void testIracingLogin() {
		try {
			String urltext = URL_MEMBERS_HOMEPAGE_BASE_SECURE + URI_ABSOLUTE_PATH_LOGIN_TARGET;

			SslContextFactory sslContextFactory = new SslContextFactory();
			HttpClient httpClient = new HttpClient(sslContextFactory);
			httpClient.setFollowRedirects(true);
			httpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36"));
			httpClient.start();
//			httpClient.setCookieStore();

			ContentResponse response = httpClient.GET("https://members.iracing.com/membersite/login.jsp");
					;
			log.info(response.getContentAsString());

			String cookies = response.getHeaders().get(SET_COOKIE);

			Request request = httpClient.POST("https://members.iracing.com/membersite/Login")
					.header(HttpHeader.CONTENT_TYPE, "application/x-www-form-urlencoded")
					.header(HttpHeader.COOKIE, cookies)
					.param("username", "robbyb@mailbox.org")
					.param("password", "2Zggq8ciRgCyKu")
					.param("utcoffset", "-60")
					.param("todaysdate", "");
			addRequestHeaders(request);
			response = request.send();

			String ssoCookies = response.getHeaders().get(HttpHeader.COOKIE);

			log.info(response.getContentAsString());

			String searchUrl = "https://members.iracing.com/membersite/member/GetDriverStatus?searchTerms=Robert%20Bausdorf";
			response = httpClient.GET(searchUrl);
			log.info(response.getContentAsString());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void addRequestHeaders(Request request) {
		request.header(HttpHeader.REFERER, "https://members.iracing.com/membersite/login.jsp")
				.header(HttpHeader.CONNECTION, "keep-alive")
				.header(HttpHeader.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.header(HttpHeader.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.3")
				.header(HttpHeader.ACCEPT_ENCODING, "gzip,deflate,sdch")
				.header(HttpHeader.CACHE_CONTROL, "max-age=0")
				.header(HttpHeader.HOST, "members.iracing.com")
				.header(HttpHeader.ORIGIN, "members.iracing.com")
				.header(HttpHeader.ACCEPT_LANGUAGE, "en-US,en;q=0.8");
	}
}
