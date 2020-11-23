package de.bausdorf.simracing.racecontrol.discord;

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

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.bausdorf.simracing.racecontrol.model.RcBulletin;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DiscordNotifier {

	private final RacecontrolServerProperties serverProperties;

	public DiscordNotifier(@Autowired RacecontrolServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	public String sendRcBulletin(RcBulletin bulletin) {

		DiscordWebhook webhook = new DiscordWebhook(serverProperties.getDiscordBulletinUrl());
		DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
				.setTitle(bulletin.getDiscordTitle())
				.addField("car", "#" + bulletin.getCarNo(), true);

		boolean messageInline = true;
		if(bulletin.getViolationDescription() != null) {
			embed.addField("rule violation", bulletin.getViolationText(), true);
			messageInline = false;
		}
		if(bulletin.getSelectedPenaltyCode() != null) {
			embed.addField("penalty", bulletin.getPenaltyText(), true);
		}
		if(!bulletin.getMessage().isEmpty()) {
			embed.addField("Message", bulletin.getMessage(), messageInline);
		}

		webhook.addEmbed(embed);
		try {
			webhook.execute();
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch(HttpClientErrorException clientError) {
			log.warn(clientError.getMessage());
		}
		return null;
	}
}
