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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalancedCarView {
    private long id;
    private long eventId;
    private long carId;
    private long carClassId;
    private String carName;
    private String carLogoUrl;
    private double maxFuel;
    private double fuelPercent;
    private double weightPenalty;
    private double enginePowerPercent;

    public String getBopFuel() {
        return String.format("%.2f", (maxFuel * fuelPercent) / 100);
    }

    public BalancedCar toEntity(BalancedCar car) {
        if(car == null) {
            car = new BalancedCar();
        }
        car.setId(id == 0L ? car.getId() : id);
        car.setCarId(carId == 0L ? car.getCarId() : carId);
        car.setCarClassId(carClassId == 0L ? car.getCarClassId() : carClassId);
        car.setCarName(carName == null ? car.getCarName() : carName);
        car.setMaxFuel(maxFuel);
        car.setFuelPercent(fuelPercent);
        car.setWeightPenalty(weightPenalty);
        car.setEnginePowerPercent(enginePowerPercent);
        return car;
    }

    public static BalancedCarView fromEntity(long eventId, BalancedCar car) {
        return BalancedCarView.builder()
                .id(car.getId())
                .eventId(eventId)
                .carId(car.getCarId())
                .carClassId(car.getCarClassId())
                .carName(car.getCarName())
                .maxFuel(car.getMaxFuel())
                .fuelPercent(car.getFuelPercent() == 0.0 ? 100.0 : car.getFuelPercent())
                .weightPenalty(car.getWeightPenalty())
                .enginePowerPercent(car.getEnginePowerPercent())
                .build();
    }
}
