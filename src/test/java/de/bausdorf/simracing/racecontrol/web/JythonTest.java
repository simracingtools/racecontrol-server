package de.bausdorf.simracing.racecontrol.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.iracing.MemberInfo;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@ActiveProfiles("local")
class JythonTest {

	@Autowired
	IRacingClient iRacingClient;

	@Autowired
	RacecontrolServerProperties serverProperties;

	@Test
	@Disabled
	void jythonInterpreterPlainTest() {
		System.getProperties().put("python.home", "/home/robert/jython2.7.2");
		PySystemState systemState = Py.getSystemState();
		systemState.path.append(new PyString("/home/robert/jython2.7.2"));
		try(PythonInterpreter pyInterp = new PythonInterpreter()) {
			pyInterp.exec("from ir_webstats.client import iRWebStats");
			pyInterp.exec("irw = iRWebStats(False)");
			pyInterp.exec("irw.login('"
					+ "robbyb@mailbox.org" + "', '"
					+ "2Zggq8ciRgCyKu" + "')");
			pyInterp.exec("driver_info = str(irw.driverdata('229120'))");
			String driver_data = pyInterp.get("driver_info").asString().replaceAll("u'", "'");
			log.info(driver_data);
		}
	}

	@Test
	@Disabled
	void jythonInterpreterTest() {
		System.getProperties().put("python.home", "/home/robert/jython2.7.2");
		try(PythonInterpreter pyInterp = new PythonInterpreter()) {
			pyInterp.exec("from ir_webstats.client import iRWebStats");
			pyInterp.exec("irw = iRWebStats(False)");
			pyInterp.exec("irw.login('"
					+ serverProperties.getIRacingUsername() + "', '"
					+ serverProperties.getIRacingPassword() + "')");
			pyInterp.exec("driver_info = str(irw.driverdata('229120'))");
			String driver_data = pyInterp.get("driver_info").asString().replaceAll("u'", "'");
			log.info(driver_data);
		}
	}

	@Test
	@Disabled
	void iRacingClientTest() throws JsonProcessingException {
		List<MemberInfo> memberInfo = iRacingClient.searchMembers("229120");
		memberInfo.stream().forEach(s -> log.info(s.toString()));
		assertThat(memberInfo).hasSize(1);

		LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(memberInfo.get(0).getLastLogin()), ZoneId.of("UTC"));
		log.info(localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm:ss")));
		localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(memberInfo.get(0).getLastSeen()), ZoneId.of("UTC"));
		log.info(localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm:ss")));
	}
}
