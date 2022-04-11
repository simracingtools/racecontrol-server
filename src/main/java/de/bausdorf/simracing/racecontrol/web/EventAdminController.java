package de.bausdorf.simracing.racecontrol.web;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2022 bausdorf engineering
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

import de.bausdorf.simracing.irdataapi.model.CarInfoDto;
import de.bausdorf.simracing.irdataapi.tools.CarCategoryEnum;
import de.bausdorf.simracing.irdataapi.tools.StockDataTools;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.model.EditCarClassView;
import de.bausdorf.simracing.racecontrol.web.model.CarView;
import de.bausdorf.simracing.racecontrol.web.model.CreateEventView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class EventAdminController extends ControllerBase {
    public static final String CREATE_EVENT_VIEW = "create-event";
    public static final String EVENT_VIEW_MODEL_KEY = "eventView";

    private final EventSeriesRepository eventRepository;
    private final CarClassRepository carClassRepository;
    private final BalancedCarRepository balancedCarRepository;
    private final IRacingClient iRacingClient;

    public EventAdminController(@Autowired EventSeriesRepository eventRepository,
                                @Autowired CarClassRepository carClassRepository,
                                @Autowired BalancedCarRepository balancedCarRepository,
                                @Autowired IRacingClient iRacingClient) {
        this.eventRepository = eventRepository;
        this.carClassRepository = carClassRepository;
        this.balancedCarRepository = balancedCarRepository;
        this.iRacingClient = iRacingClient;
    }

    @GetMapping("/create-event")
    public String createEvent(@RequestParam Optional<Long> eventId, @RequestParam Optional<String> error, Model model) {
        error.ifPresent(e -> addError(e, model));

        if (eventId.isPresent()) {
            Optional<EventSeries> eventSeries = eventRepository.findById(eventId.get());
            if(eventSeries.isPresent()) {
                model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.fromEntity(eventSeries.get()));
            } else {
                addWarning("Event with id " + eventId.get() + " not found", model);
                model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.createEmpty());
            }
        } else {
            model.addAttribute(EVENT_VIEW_MODEL_KEY, CreateEventView.createEmpty());
        }
        model.addAttribute("editCarClassView", EditCarClassView.builder()
                .eventId(eventId.orElse(0L))
                .build());

        return CREATE_EVENT_VIEW;
    }

    @PostMapping("/create-event")
    @Transactional
    public String createEvent(@ModelAttribute CreateEventView eventView) {
        String error = null;
        EventSeries eventSeriesToSave = null;
        if (eventView.getEventId() != 0) {
            Optional<EventSeries> eventSeries = eventRepository.findById(eventView.getEventId());
            if (eventSeries.isEmpty()) {
                error = "Event series with id " + eventView.getEventId() + " does not exist";
            } else {
                eventSeriesToSave = eventSeries.get();
            }
        }
        eventSeriesToSave = eventRepository.save(eventView.toEntity(eventSeriesToSave));
        return redirectView(CREATE_EVENT_VIEW, eventSeriesToSave.getId(), error);
    }

    @PostMapping("event-save-carclass")
    @Transactional
    public String addCarClass(@ModelAttribute EditCarClassView carClassView) {
        AtomicReference<String> error = new AtomicReference<>();
        if(carClassView.getId() != 0) {
            CarClass carClass = carClassRepository.findById(carClassView.getId()).orElse(null);
            if(carClass != null) {
                balancedCarRepository.deleteAllByCarClassId(carClass.getId());
                carClassRepository.save(updateCarData(carClassView.toEntity(carClass)));
            } else {
                error.set("Car class with id " + carClassView.getId() + " not found");
            }
        } else {
            Optional<EventSeries> eventSeriesOptional = eventRepository.findById(carClassView.getEventId());
            eventSeriesOptional.ifPresentOrElse(eventSeries -> {
                        CarClass carClass = carClassView.toEntity(null);
                        CarClass finalCarClass = updateCarData(carClassRepository.save(carClass));
                        eventSeries.getCarClassPreset().add(finalCarClass);
                        eventRepository.save(eventSeries);
                    },
                    () -> error.set("No event series with id " + carClassView.getEventId() + " found."));
        }
        return redirectView(CREATE_EVENT_VIEW, carClassView.getEventId(), error.get());
    }

    @GetMapping("remove-car-class")
    @Transactional
    public String removeCarClass(@RequestParam long classId) {
        Optional<CarClass> carClass = carClassRepository.findById(classId);
        AtomicLong eventId = new AtomicLong();
        AtomicReference<String> error = new AtomicReference<>(null);
        carClass.ifPresentOrElse(
                cc -> {
                    eventId.set(cc.getEventId());
                    carClassRepository.delete(cc);
                },
                () -> error.set("No car class found for id " + classId)
        );
        return redirectView(CREATE_EVENT_VIEW, eventId.get(), error.get());
    }

    @ModelAttribute(name="allCars")
    public List<CarView> allCars() {
        return StockDataTools.carsByCategory(iRacingClient.getDataCache().getCars(), CarCategoryEnum.ROAD, false).stream()
                .filter(car -> Arrays.stream(car.getCarTypes()).anyMatch(type -> type.getCarType().equalsIgnoreCase("road")))
                .map(car -> CarView.builder()
                        .carId(car.getCarId())
                        .name(car.getCarName())
                        .build())
                .sorted(Comparator.comparing(CarView::getName))
                .collect(Collectors.toList());
    }

    private String redirectView(String viewName, long eventId, String error) {
        return "redirect:/" + viewName
                + (eventId != 0 ? "?eventId=" + eventId : "")
                + (error != null ? "&error=" + error : "");
    }

    private CarClass updateCarData(CarClass carClass) {
        carClass.getCars().forEach(bc -> {
            Optional<CarInfoDto> carInfo = Arrays.stream(iRacingClient.getDataCache().getCars())
                    .filter(c -> c.getCarId().equals(bc.getCarId()))
                    .findFirst();
            carInfo.ifPresent(ci -> {
                bc.setCarName(ci.getCarName());
                bc.setCarClassId(carClass.getId());
            });
        });
        return carClass;
    }
}
