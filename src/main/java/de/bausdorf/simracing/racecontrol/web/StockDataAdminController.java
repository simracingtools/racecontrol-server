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

import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class StockDataAdminController extends ControllerBase {
	public static final String STOCKDATA_ADMIN_VIEW = "stockdataadmin";
	public static final String CONFIGURATION_VIEWS = "configurationViews";
	public static final String SELECTED_CONFIGURATION = "selectedConfiguration";
	public static final String SELECTED_TRACK = "selectedTrack";
	public static final String TRACK_VIEWS = "trackViews";
	public static final String SELECTED_CAR = "selectedCar";
	public static final String CAR_VIEWS = "carViews";

	private final CarRepository carRepository;
	private final TrackRepository trackRepository;
	private final TrackConfigRepository trackConfigRepository;

	public StockDataAdminController(@Autowired CarRepository carRepository,
                                    @Autowired TrackRepository trackRepository,
                                    @Autowired TrackConfigRepository trackConfigRepository) {
		this.carRepository = carRepository;
		this.trackRepository = trackRepository;
		this.trackConfigRepository = trackConfigRepository;
	}

	@GetMapping("/stockdata")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String getPenaltiesView(@RequestParam Optional<String> activeTab,
			@RequestParam Optional<Long> selectedCarId,
			@RequestParam Optional<Long> selectedTrackId,
			@RequestParam Optional<Long> selectedTrackConfigId,
			@RequestParam Optional<String> error,
			@RequestParam Optional<String> warn,
			@RequestParam Optional<String> info,
			Model model ) {

		prepareMessageModel(error, warn, info, model);

		if(activeTab.isPresent()) {
			model.addAttribute("activeTab", activeTab.get());
		} else {
			model.addAttribute("activeTab", "cars");
		}

		prepareCarTabModel(model, selectedCarId);
		prepareTrackTabModel(model, selectedTrackId);
		prepareConfigTabModel(model, selectedTrackConfigId, selectedTrackId);

		return STOCKDATA_ADMIN_VIEW;
	}

	@PostMapping("/savecar")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String saveCar(@ModelAttribute CarView selectedCar) {
		Optional<Car> carToSave = carRepository.findById(selectedCar.getCarId());
		if(carToSave.isPresent()) {
			selectedCar.updateEntity(carToSave.get());
		} else {
			Car newCar = Car.builder()
					.carId(selectedCar.getCarId())
					.name(selectedCar.getName())
					.brand(selectedCar.getBrand())
					.maxFuel(selectedCar.getMaxFuel())
					.build();
			carRepository.save(newCar);
		}
		return "redirect:/stockdata?activeTab=cars";
	}

	@PostMapping("/savetrack")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String saveTrack(@ModelAttribute TrackView selectedTrack) {
		Optional<Track> track = trackRepository.findById(selectedTrack.getPkgId());
		if(track.isPresent()) {
			selectedTrack.updateEntity(track.get());
		} else {
			Track newTrack = Track.builder()
					.pkgId(selectedTrack.getPkgId())
					.name(selectedTrack.getName())
					.build();
			trackRepository.save(newTrack);
		}
		return "redirect:/stockdata?activeTab=tracks";
	}

	@PostMapping("/savetrackconfig")
	@Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR", "ROLE_STEWARD"})
	@Transactional
	public String saveTrackConfig(@ModelAttribute TrackConfigurationView selectedConfig, Model model) {
		Optional<TrackConfiguration> trackConfiguration = trackConfigRepository.findById(selectedConfig.getTrackId());
		Track track = trackRepository.findById(selectedConfig.getTrackPkgId()).orElse(null);
		if(trackConfiguration.isPresent()) {
			if(track != null) {
				trackConfiguration.get().setTrackId(selectedConfig.getTrackId());
				trackConfiguration.get().setConfigName(selectedConfig.getConfigName());
				trackConfiguration.get().setTrack(track);
				addInfo("TrackConfiguration changed", model);
			} else {
				log.warn("Track pkgId {} not found", selectedConfig.getTrackPkgId());
				addWarning("Track " + selectedConfig.getTrackPkgId() + " not found", model);

			}
		} else {
			if(track != null) {
				TrackConfiguration trackEntity = TrackConfiguration.builder()
						.track(track)
						.trackId(selectedConfig.getTrackId())
						.configName(selectedConfig.getConfigName())
						.build();
				trackConfigRepository.save(trackEntity);
				addInfo("New track configuration saved", model);
			} else {
				log.warn("Track pkgId {} not found", selectedConfig.getTrack().getPkgId());
				addWarning("Configuration " + selectedConfig.getTrackPkgId() + " not found", model);
			}
		}

		return "redirect:/stockdata?activeTab=configs";
	}

	private List<CarView> getCarList() {
		return carRepository.findAllByNameNotContainingOrderByNameAsc("[Legacy]").stream()
				.map(CarView::buildFromEntity)
				.collect(Collectors.toList());
	}

	private List<TrackView> getTrackList() {
		return trackRepository.findAllByNameNotContainingOrderByNameAsc("[Legacy]").stream()
				.map(TrackView::buildFromEntity)
				.collect(Collectors.toList());
	}

	private void prepareCarTabModel(Model model, Optional<Long> carId) {
		model.addAttribute(CAR_VIEWS, getCarList());
		if(carId.isPresent()) {
			model.addAttribute(SELECTED_CAR,
					CarView.buildFromEntity(carRepository.findById(carId.get()).orElse(null)));
		} else {
			model.addAttribute(SELECTED_CAR,
					CarView.buildEmpty());
		}
	}

	private void prepareTrackTabModel(Model model, Optional<Long> selectedTrackId) {
		model.addAttribute(TRACK_VIEWS, getTrackList());
		if(selectedTrackId.isPresent()) {
			model.addAttribute(SELECTED_TRACK,
					TrackView.buildFromEntity(
							trackRepository.findById(selectedTrackId.get()).orElse(null)
					)
			);
		} else {
			model.addAttribute(SELECTED_TRACK, TrackView.buildEmpty());
		}
	}

	private void prepareConfigTabModel(Model model, Optional<Long> selectedConfigId, Optional<Long> selectedTrackId) {
		if(selectedConfigId.isPresent()) {
			TrackConfiguration trackConfiguration = trackConfigRepository.findById(selectedConfigId.get()).orElse(null);
			if(trackConfiguration != null) {
				model.addAttribute(CONFIGURATION_VIEWS, getConfigurationList(trackConfiguration.getTrack()));
				model.addAttribute(SELECTED_CONFIGURATION,
						TrackConfigurationView.buildFromEntity(
								trackConfiguration
						)
				);
			} else if(selectedTrackId.isPresent()) {
				Track track = trackRepository.findById(selectedTrackId.get()).orElse(null);
				model.addAttribute(CONFIGURATION_VIEWS, getConfigurationList(track));
				model.addAttribute(SELECTED_CONFIGURATION, TrackConfigurationView.buildEmpty());
			} else {
				addWarning("selected track configuration " + selectedConfigId.get() + " not found", model);
				model.addAttribute(CONFIGURATION_VIEWS, new ArrayList<TrackConfigurationView>());
				model.addAttribute(SELECTED_CONFIGURATION, TrackConfigurationView.buildEmpty());
			}
		} else {
			model.addAttribute(CONFIGURATION_VIEWS, new ArrayList<TrackConfigurationView>());
			model.addAttribute(SELECTED_CONFIGURATION, TrackConfigurationView.buildEmpty());
		}
	}

	private List<TrackConfigurationView> getConfigurationList(Track track) {
		return track != null
				? trackConfigRepository.findAllByTrack(track).stream()
						.map(TrackConfigurationView::buildFromEntity)
						.collect(Collectors.toList())
				: new ArrayList<>();

	}
}
