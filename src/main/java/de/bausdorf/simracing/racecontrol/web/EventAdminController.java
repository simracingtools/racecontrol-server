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
import de.bausdorf.simracing.irdataapi.tools.CarCategoryType;
import de.bausdorf.simracing.irdataapi.tools.StockDataTools;
import de.bausdorf.simracing.racecontrol.iracing.IRacingClient;
import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.util.FileTypeEnum;
import de.bausdorf.simracing.racecontrol.util.UploadFileManager;
import de.bausdorf.simracing.racecontrol.web.model.orga.EditCarClassView;
import de.bausdorf.simracing.racecontrol.web.model.orga.CarView;
import de.bausdorf.simracing.racecontrol.web.model.orga.CreateEventView;
import de.bausdorf.simracing.racecontrol.web.model.orga.PersonView;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class EventAdminController extends ControllerBase {
    private static final String CREATE_EVENT_VIEW = "create-event";
    private static final String EVENT_VIEW_MODEL_KEY = "eventView";

    private final EventSeriesRepository eventRepository;
    private final CarClassRepository carClassRepository;
    private final BalancedCarRepository balancedCarRepository;
    private final PersonRepository personRepository;
    private final IRacingClient iRacingClient;
    private final UploadFileManager uploadFileManager;

    public EventAdminController(@Autowired EventSeriesRepository eventRepository,
                                @Autowired CarClassRepository carClassRepository,
                                @Autowired BalancedCarRepository balancedCarRepository,
                                @Autowired PersonRepository personRepository,
                                @Autowired UploadFileManager uploadFileManager,
                                @Autowired IRacingClient iRacingClient) {
        this.eventRepository = eventRepository;
        this.carClassRepository = carClassRepository;
        this.balancedCarRepository = balancedCarRepository;
        this.personRepository = personRepository;
        this.uploadFileManager = uploadFileManager;
        this.iRacingClient = iRacingClient;
    }

    @GetMapping("/create-event")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    public String createEvent(@RequestParam Optional<Long> eventId, @RequestParam Optional<String> messages, Model model) {
        messages.ifPresent(e -> decodeMessagesToModel(e, model));

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
        model.addAttribute("editStaffView", PersonView.builder()
                .eventId(eventId.orElse(0L))
                .build());

        return CREATE_EVENT_VIEW;
    }

    @PostMapping("/create-event")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String createEvent(@ModelAttribute CreateEventView eventView, Model model) {
        EventSeries eventSeriesToSave = null;
        if (eventView.getEventId() != 0) {
            Optional<EventSeries> eventSeries = eventRepository.findById(eventView.getEventId());
            if (eventSeries.isEmpty()) {
                addError("Event series with id " + eventView.getEventId() + " does not exist", model);
            } else {
                eventSeriesToSave = eventSeries.get();
            }
        }
        eventSeriesToSave = eventRepository.save(eventView.toEntity(eventSeriesToSave));
        return redirectView(eventSeriesToSave.getId(), messagesEncoded(model));
    }

    @PostMapping("/event-save-carclass")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String addCarClass(@ModelAttribute EditCarClassView carClassView, Model model) {
        if(carClassView.getId() != 0) {
            CarClass carClass = carClassRepository.findById(carClassView.getId()).orElse(null);
            if(carClass != null) {
                balancedCarRepository.deleteAllByCarClassId(carClass.getId());
                carClassRepository.save(updateCarData(carClassView.toEntity(carClass)));
            } else {
                addError("Car class with id " + carClassView.getId() + " not found", model);
            }
        } else {
            Optional<EventSeries> eventSeriesOptional = eventRepository.findById(carClassView.getEventId());
            eventSeriesOptional.ifPresentOrElse(eventSeries -> {
                        CarClass carClass = carClassView.toEntity(null);
                        CarClass finalCarClass = updateCarData(carClassRepository.save(carClass));
                        eventSeries.getCarClassPreset().add(finalCarClass);
                        eventRepository.save(eventSeries);
                    },
                    () -> addError("No event series with id " + carClassView.getEventId() + " found.", model));
        }
        return redirectView(carClassView.getEventId(), messagesEncoded(model));
    }

    @GetMapping("/remove-car-class")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String removeCarClass(@RequestParam long classId, Model model) {
        Optional<CarClass> carClass = carClassRepository.findById(classId);
        AtomicLong eventId = new AtomicLong();
        carClass.ifPresentOrElse(
                cc -> {
                    eventId.set(cc.getEventId());
                    carClassRepository.delete(cc);
                },
                () -> addError("No car class found for id " + classId, model)
        );
        return redirectView(eventId.get(), messagesEncoded(model));
    }

    @PostMapping("/event-logo-upload")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String eventLogoUpload(@RequestParam("file") MultipartFile multipartFile,
                                  @RequestParam("eventId") String eventId, Model model) {
        Optional<EventSeries> series = eventRepository.findById(Long.parseLong(eventId));
        series.ifPresentOrElse(
                event -> {
                    try {
                        String logoUrl = uploadFileManager.uploadEventFile(multipartFile, eventId, FileTypeEnum.LOGO);
                        event.setLogoUrl(logoUrl);
                        eventRepository.save(event);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        addError("Could not save uploaded file " + multipartFile.getOriginalFilename(), model);
                    }
                },
                () -> addError("Event series not found for id " + eventId, model)
        );
        return redirectView(Long.parseLong(eventId), messagesEncoded(model));
    }

    @PostMapping("event-save-person")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String saveStaffPerson(@ModelAttribute PersonView personView) {
        Optional<Person> staff = personRepository.findByEventIdAndIracingId(personView.getEventId(), personView.getIracingId());
        Person toSave = personView.toEntity(staff.orElse(new Person()));
        personRepository.save(toSave);
        return redirectView(personView.getEventId(), null);
    }

    @GetMapping("remove-staff")
    @Secured({"ROLE_SYSADMIN", "ROLE_RACE_DIRECTOR"})
    @Transactional
    public String removeStaffPerson(@RequestParam long personId, Model model) {
        Optional<Person> person = personRepository.findById(personId);
        AtomicLong eventId = new AtomicLong(0L);
        person.ifPresentOrElse(
                p -> {
                    eventId.set(p.getEventId());
                    personRepository.delete(p);
                },
                () -> addError("No person found for id " + personId, model)
        );
        return redirectView(eventId.get(), messagesEncoded(model));
    }

    @ModelAttribute(name="allCars")
    public List<CarView> allCars() {
        return StockDataTools.carsByCategory(iRacingClient.getDataCache().getCars(), CarCategoryType.ROAD, false).stream()
                .filter(car -> Arrays.stream(car.getCarTypes()).anyMatch(type -> type.getCarType().equalsIgnoreCase("road")))
                .map(car -> CarView.builder()
                        .carId(car.getCarId())
                        .name(car.getCarName())
                        .build())
                .sorted(Comparator.comparing(CarView::getName))
                .collect(Collectors.toList());
    }

    @ModelAttribute(name="staffRoles")
    public List<OrgaRoleType> staffRoles() {
        return OrgaRoleType.racecontrolValues();
    }

    private String redirectView(long eventId, String messagesEncoded) {
        return "redirect:/" + EventAdminController.CREATE_EVENT_VIEW
                + (eventId != 0 ? "?eventId=" + eventId : "")
                + (messagesEncoded != null ? "&messages=" + messagesEncoded : "");
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
