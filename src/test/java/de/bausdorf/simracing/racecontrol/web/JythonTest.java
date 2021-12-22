package de.bausdorf.simracing.racecontrol.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;

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
	void iRacingClientTest() throws JsonProcessingException {
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
