package de.bausdorf.simracing.racecontrol;

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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

//@RunWith(SpringRunner.class)
@SpringBootTest
//@Testcontainers
@ActiveProfiles("test")
@Slf4j
class RacecontrolServerApplicationTests {
//	@Container
//	public static MariaDBContainer database = new MariaDBContainer();
//
//	@DynamicPropertySource
//	static void databaseProperties(DynamicPropertyRegistry registry) {
//		registry.add("spring.datasource.url", database::getJdbcUrl);
//		registry.add("spring.datasource.username", database::getUsername);
//		registry.add("spring.datasource.password", database::getPassword);
//	}

	@Test
	void contextLoads() {
		try {
			log.info("context successfully loaded");
		} catch (Exception e) {
			Assertions.fail(e.getMessage(), e);
		}
	}

	@Test
	void testEncodings() {
		String encoded = "J\u00f9lien Jean";
		try {
			String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
			log.info("{} -> {}", encoded, decoded);
		} catch (IllegalArgumentException e)  {
			Assertions.fail(e.getMessage(), e);
		}
	}
}
