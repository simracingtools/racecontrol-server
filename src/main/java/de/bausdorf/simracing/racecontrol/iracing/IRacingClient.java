package de.bausdorf.simracing.racecontrol.iracing;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2021 bausdorf engineering
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.bausdorf.simracing.irdataapi.client.AuthorizationException;
import de.bausdorf.simracing.irdataapi.client.DataApiException;
import de.bausdorf.simracing.irdataapi.client.IrDataClient;
import de.bausdorf.simracing.irdataapi.client.impl.IrDataClientImpl;
import de.bausdorf.simracing.irdataapi.config.ConfigProperties;
import de.bausdorf.simracing.irdataapi.model.LeagueInfoDto;
import de.bausdorf.simracing.irdataapi.model.LoginRequestDto;
import de.bausdorf.simracing.irdataapi.model.MembersInfoDto;
import de.bausdorf.simracing.irdataapi.tools.StockDataCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

@Component
@Slf4j
public class IRacingClient {

	private final ConfigProperties serverProperties;
	private final IrDataClient dataClient;
	private final StockDataCache dataCache;

	public IRacingClient(@Autowired ConfigProperties serverProperties) {
		this.serverProperties = serverProperties;
		this.dataClient = new IrDataClientImpl();
		this.dataCache = new StockDataCache(serverProperties.getCacheDirectory());
	}

	public StockDataCache getDataCache() {
		if(!dataCache.isInitialized()) {
			authenticate();
			dataCache.fetchFromService(dataClient);
		}
		return dataCache;
	}

	public Optional<MemberInfo> getMemberInfo(Long ircacingId) {
		return getMemberInfo(List.of(ircacingId)).stream().findFirst();
	}

	public List<MemberInfo> getMemberInfo(List<Long> ircacingIds) {
		try{
			authenticate();
			MembersInfoDto membersInfos = dataClient.getMembersInfo(ircacingIds);
			return Arrays.stream(membersInfos.getMembers())
					.map(s -> MemberInfo.builder()
							.custid(s.getCustId().intValue())
							.name(s.getDisplayName())
							.build())
					.collect(Collectors.toList());
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	public LeagueInfoDto getLeagueInfo(long leagueId) {
		try {
			authenticate();
			return dataClient.getLeagueInfo(leagueId);
		} catch(HttpClientErrorException clientError) {
			log.warn("iRacing http error {}: {}",clientError.getRawStatusCode(), clientError.getResponseBodyAsString());
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	private void authenticate() {
		try {
			dataClient.getMemberSummary(229120L);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				log.warn("IrDataClient not authorized - try to authenticate");
				LoginRequestDto loginRequestDto = LoginRequestDto.builder()
						.email(serverProperties.getUser())
						.password(serverProperties.getPassword())
						.build();
				try {
					dataClient.authenticate(loginRequestDto);
					log.info("IrDataClient authentication success");
				} catch (DataApiException | AuthorizationException exx) {
					log.error(exx.getMessage());
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			}
		}
	}
}
