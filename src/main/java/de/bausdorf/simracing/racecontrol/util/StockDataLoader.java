package de.bausdorf.simracing.racecontrol.util;

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

import de.bausdorf.simracing.racecontrol.orga.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StockDataLoader {

    private final CarRepository carRepository;
    private final TrackRepository trackRepository;

    public StockDataLoader(@Autowired CarRepository carRepository, @Autowired TrackRepository trackRepository) {
        this.carRepository = carRepository;
        this.trackRepository = trackRepository;
    }

    @Data
    @ToString
    @NoArgsConstructor
    public static class CarJsonData {
        Long carID;
        String url;
        String name;
        String desc;
        String model;
        String make;
        String price;
        Long hp;
        Long weight;
        String w2pRatio;
        Boolean freeWithSubscription;
        String[] discountGroupNames;
    }

    @Data
    @ToString
    @NoArgsConstructor
    public static class TrackJsonData {
        String name;
        Double price;
        String url;
        Long pkgID;
        Long configs;
        String[] categories;
        Boolean freeWithSubscription;
    }

    @Data
    @ToString
    @NoArgsConstructor
    public static class TrackConfigJsonData {
        String name;
        String category;
        String configname;
        Long trackID;
        String url;
        Long sku;
        Double price;
        Long pkgID;
        Boolean freeWithSubscription;
        String[] discountGroupNames;
        Long nlapsQual;
        Long nlapsSolo;
    }

    @Transactional
    public void loadCarJsonData(List<CarJsonData> carJsonDataList) {
        carJsonDataList.forEach(jsonData -> {
            Car car = Car.builder()
                    .carId(jsonData.getCarID())
                    .name(jsonData.getName())
                    .brand(jsonData.getMake())
                    .maxFuel(0.0D)
                    .build();
            Optional<Car> existingCar = carRepository.findById(car.getCarId());
            if(!existingCar.isPresent()) {
                carRepository.save(car);
                log.info("Saved: {}", car);
            } else {
                log.warn("Car with Id {} already present: {}", car.getCarId(), existingCar.get().getName());
            }
        });
    }

    @Transactional
    public void loadTrackJsonData(List<TrackJsonData> trackJsonData, List<TrackConfigJsonData> configJsonData) {
        trackJsonData.forEach(trackData -> {
            List<TrackConfigJsonData> configsForTrack = configJsonData.stream()
                    .filter(s -> s.getPkgID().equals(trackData.getPkgID()))
                    .collect(Collectors.toList());

            Track trackEntity = Track.builder()
                    .pkgId(trackData.getPkgID())
                    .name(trackData.getName())
                    .build();
            if(configsForTrack.size() != trackData.getConfigs()) {
                log.warn("{} expected configs for track {} ({}), but found {}",
                        trackData.getConfigs(), trackData.getPkgID(), trackData.getName(), configsForTrack.size());
            }
            configsForTrack.forEach(configData -> {
                TrackConfiguration configEntity = TrackConfiguration.builder()
                        .track(trackEntity)
                        .configName(configData.getConfigname().isEmpty() ? configData.getName() : configData.getConfigname())
                        .trackId(configData.getTrackID())
                        .build();

                trackEntity.getConfigurations().add(configEntity);
            });
            trackRepository.save(trackEntity);
        });
    }

}
