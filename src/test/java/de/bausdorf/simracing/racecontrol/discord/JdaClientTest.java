package de.bausdorf.simracing.racecontrol.discord;

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

import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.internal.handle.GuildSetupController;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;


@SpringBootTest(classes = {JdaClient.class})
@EnableConfigurationProperties(value = RacecontrolServerProperties.class)
@TestPropertySource("file:application-local.properties")
@Slf4j
public class JdaClientTest {

    @Autowired private RacecontrolServerProperties config;
    @Autowired private JdaClient jdaClient;

    @Test
    public void testJda() {
        JDA api = jdaClient.getApi();
        SelfUser self = api.getSelfUser();
        log.info(self.toString());
        AccountType accoutType = api.getAccountType();
        log.info(accoutType.toString());
//        Guild guild = api.getGuildById(631774413057949706L);
//
//
//        log.info(guild.toString());
//        guilds.forEach(g -> log.info(g.getName()));
    }
}