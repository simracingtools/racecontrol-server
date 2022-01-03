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

import de.bausdorf.simracing.racecontrol.orga.model.Car;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarView {
    private long carId;
    private String name;
    private String brand;
    private double maxFuel;

    public void updateEntity(Car entity) {
        entity.setCarId(carId);
        entity.setName(name);
        entity.setBrand(brand);
        entity.setMaxFuel(maxFuel);
    }
    public static CarView buildFromEntity(@Nullable Car entity) {
        if(entity == null) {
            return buildEmpty();
        }
        return CarView.builder()
                .carId(entity.getCarId())
                .name(entity.getName())
                .brand(entity.getBrand())
                .maxFuel(entity.getMaxFuel())
                .build();
    }

    public static CarView buildEmpty() {
        return CarView.builder()
                .carId(0)
                .name("")
                .brand("")
                .maxFuel(0.0D)
                .build();
    }
}
