package de.bausdorf.simracing.racecontrol.impl;

import de.bausdorf.simracing.racecontrol.api.EventType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.bausdorf.simracing.racecontrol.api.ClientMessage;
import de.bausdorf.simracing.racecontrol.model.Driver;
import de.bausdorf.simracing.racecontrol.model.DriverChange;
import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class SessionController {
	private String sessionId;

	private final Map<String, Team> teams;

	public SessionController(ClientMessage message) {
		this.teams = new HashMap<>();
		this.sessionId = message.getSessionId();
		this.processEventMessage(message);
	}

	public void processEventMessage(ClientMessage message) {
		if (!teams.containsKey(message.getTeamId())) {
			Driver driver = createNewDriver(message);
			updateDriverStint(driver, message.getEventType(), message.getSessionTime());
			Team newTeam = Team.builder()
					.id(message.getTeamId())
					.name(message.getTeamName())
					.carNo(message.getCarNo())
					.drivers(new HashMap<>())
					.driverChanges(new ArrayList<>())
					.currentDriver(driver)
					.build();
			newTeam.getDrivers().put(driver.getId(), driver);
			teams.put(newTeam.getId(), newTeam);
		} else {
			Team team = teams.get(message.getTeamId());
			Driver driver;
			if (!team.getDrivers().containsKey(message.getDriverId())) {
				driver = createNewDriver(message);
				team.getDrivers().put(driver.getId(), driver);
			} else {
				driver = team.getDrivers().get(message.getDriverId());
			}
			updateDriverStint(driver, message.getEventType(), message.getSessionTime());

			if(message.getEventType() == EventType.DRIVER_CHANGE) {
				DriverChange change = DriverChange.builder()
						.changeFrom(team.getCurrentDriver())
						.changeTo(driver)
						.changeTime(message.getSessionTime())
						.build();
				log.info("Driver change for team {}; {}", team.getName(), change);
				team.getDriverChanges().add(change);
				team.setCurrentDriver(driver);
			}
		}
	}

	private void updateDriverStint(Driver driver, EventType eventType, Duration sessionTime) {
		Stint stint = driver.getLastStint();
		switch(eventType) {
			case ON_TRACK:
				if(stint.getStartTime() == null) {
					log.debug("{} stating stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
					stint.setStartTime(sessionTime);
				}
				break;
			case ENTER_PITLANE:
				if(stint.getEndTime() == null) {
					log.debug("{} ending stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
					stint.setEndTime(sessionTime);
				}
				break;
			case EXIT_PITLANE:
				log.debug("{} stating stint at {}", driver.getName(), TimeTools.longDurationString(sessionTime));
				if(stint.getStartTime() == null) {
					stint.setStartTime(sessionTime);
				} else {
					driver.getStints().add(Stint.builder()
							.startTime(sessionTime)
							.build());
				}
				break;
			default:
		}

	}

	private Driver createNewDriver(ClientMessage message) {
		return Driver.builder()
				.id(message.getDriverId())
				.name(message.getDriverName())
				.iRating(message.getIRating())
				.stints(new ArrayList<>(Arrays.asList(Stint.builder().build())))
				.build();
	}
}
