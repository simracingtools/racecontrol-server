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
		if(!serverProperties.getJythonHome().isEmpty()) {
			System.getProperties().put("python.home", serverProperties.getJythonHome());
			pyInterp = new PythonInterpreter();
			log.info("Initialize jython ...");
			pyInterp.exec("from ir_webstats.client import iRWebStats");
			pyInterp.exec("irw = iRWebStats(False)");
			log.info("... done.");
		} else {
			pyInterp = null;
		}
	}

	public List<MemberInfo> searchMembers(String search) {
		if(pyInterp == null) {
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
