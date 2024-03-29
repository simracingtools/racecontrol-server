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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarView {
    private long carId;
    private long carClassId;
    private String name;
    private double maxFuel;

    public void updateEntity(BalancedCar entity) {
        entity.setCarId(carId);
        entity.setCarName(name);
        entity.setCarClassId(carClassId);
        entity.setMaxFuel(maxFuel);
    }
    public static CarView buildFromEntity(@Nullable BalancedCar entity) {
        if(entity == null) {
            return buildEmpty();
        }
        return CarView.builder()
                .carId(entity.getCarId())
                .carClassId(entity.getCarClassId())
                .name(entity.getCarName())
                .maxFuel(entity.getMaxFuel())
                .build();
    }

    public static List<CarView> buildFromEntityList(List<BalancedCar> cars) {
        return cars.stream().map(CarView::buildFromEntity).collect(Collectors.toList());
    }

    public static CarView buildEmpty() {
        return CarView.builder()
                .carId(0)
                .carClassId(0)
                .name("")
                .maxFuel(0.0D)
                .build();
    }
}
