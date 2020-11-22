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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

//@Component
public class CookieHandlingRestTemplate extends RestTemplate{

	public static final String COOKIE = "Cookie";
	private final List<HttpCookie> cookies = new ArrayList<>();

	public CookieHandlingRestTemplate() {
		super();
	}

	public CookieHandlingRestTemplate(ClientHttpRequestFactory requestFactory) {
		super(requestFactory);
	}

	public synchronized List<HttpCookie> getCookies() {
		return cookies;
	}

	public synchronized void resetCookies() {
		cookies.clear();
	}

	private void processHeaders(HttpHeaders headers) {
		final List<String> cooks = headers.get("Set-Cookie");
		if (cooks != null && !cooks.isEmpty()) {
			cooks.stream().map(HttpCookie::parse).forEachOrdered(cook ->
				cook.forEach(a -> {
					HttpCookie cookieExists = cookies.stream().filter(x -> a.getName().equals(x.getName())).findAny().orElse(null);
					if (cookieExists != null) {
						cookies.remove(cookieExists);
					}
					cookies.add(a);
				})
			);
		}
	}

	@Override
	protected <T extends Object> T doExecute(URI url, HttpMethod method, final RequestCallback requestCallback,
			final ResponseExtractor<T> responseExtractor) {


		return super.doExecute(url, method, new RequestCallback() {
			@Override
			public void doWithRequest(ClientHttpRequest chr) throws IOException {

				for (HttpCookie cookie : cookies) {
					StringBuilder sb = new StringBuilder()
							.append(cookie.getName())
							.append("=")
							.append(cookie.getValue());

					if(chr.getHeaders().get(COOKIE) == null) {
						chr.getHeaders().set(COOKIE, sb.toString());
					} else {
						chr.getHeaders().get(COOKIE).add(sb.toString());
					}
				}
				requestCallback.doWithRequest(chr);
			}

		}, new ResponseExtractor<T>() {
			@Override
			public T extractData(ClientHttpResponse chr) throws IOException {
				processHeaders(chr.getHeaders());
				return responseExtractor.extractData(chr);
			}
		});
	}
}
