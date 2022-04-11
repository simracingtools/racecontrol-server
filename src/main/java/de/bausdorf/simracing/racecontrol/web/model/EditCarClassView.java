package de.bausdorf.simracing.racecontrol.web.model;

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

import de.bausdorf.simracing.racecontrol.orga.model.BalancedCar;
import de.bausdorf.simracing.racecontrol.orga.model.CarClass;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EditCarClassView {
    private long id;
    private long eventId;
    private String name;
    private List<Long> carIds;
    private long slots;
    private long wildcards;

    public CarClass toEntity(CarClass carClass) {
        if (carClass == null) {
            carClass = new CarClass();
        }
        carClass.setId(id == 0 ? carClass.getId() : id);
        carClass.setEventId(eventId == 0 ? carClass.getEventId() : eventId);
        carClass.setMaxSlots(Math.toIntExact(slots == 0 ? carClass.getMaxSlots() : slots));
        carClass.setWildcards(Math.toIntExact(wildcards == 0 ? carClass.getWildcards() : wildcards));
        carClass.setName(name == null ? carClass.getName() : name);
        if(carClass.getCars() != null) {
            carClass.getCars().clear();
        }
        carClass.setCars(carIds == null ? carClass.getCars() : carIds.stream()
                .map(carId -> {
                    BalancedCar bc = new BalancedCar();
                    bc.setCarId(carId);
                    return bc;
                })
                .collect(Collectors.toList()));
        return carClass;
    }
}
