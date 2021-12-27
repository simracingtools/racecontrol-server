package de.bausdorf.simracing.racecontrol.web;

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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@ActiveProfiles("test")
class JythonTest {

	@Autowired
	IRacingClient iRacingClient;

	@Autowired
	RacecontrolServerProperties serverProperties;

	@Test
	@Disabled
	void iRacingClientTest() {
		List<MemberInfo> memberInfo = iRacingClient.searchMembers("229120");
		memberInfo.stream().forEach(s -> log.info(s.toString()));

		if(memberInfo.size() == 1) {
			LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(memberInfo.get(0).getLastLogin()), ZoneId.of("UTC"));
			log.info(localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm:ss")));
			localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(memberInfo.get(0).getLastSeen()), ZoneId.of("UTC"));
			log.info(localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm:ss")));
		}
	}
}
