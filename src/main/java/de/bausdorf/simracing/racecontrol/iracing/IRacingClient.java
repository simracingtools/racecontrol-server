package de.bausdorf.simracing.racecontrol.iracing;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IRacingClient {

	private final RacecontrolServerProperties serverProperties;
	private final PythonInterpreter pyInterp;

	public IRacingClient(@Autowired RacecontrolServerProperties serverProperties) {
		this.serverProperties = serverProperties;
		System.getProperties().put("python.home", serverProperties.getJythonHome());
		pyInterp = new PythonInterpreter();
		log.info("Initialize jython ...");
		pyInterp.exec("from ir_webstats.client import iRWebStats");
		pyInterp.exec("irw = iRWebStats(False)");
		log.info("... done.");
	}

	public List<MemberInfo> searchMembers(String search) {
		if(serverProperties.getJythonHome().isEmpty()) {
			return Collections.emptyList();
		}

		try{
			pyInterp.exec("irw.login('"
					+ serverProperties.getIRacingUsername() + "', '"
					+ serverProperties.getIRacingPassword() + "')");
			pyInterp.exec("driver_info = irw.driverdata('" + search + "')");
			PyDictionary dict = (PyDictionary)pyInterp.get("driver_info");
			PyList searchResults = (PyList) dict.get(new PyString("searchRacers"));
			return (List<MemberInfo>) searchResults.stream()
					.map(s -> MemberInfo.builder()
							.custid(((PyDictionary)s).get(new PyString("custid")).asInt())
							.name(((PyDictionary)s).get(new PyString("name")).asString().replace("+", " "))
							.lastLogin(((PyDictionary)s).get(new PyString("lastLogin")).asLong())
							.lastSeen(((PyDictionary)s).get(new PyString("lastSeen")).asLong())
					.build())
					.collect(Collectors.toList());
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}
}
