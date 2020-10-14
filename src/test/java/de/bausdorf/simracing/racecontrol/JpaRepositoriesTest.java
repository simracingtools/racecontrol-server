package de.bausdorf.simracing.racecontrol;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.DURATION;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.bausdorf.simracing.racecontrol.model.Driver;
import de.bausdorf.simracing.racecontrol.model.DriverChange;
import de.bausdorf.simracing.racecontrol.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.model.DriverRepository;
import de.bausdorf.simracing.racecontrol.model.IRacingPk;
import de.bausdorf.simracing.racecontrol.model.Session;
import de.bausdorf.simracing.racecontrol.model.SessionRepository;
import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.model.TeamRepository;

@SpringBootTest
class JpaRepositoriesTest {

	private static final String SESSION_ID = "FBP Racing #BLUE@123456#654321#2";

	@Autowired
	DriverRepository driverRepository;

	@Autowired
	TeamRepository teamRepository;

	@Autowired
	DriverChangeRepository changeRepository;

	@Autowired
	SessionRepository sessionRepository;

	Team team;
	Driver driver;

	@BeforeEach
	public void setUp() {
		driver = Driver.builder()
				.sessionId(SESSION_ID)
				.driverId(4711)
				.name("Zaphod Beeblebrox")
				.iRating(666)
				.team(team = Team.builder()
						.sessionId(SESSION_ID)
						.teamId(8150)
						.carNo(999)
						.name("FBP Racing #BLUE")
						.build())
				.build();
	}

	@AfterEach
	public void tearDown() {
		changeRepository.findBySessionIdAndTeamOrderByChangeTimeAsc(SESSION_ID, team).stream()
				.forEach(s -> changeRepository.delete(s));

		driverRepository.delete(driver);
	}

	@Test
	void testCrudTeam() {
		teamRepository.save(team);

		Optional<Team> team1 = teamRepository.findBySessionIdAndIracingId(SESSION_ID, 8150);
		assertThat(team1).isPresent();

		Team team2 = getTeam(8150);
		assertThat(team2).isNotNull();
	}

	@Test
	void testCrudDriver() {
		driverRepository.save(driver);

		Optional<Driver> driver1 = driverRepository.findBySessionIdAndIracingId(SESSION_ID, 4711);
		assertThat(driver1).isPresent();

		driver1.get().getTeam().setCurrentDriverId(driver1.get().getIracingId());
		driverRepository.save(driver1.get());

		Team team1 = getTeam(8510);
//		Team team1 = teamRepository.findBySessionIdAndIracingId(SESSION_ID, 8150).orElse(null);
		assertThat(team1).isNotNull();
		assertThat(team1.getCurrentDriverId()).isNotZero();
		assertThat(team1.getCurrentDriverId()).isEqualTo(4711);

	}

	@Test
	void testCrudMultipleTeamDrivers() {
		driverRepository.save(driver);

		Optional<Driver> driver1 = driverRepository.findBySessionIdAndIracingId(SESSION_ID, 4711);
		assertThat(driver1).isPresent();

		Team team = getTeam(8510);

		Driver driver2 = Driver.builder()
				.sessionId(SESSION_ID)
				.driverId(8086)
				.name("Luke Skywalker")
				.team(team)
				.iRating(999)
				.build();
		driverRepository.save(driver2);

		Team team1 = getTeam(8150);
		assertThat(team1).isNotNull();
		assertThat(team1.getDrivers().size()).isEqualTo(2);

		driverRepository.delete(driver1.get());

		team1 = getTeam(8150);
		assertThat(team1).isNotNull();
		assertThat(team1.getDrivers().size()).isEqualTo(1);
	}

	@Test
	void testCrudDriverStints() {
		driverRepository.save(driver);

		Optional<Driver> driver1 = driverRepository.findBySessionIdAndIracingId(SESSION_ID, 4711);
		assertThat(driver1).isPresent();

		driver1.get().getStints().add(Stint.builder()
				.sessionId(SESSION_ID)
				.driver(driver1.get())
				.startTime(Duration.ZERO.plusMinutes(2))
				.endTime(Duration.ZERO.plusMinutes(55))
				.build());

		driverRepository.save(driver1.get());

		driver1 = driverRepository.findBySessionIdAndIracingId(SESSION_ID, 4711);
		assertThat(driver1).isPresent();
		assertThat(driver1.get().getStints()).isNotEmpty();

		driver1.get().getStints().add(Stint.builder()
				.sessionId(SESSION_ID)
				.driver(driver1.get())
				.startTime(Duration.ZERO.plusMinutes(119))
				.endTime(Duration.ZERO.plusMinutes(201))
				.build());

		driverRepository.save(driver1.get());

		driver1 = driverRepository.findBySessionIdAndIracingId(SESSION_ID, 4711);
		assertThat(driver1).isPresent();
		assertThat(driver1.get().getStints()).hasSize(2);
	}

	@Test
	void testCrudDriverChange() {
		driverRepository.save(driver);
		Driver driver2 = Driver.builder()
				.sessionId(SESSION_ID)
				.driverId(8086)
				.name("Luke Skywalker")
				.team(team)
				.iRating(999)
				.build();
		driverRepository.save(driver2);

		DriverChange change = DriverChange.builder()
				.sessionId(SESSION_ID)
				.team(team)
				.changeFromId(driver.getIracingId())
				.changeToId(driver2.getIracingId())
				.changeTime(Duration.ofMinutes(55))
				.build();
		changeRepository.save(change);

		List<DriverChange> changes = changeRepository.findBySessionIdAndTeamOrderByChangeTimeAsc(SESSION_ID, team);
		assertThat(changes).isNotEmpty();

		Team team1 = getTeam(8150);
		assertThat(team1).isNotNull();

		DriverChange change1 = DriverChange.builder()
				.sessionId(SESSION_ID)
				.team(team1)
				.changeFromId(driver2.getIracingId())
				.changeToId(driver.getIracingId())
				.changeTime(Duration.ofMinutes(108))
				.build();
		changeRepository.save(change1);

		List<DriverChange> changes1 = changeRepository.findBySessionIdAndTeamOrderByChangeTimeAsc(SESSION_ID, team1);
		assertThat(changes1).hasSize(2);
	}

	@Test
	void testCrudSessionRepository() {
		LocalDateTime created = LocalDateTime.now();
		sessionRepository.save(Session.builder()
				.sessionId(SESSION_ID)
				.lastUpdate(Duration.ZERO)
				.sessionDuration(Duration.ofHours(24))
				.trackName("NOS")
				.build());

		Session session = sessionRepository.findBySessionId(SESSION_ID).orElse(null);
		assertThat(session).isNotNull();

		session.setLastUpdate(Duration.ofSeconds(1));
		sessionRepository.save(session);

		Session session1 = sessionRepository.findBySessionId(SESSION_ID).orElse(null);
		assertThat(session1).isNotNull();
		assertThat(session1.getLastUpdate()).isPositive();

		sessionRepository.delete(session1);

		Session session2 = sessionRepository.findBySessionId(SESSION_ID).orElse(null);
		assertThat(session2).isNull();
	}
	Team getTeam(long teamId) {
//		return teamRepository.findBySessionIdAndIracingId(SESSION_ID, teamId).orElse(null);
		return teamRepository.findBySessionIdAndIracingId(SESSION_ID, 8150).orElse(null);

	}
}
