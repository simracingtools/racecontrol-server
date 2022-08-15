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

import de.bausdorf.simracing.irdataapi.model.CarInfoDto;
import de.bausdorf.simracing.racecontrol.orga.model.BalancedCar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
@Builder
public class BalancedCarBopView {
    private BalancedCarView carView;

    // iRacing values
    private long weight;
    private long horsePower;
    private double maxPowerAdjustPct;
    private double minPowerAdjustPct;
    private long maxWeightPenalty;

    public String getBopFuel() {
        return carView.getBopFuel();
    }

    public String getBopWeight() {
        return String.format("%.1f",weight + carView.getWeightPenalty());
    }

    public String getBopPower() {
        return String.format("%.2f", horsePower + ((horsePower * carView.getEnginePowerPercent()) / 100));
    }

    public static BalancedCarBopView fromEntityAndCar(long eventId, BalancedCar car, Optional<CarInfoDto> irCar) {
        BalancedCarBopView bopView =  BalancedCarBopView.builder()
                .carView(BalancedCarView.fromEntity(eventId, car))
                .build();

        irCar.ifPresent(c -> {
                bopView.setWeight(c.getCarWeight());
                bopView.setHorsePower(c.getHp());
                bopView.setMaxPowerAdjustPct(c.getMaxPowerAdjustPct());
                bopView.setMinPowerAdjustPct(c.getMinPowerAdjustPct());
                bopView.setMaxWeightPenalty(c.getMaxWeightPenaltyKg());
            });

        return bopView;
    }
}
