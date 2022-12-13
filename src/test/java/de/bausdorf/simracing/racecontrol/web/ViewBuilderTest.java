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

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.bausdorf.simracing.racecontrol.live.model.Team;
import de.bausdorf.simracing.racecontrol.live.model.TeamRepository;

@SpringBootTest
//@Testcontainers
@ActiveProfiles("test")
class ViewBuilderTest {
//	@Container
//	public static MariaDBContainer database = new MariaDBContainer();
//
//	@DynamicPropertySource
//	static void databaseProperties(DynamicPropertyRegistry registry) {
//		registry.add("spring.datasource.url", database::getJdbcUrl);
//		registry.add("spring.datasource.username", database::getUsername);
//		registry.add("spring.datasource.password", database::getPassword);
//	}

	@Autowired
	ViewBuilder viewBuilder;

	@Autowired
	TeamRepository teamRepository;

	@Test
	void testCuriousTeam() {
		Optional<Team> team = teamRepository.findBySessionIdAndIracingId("roadamerica full@139239792#35010657#2",200098);
		team.ifPresent(value -> Assertions.assertNotNull(viewBuilder.buildFromTeam(value, 9001L)));
	}
}
