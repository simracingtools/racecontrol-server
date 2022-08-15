package de.bausdorf.simracing.racecontrol.web;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
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

import de.bausdorf.simracing.irdataapi.client.IrDataClient;
import de.bausdorf.simracing.irdataapi.client.impl.IrDataClientImpl;
import de.bausdorf.simracing.irdataapi.config.ConfigProperties;
import de.bausdorf.simracing.irdataapi.model.AuthResponseDto;
import de.bausdorf.simracing.irdataapi.model.LoginRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class IracingApiTest {

	@Autowired
	ConfigProperties props;

	private final IrDataClient dataClient = new IrDataClientImpl();

	@Test
	void testIracingLogin() {
		try {
			LoginRequestDto irLoginRequest = LoginRequestDto.builder()
					.email(props.getUser())
					.password(props.getPassword())
					.build();
			AuthResponseDto responseDto = dataClient.authenticate(irLoginRequest);
			log.info(responseDto.toString());
		} catch (Exception e) {
			fail(e.getMessage(), e);
		}
	}

}
