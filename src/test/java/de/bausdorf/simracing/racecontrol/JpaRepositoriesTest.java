package de.bausdorf.simracing.racecontrol;

import static org.assertj.core.api.Assertions.assertThat;

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
		changeRepository.deleteAll();
		driverRepository.deleteAll();
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

		driver1.get().getTeam().setCurrentDriver(driver1.get());
		driverRepository.save(driver1.get());

		Team team1 = getTeam(8510);
//		Team team1 = teamRepository.findBySessionIdAndIracingId(SESSION_ID, 8150).orElse(null);
		assertThat(team1).isNotNull();
		assertThat(team1.getCurrentDriver()).isNotNull();
		assertThat(team1.getCurrentDriver().getDriverId()).isEqualTo(4711);

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
				.changeFrom(driver)
				.changeTo(driver2)
				.changeTime(Duration.ofMinutes(55))
				.build();
		changeRepository.save(change);

		List<DriverChange> changes = changeRepository.findBySessionIdAndTeam(SESSION_ID, team);
		assertThat(changes).isNotEmpty();

		Team team1 = getTeam(8150);
		assertThat(team1).isNotNull();

		DriverChange change1 = DriverChange.builder()
				.sessionId(SESSION_ID)
				.team(team1)
				.changeFrom(driver2)
				.changeTo(driver)
				.changeTime(Duration.ofMinutes(108))
				.build();
		changeRepository.save(change1);

		List<DriverChange> changes1 = changeRepository.findBySessionIdAndTeam(SESSION_ID, team1);
		assertThat(changes1).hasSize(2);
	}

	@Test
	void testCrudSessionRepository() {
		LocalDateTime created = LocalDateTime.now();
		sessionRepository.save(Session.builder()
				.sessionId(SESSION_ID)
				.lastUpdate(Timestamp.valueOf(created))
				.sessionDuration(Duration.ofHours(24))
				.startTime(ZonedDateTime.now())
				.trackName("NOS")
				.build());

		Session session = sessionRepository.findBySessionId(SESSION_ID).orElse(null);
		assertThat(session).isNotNull();

		session.setLastUpdate(Timestamp.valueOf(created.plusSeconds(1)));
		sessionRepository.save(session);

		Session session1 = sessionRepository.findBySessionId(SESSION_ID).orElse(null);
		assertThat(session1).isNotNull();
		assertThat(session1.getLastUpdate()).isAfter(Timestamp.valueOf(created));

		sessionRepository.delete(session1);

		Session session2 = sessionRepository.findBySessionId(SESSION_ID).orElse(null);
		assertThat(session2).isNull();
	}
	Team getTeam(long teamId) {
//		return teamRepository.findBySessionIdAndIracingId(SESSION_ID, teamId).orElse(null);
		return teamRepository.findBySessionIdAndIracingId(SESSION_ID, 8150).orElse(null);

	}
}
