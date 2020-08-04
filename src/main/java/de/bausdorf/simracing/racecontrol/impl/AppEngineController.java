package de.bausdorf.simracing.racecontrol.impl;

/*-
 * #%L
 * tt-cloud-server
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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class AppEngineController {

	@GetMapping("/_ah/stop")
	public ResponseEntity<String> appEngineStopRequest() {
		log.warn("AppEngine send stop request");
		return new ResponseEntity<>("OK", HttpStatus.OK);
	}

	@GetMapping("/_ah/start")
	public ResponseEntity<String> appEngineStartRequest() {
		return new ResponseEntity<>("OK", HttpStatus.OK);
	}

	@GetMapping("/_ah/health")
	public ResponseEntity<String> healthCheck() {
		return new ResponseEntity<>("Healthy", HttpStatus.OK);
	}
}
