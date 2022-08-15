package de.bausdorf.simracing.racecontrol.web.model.orga;

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
import org.springframework.lang.NonNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CarClassView {
    private long id;
    private long eventId;
    private String name;
    private List<CarView> cars;
    private List<Long> carIds;
    private long slots;
    private long wildcards;
    private long classOrder;

    public static CarClassView fromEntity(@NonNull CarClass carClass) {
        return CarClassView.builder()
                .id(carClass.getId())
                .eventId(carClass.getEventId())
                .name(carClass.getName())
                .cars(carClass.getCars().stream()
                        .map(c-> CarView.builder()
                                .carClassId(c.getCarClassId())
                                .carId(c.getId())
                                .name(c.getCarName())
                                .build())
                        .collect(Collectors.toList())
                )
                .carIds(carClass.getCars().stream()
                        .map(BalancedCar::getCarId)
                        .collect(Collectors.toList()))
                .slots(carClass.getMaxSlots())
                .wildcards(carClass.getWildcards())
                .classOrder(carClass.getClassOrder())
                .build();
    }

    public static List<CarClassView> fromEntityList(@NonNull List<CarClass> carClasses) {
        return carClasses.stream()
                .sorted(Comparator.comparing(CarClass::getClassOrder))
                .map(CarClassView::fromEntity)
                .collect(Collectors.toList());
    }
}
