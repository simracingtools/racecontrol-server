package de.bausdorf.simracing.racecontrol.discord.command;

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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractCommand {
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    List<OptionData> options = new ArrayList<>();

    protected AbstractCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void buildCommand(Guild guild) {
        log.debug("Update command {}", name);
        guild.upsertCommand(name, description).addOptions(options).queue();
    }

    public abstract void onEvent(@NotNull SlashCommandInteractionEvent event);
}
