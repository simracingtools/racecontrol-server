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
import java.util.stream.Collectors;

import de.bausdorf.simracing.irdataapi.client.AuthorizationException;
import de.bausdorf.simracing.irdataapi.client.DataApiException;
import de.bausdorf.simracing.irdataapi.client.IrDataClient;
import de.bausdorf.simracing.irdataapi.model.LoginRequestDto;
import de.bausdorf.simracing.irdataapi.model.MembersInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IRacingClient {

	private final RacecontrolServerProperties serverProperties;
	private final IrDataClient dataClient;

	public IRacingClient(@Autowired RacecontrolServerProperties serverProperties) {
		this.serverProperties = serverProperties;
		this.dataClient = new IrDataClient();
	}

	public List<MemberInfo> getMemberInfo(List<Long> ircacingIds) {
		try{
			authenticate();
			MembersInfoDto membersInfos = dataClient.getMembersInfo(ircacingIds);
			return Arrays.stream(membersInfos.getMembers())
					.map(s -> MemberInfo.builder()
							.custid(s.getCust_id().intValue())
							.name(s.getDisplay_name())
							.build())
					.collect(Collectors.toList());
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}

	private void authenticate() {
		if(!dataClient.isAuthenticated()) {
			LoginRequestDto loginRequestDto = LoginRequestDto.builder()
					.email(serverProperties.getIRacingUsername())
					.password(serverProperties.getIRacingPassword())
					.build();
			try {
				dataClient.authenticate(loginRequestDto);
			} catch (DataApiException | AuthorizationException e) {
				log.error(e.getMessage());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
