package de.bausdorf.simracing.racecontrol.web;

import de.bausdorf.simracing.racecontrol.orga.model.CarClass;
import de.bausdorf.simracing.racecontrol.orga.model.CarClassRepository;
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistration;
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistrationRepository;
import de.bausdorf.simracing.racecontrol.web.model.orga.AvailableSlotsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EventOrganizer {
    private final CarClassRepository carClassRepository;
    private final TeamRegistrationRepository registrationRepository;

    public EventOrganizer(@Autowired CarClassRepository carClassRepository,
                          @Autowired TeamRegistrationRepository registrationRepository) {
        this.carClassRepository = carClassRepository;
        this.registrationRepository = registrationRepository;
    }

    public List<AvailableSlotsView> getAvailableGridSlots(long eventId) {
        List<AvailableSlotsView> carClassMap = new ArrayList<>();
        List<CarClass> eventClasses = carClassRepository.findAllByEventId(eventId);
        List<TeamRegistration> eventRegistrations = registrationRepository.findAllByEventId(eventId);
        eventClasses.forEach(carClass -> {
            AtomicLong availableSlotsCount = new AtomicLong(carClass.getMaxSlots());
            AtomicLong wildcards = new AtomicLong(carClass.getWildcards());
            AtomicLong onWaitingList = new AtomicLong(0L);

            eventRegistrations.stream()
                    .filter(r -> (r.getCar().getCarClassId() == carClass.getId()
                            && !r.getWorkflowState().isInActive()))
                    .forEach(r -> {
                        if (availableSlotsCount.get() > 0) {
                            availableSlotsCount.decrementAndGet();
                        } else {
                            onWaitingList.incrementAndGet();
                        }
                        if (r.isWildcard() && wildcards.get() > 0) {
                            wildcards.decrementAndGet();
                        }
                    });

            AvailableSlotsView availableSlots = AvailableSlotsView.builder()
                    .carClassId(carClass.getId())
                    .name(carClass.getName())
                    .availableSlots(availableSlotsCount.get())
                    .wildcards(wildcards.get())
                    .onWaitingList(onWaitingList.get())
                    .build();
            carClassMap.add(availableSlots);
        });
        return carClassMap;
    }
}
