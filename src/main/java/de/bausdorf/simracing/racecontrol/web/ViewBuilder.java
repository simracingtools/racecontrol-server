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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import de.bausdorf.simracing.racecontrol.api.EventType;
import de.bausdorf.simracing.racecontrol.api.SessionStateType;
import de.bausdorf.simracing.racecontrol.api.StintStateType;
import de.bausdorf.simracing.racecontrol.model.Driver;
import de.bausdorf.simracing.racecontrol.model.DriverChange;
import de.bausdorf.simracing.racecontrol.model.DriverChangeRepository;
import de.bausdorf.simracing.racecontrol.model.Event;
import de.bausdorf.simracing.racecontrol.model.EventRepository;
import de.bausdorf.simracing.racecontrol.model.Session;
import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.util.RuleComplianceCheck;
import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import de.bausdorf.simracing.racecontrol.web.model.CssClassType;
import de.bausdorf.simracing.racecontrol.web.model.DriverView;
import de.bausdorf.simracing.racecontrol.web.model.EventView;
import de.bausdorf.simracing.racecontrol.web.model.SessionView;
import de.bausdorf.simracing.racecontrol.web.model.StintView;
import de.bausdorf.simracing.racecontrol.web.model.TableCellView;
import de.bausdorf.simracing.racecontrol.web.model.TeamDetailView;
import de.bausdorf.simracing.racecontrol.web.model.TeamView;
import de.bausdorf.simracing.racecontrol.web.model.TrackTimeView;
import de.bausdorf.simracing.racecontrol.web.security.RcUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ViewBuilder {

	private final RuleComplianceCheck complianceCheck;
	private final DriverChangeRepository changeRepository;
	private final EventRepository eventRepository;

	public ViewBuilder(@Autowired RuleComplianceCheck complianceCheck,
			@Autowired DriverChangeRepository driverChangeRepository,
			@Autowired EventRepository eventRepository) {
		this.changeRepository = driverChangeRepository;
		this.complianceCheck = complianceCheck;
		this.eventRepository = eventRepository;
	}

	public SessionView buildSessionView(Session selectedSession, @Nullable List<Team> teamsInSession) {
		SessionView sessionView = SessionView.builder()
				.sessionId(selectedSession.getSessionId())
				.sessionDuration(TableCellView.builder()
						.value(TimeTools.shortDurationString(selectedSession.getSessionDuration()))
						.displayType(CssClassType.DEFAULT)
						.build())
				.sessionType(TableCellView.builder()
						.value(selectedSession.getSessionType())
						.displayType(selectedSession.getSessionType().equalsIgnoreCase("RACE")
								? CssClassType.TBL_SUCCESS : CssClassType.TBL_WARNING)
						.build())
				.trackName(TableCellView.builder()
						.value(selectedSession.getTrackName())
						.displayType(CssClassType.DEFAULT)
						.build())
				.sessionState(TableCellView.builder()
						.value(selectedSession.getSessionState().name())
						.displayType(cssTypeForSessionState(selectedSession.getSessionState()))
						.build())
				.build();
		if(teamsInSession != null) {
			sessionView.setTeams(teamsInSession.stream()
					.map(this::buildFromTeam)
					.collect(Collectors.toList()));
		}
		return sessionView;
	}

	public TeamView buildFromTeam(Team team) {
		TeamView teamView = TeamView.builder()
				.name(TableCellView.builder()
						.value(team.getName())
						.displayType(CssClassType.DEFAULT)
						.build())
				.carNo(TableCellView.builder()
						.value(String.valueOf(team.getCarNo()))
						.displayType(CssClassType.DEFAULT)
						.build())
				.carName(team.getCarName())
				.carClass(team.getCarClass())
				.carClassColor(team.getCarClassColor())
				.teamId(team.getTeamId())
				.drivers(team.getDrivers().stream()
						.map(this::buildDriverView)
						.collect(Collectors.toList()))
				.build();

		// Calculate fair share ruling
		int maxDrivingMillis = teamView.getDrivers().stream()
				.mapToInt(DriverView::getDrivingMillis)
				.max().orElse(0);
		for(DriverView driverView : teamView.getDrivers()) {
			driverView.getDrivingTime().setDisplayType(
					complianceCheck.isFairShareCompliant(maxDrivingMillis, driverView.getDrivingMillis())
					? CssClassType.TBL_SUCCESS : CssClassType.TBL_DANGER);
		}

		// Calculate PRO/AM ruling
		OptionalDouble avgTeamRating = team.getDrivers().stream().mapToLong(Driver::getIRating).average();
		if(avgTeamRating.isPresent()) {
			teamView.setAvgTeamRating(TableCellView.builder()
					.displayType(
							complianceCheck.isProTeam(avgTeamRating.getAsDouble()) ? CssClassType.TBL_DARK : CssClassType.TBL_SECONDARY)
					.value(String.format("%.2f", avgTeamRating.getAsDouble()))
					.build());
			teamView.setProTeam(complianceCheck.isProTeam(avgTeamRating.getAsDouble()));
		}
		return teamView;
	}

	public TeamDetailView buildFromTeamView(TeamView teamView, String sessionId, RcUser user) {
		TeamDetailView detailView = TeamDetailView.builder()
				.avgTeamRating(teamView.getAvgTeamRating())
				.carNo(teamView.getCarNo())
				.carName(teamView.getCarName())
				.carClass(teamView.getCarClass())
				.carClassColor(teamView.getCarClassColor())
				.teamId(teamView.getTeamId())
				.name(teamView.getName())
				.build();

		List<StintView> changes = new ArrayList<>();
		for(DriverView driver : teamView.getDrivers()) {
			changes.addAll(driver.getStints());
		}
		detailView.setStints(changes.stream()
				.sorted(Comparator.comparing(StintView::getChangeTime))
				.collect(Collectors.toList()));
		detailView.setEvents(buildFromEventList(eventRepository
				.findBySessionIdAndTeamIdOrderBySessionTimeDesc(sessionId, teamView.getTeamId()), user));
		return detailView;
	}

	public List<EventView> buildFromEventList(List<Event> events, RcUser user) {
		return events.stream()
				.filter(s -> user.getEventFilter().contains(EventType.valueOf(s.getEventType())))
				.map(this::buildEventView)
				.collect(Collectors.toList());
	}

	public EventView buildEventView(Event event) {
		return EventView.builder()
				.driverId(event.getDriverId())
				.teamId(event.getTeamId())
				.sessionTime(event.getSessionTime())
				.sessionMillis(event.getSessionTime().toMillis())
				.carNo(TableCellView.builder()
						.value(event.getCarNo())
						.displayType(CssClassType.DEFAULT)
						.build())
				.carName(TableCellView.builder()
						.value(event.getCarName())
						.displayType(CssClassType.DEFAULT)
						.build())
				.driverName(TableCellView.builder()
						.value(event.getDriverName())
						.displayType(CssClassType.DEFAULT)
						.build())
				.eventTime(TableCellView.builder()
						.value(event.getEventTime())
						.displayType(CssClassType.TBL_SECONDARY)
						.build())
				.eventType(TableCellView.builder()
						.value(event.getEventType())
						.displayType(cssTypeForEventType(EventType.valueOf(event.getEventType())))
						.build())
				.teamName(TableCellView.builder()
						.value(event.getTeamName())
						.displayType(CssClassType.DEFAULT)
						.build())
				.lap(TableCellView.builder()
						.value(String.valueOf(event.getLap()))
						.displayType(CssClassType.DEFAULT)
						.build())
				.build();
	}

	public DriverView buildDriverView(Driver driver) {
		List<StintView> stintViews = buildStintViews(driver);
		Duration trackTime = Duration.ZERO;
		for(StintView s : stintViews) {
			trackTime = trackTime.plus(s.getTrackTime());
		}
		return DriverView.builder()
						.name(TableCellView.builder()
								.value(driver.getName())
								.displayType(classTypeForTrackState(driver.getLastEventType()))
								.build())
						.iRacingId(String.valueOf(driver.getIracingId()))
						.iRating(String.valueOf(driver.getIRating()))
						.stints(stintViews)
						.drivingTime(TableCellView.builder()
								.value(TimeTools.shortDurationString(trackTime))
								.displayType(CssClassType.TBL_SUCCESS)
								.build())
						.drivingMillis((int)trackTime.toMillis())
						.build();
	}

	public List<StintView> buildStintViews(Driver driver) {
		List<StintView> stintViews = new ArrayList<>();

		List<DriverChange> changes = changeRepository
				.findBySessionIdAndTeamOrderByChangeTimeAsc(driver.getSessionId(), driver.getTeam()).stream()
				.filter(s -> s.getChangeFromId() == driver.getIracingId() || s.getChangeToId() == driver.getIracingId())
				.collect(Collectors.toList());

		StintViewData currentStintView = null;
		StintViewData lastStintView = null;
		if(changes.isEmpty()) {
			// No driver change yet or single driver
			currentStintView = new StintViewData(Duration.ZERO, Duration.ofHours(25), Duration.ZERO,
					true, false, new ArrayList<>());
		}
		for(DriverChange change : changes) {
			if(currentStintView == null) {
				if(change.getChangeToId() != driver.getIracingId()) {
					// first stint
					currentStintView = new StintViewData(Duration.ZERO, change.getChangeTime(), Duration.ZERO,
							true, false, new ArrayList<>());
					currentStintView.calculateTrackTime(driver);
					stintViews.add(buildStintView(currentStintView));
					lastStintView = currentStintView;
					currentStintView = null;
				} else {
					currentStintView = new StintViewData(change.getChangeTime(), Duration.ZERO, Duration.ZERO,
							true, false, 	new ArrayList<>());
				}
			} else {
				if(change.getChangeFromId() == driver.getIracingId()) {
					currentStintView.setStopTime(change.getChangeTime());
					currentStintView.calculateTrackTime(driver);
					if(!currentStintView.isUnfinished()) {
						if (lastStintView != null) {
							currentStintView.setStartTimeCompliant(complianceCheck.isRestTimeCompliant(
									lastStintView.getTrackTime(), lastStintView.getStopTime(), currentStintView.getStartTime()));
						}
						stintViews.add(buildStintView(currentStintView));
					}
					lastStintView = currentStintView;
					currentStintView = null;
				}
			}
		}
		if(currentStintView != null) {
			// last stint
			currentStintView.setStopTime(driver.getLastStint().getEndTime());
			currentStintView.calculateTrackTime(driver);
			stintViews.add(buildStintView(currentStintView));
		}

		return stintViews;
	}

	private StintView buildStintView(StintViewData data) {
		return StintView.builder()
				.startTime(TableCellView.builder()
						.value(TimeTools.shortDurationString(data.getStartTime()))
						.displayType(data.isStartTimeCompliant() ? CssClassType.TBL_PRIMARY : CssClassType.TBL_DANGER)
						.build())
				.stopTime(TableCellView.builder()
						.value(data.isUnfinished() ? "- DNF -" : TimeTools.shortDurationString(data.getStopTime()))
						.displayType(data.isUnfinished() ? CssClassType.TBL_DARK : CssClassType.TBL_INFO)
						.build())
				.duration(TableCellView.builder()
						.value(TimeTools.shortDurationString(data.getTrackTime()))
						.displayType(complianceCheck.isStintDurationCompliant(data.getTrackTime(), data.startTime.equals(Duration.ZERO)) ? CssClassType.TBL_SUCCESS : CssClassType.TBL_DANGER)
						.build())
				.changeTime(data.isUnfinished() ? data.getStartTime() : data.getStopTime())
				.changeTimeStr(data.isUnfinished() ? TimeTools.shortDurationString(data.getStartTime()) : TimeTools.shortDurationString(data.getStopTime()))
				.trackTime(data.getTrackTime())
				.trackTimes(data.getTrackTimeViews())
				.build();
	}

	private CssClassType classTypeForTrackState(EventType trackStateType) {
		if(trackStateType == null) {
			return CssClassType.DEFAULT;
		}
		switch(trackStateType) {
			case ON_TRACK: return CssClassType.TBL_SUCCESS;
			case OFF_TRACK: return CssClassType.TBL_WARNING;
			case APPROACHING_PITS:
			case ENTER_PITLANE:
			case EXIT_PITLANE: return CssClassType.TBL_INFO;
			case DRIVER_CHANGE:
			case IN_PIT_STALL: return CssClassType.TBL_PRIMARY;
			default: return CssClassType.DEFAULT;
		}
	}

	@Data
	@AllArgsConstructor
	@ToString
	class StintViewData {
		private Duration startTime;
		private Duration stopTime;
		private Duration trackTime;
		private boolean startTimeCompliant;
		private boolean unfinished;
		private List<TrackTimeView> trackTimeViews;

		public void calculateTrackTime(Driver driver) {
			List<Stint> stintsBetween = new ArrayList<>();
			for(Stint s : driver.getStints()) {
				if(startTime == null || stopTime == null || !validateStint(s, driver)) {
					log.debug("Null times in StintView: {}, {}", startTime, stopTime);
					continue;
				}
				try {
					if (s.getStartTime().compareTo(startTime) >= 0 && s.getEndTime().compareTo(stopTime) <= 0) {
						stintsBetween.add(s);
					}
				} catch(Exception e) {
					log.error("StintViewData: {}, Stint: {}", this, s);
					throw e;
				}
			}
			if (stintsBetween.isEmpty()) {
				log.info("No stints for {} ({}) between {} and {}", driver.getName(), driver.getIracingId(), startTime, stopTime);
				unfinished = true;
			} else {
				stintsBetween.forEach(s -> {
					addTrackTime(s.getStintDuration());
					trackTimeViews.add(TrackTimeView.builder()
							.startTime(TableCellView.builder()
									.value(TimeTools.shortDurationString(s.getStartTime()))
									.displayType(CssClassType.DEFAULT)
									.build())
							.stopTime(TableCellView.builder()
									.value(TimeTools.shortDurationString(s.getEndTime()))
									.displayType(CssClassType.DEFAULT)
									.build())
							.duration(TableCellView.builder()
									.value(TimeTools.shortDurationString(s.getStintDuration()))
									.displayType(CssClassType.DEFAULT)
									.build())
							.driver(TableCellView.builder()
									.value(driver.getName())
									.displayType(CssClassType.DEFAULT)
									.build())
							.build());
				});
			}
		}

		public void addTrackTime(Duration timeSlice) {
			trackTime = trackTime.plus(timeSlice);
		}

		private boolean validateStint(Stint s, Driver driver) {
			if(s.getState() == StintStateType.UNDEFINED) {
				log.warn("Undefined stint state for {}: {}", driver.getName(), s);
				return false;
			}
			if(s.getStartTime() == null) {
				log.warn("Null start time in stint for {}: {}", driver.getName(), s);
				return false;
			}
			if(s.getEndTime() == null) {
				log.debug("Unfinished stint for {}: {}", driver.getName(), s);
				return false;
			}

			return true;
		}
	}

	private CssClassType cssTypeForSessionState(SessionStateType sessionState) {
		if(sessionState == null) {
			return CssClassType.DEFAULT;
		}
		switch(sessionState) {
			case GRIDING: return CssClassType.TBL_PRIMARY;
			case WARMUP:
			case PARADE_LAPS: return CssClassType.TBL_INFO;
			case RACING: return CssClassType.TBL_SUCCESS;
			case CHECKERED:
			case COOL_DOWN: return CssClassType.TBL_DARK;
			default: return CssClassType.DEFAULT;
		}
	}

	private CssClassType cssTypeForEventType(EventType eventType) {
		if(eventType == null) {
			return CssClassType.DEFAULT;
		}
		switch(eventType) {
			case DRIVER_CHANGE: return CssClassType.TBL_DARK;
			case ENTER_PITLANE:
			case EXIT_PITLANE: return CssClassType.TBL_PRIMARY;
			case IN_PIT_STALL: return CssClassType.TBL_INFO;
			case APPROACHING_PITS:
			case OFF_TRACK: return CssClassType.TBL_WARNING;
			case OFF_WORLD: return CssClassType.TBL_DANGER;
			case ON_TRACK: return CssClassType.TBL_SUCCESS;
			default: return CssClassType.DEFAULT;

		}
	}
}
