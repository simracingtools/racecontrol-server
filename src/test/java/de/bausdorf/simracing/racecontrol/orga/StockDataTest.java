package de.bausdorf.simracing.racecontrol.orga;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bausdorf.simracing.racecontrol.orga.model.Car;
import de.bausdorf.simracing.racecontrol.orga.model.CarRepository;
import de.bausdorf.simracing.racecontrol.util.StockDataLoader;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class StockDataTest {

    @Autowired
    StockDataLoader dataLoader;


    @Value("classpath:data/cars.json")
    Resource carResourceFile;
    @Value("classpath:data/tracks.json")
    Resource trackResourceFile;
    @Value("classpath:data/configs.json")
    Resource configResourceFile;


    @Test
    @Rollback(false)
    void testLoadCarJson() {
        ObjectMapper mapper = new ObjectMapper();

        // read JSON file and map/convert to java POJO
        try {
            List<StockDataLoader.CarJsonData> carJsonDataList = mapper.readValue(carResourceFile.getInputStream(),
                    new TypeReference<List<StockDataLoader.CarJsonData>>(){});
            dataLoader.loadCarJsonData(carJsonDataList);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Test
    @Rollback(false)
    void testLoadTrackJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<StockDataLoader.TrackJsonData> trackJsonDataList = mapper.readValue(trackResourceFile.getInputStream(),
                    new TypeReference<List<StockDataLoader.TrackJsonData>>(){});
            List<StockDataLoader.TrackConfigJsonData> trackConfigJsonData = mapper.readValue(configResourceFile.getInputStream(),
                    new TypeReference<List<StockDataLoader.TrackConfigJsonData>>() {});
            dataLoader.loadTrackJsonData(trackJsonDataList, trackConfigJsonData);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
