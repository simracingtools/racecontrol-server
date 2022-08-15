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

import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.orga.model.Person;
import de.bausdorf.simracing.racecontrol.orga.model.PersonRepository;
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistration;
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MembersCommand extends AbstractCommand{
    private final EventOrganizer eventOrganizer;

    public MembersCommand(@Autowired EventOrganizer eventOrganizer,
                          @Autowired CommandHolder commandHolder) {
        super("members", "Retrieve team member information");
        this.eventOrganizer = eventOrganizer;
        OptionData carOrQualifier = new OptionData(OptionType.STRING, "qualifier", "Car qualifier or car number", false);
        getOptions().add(carOrQualifier);
        commandHolder.addCommand(this);
    }

    @Override
    @Transactional
    public void onEvent(@NotNull SlashCommandInteractionEvent event) {
        try {
            Member member = event.getMember();
            Guild guild = event.getGuild();
            if (member != null && guild != null) {
                log.info("/members from {}", Objects.requireNonNull(event.getMember()).getEffectiveName());
                OptionMapping optionMapping = event.getOption("qualifier");
                AtomicReference<String> optionValue = new AtomicReference<>("");
                if (optionMapping != null) {
                    optionValue.set(optionMapping.getAsString());
                    log.info("option: " + optionValue.get());
                }

                List<EventSeries> eventSeries = eventOrganizer.getActiveEventsForGuildId(guild.getIdLong());
                if (eventSeries.size() != 1) {
                    log.info("No active event identified");
                    event.reply("Sorry, can not identify an active event").queue();
                    return;
                }
                long eventId = eventSeries.get(0).getId();
                List<TeamRegistration> teams = getTeamsForRole(eventId, member, optionValue);
                log.debug("{} matching registrations on member roles", teams.size());

                if (!teams.isEmpty()) {
                    event.reply(buildReply(teams)).queue();
                } else {
                    event.reply("No teams matching you role(s) found").queue();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            event.reply("Error: " + e.getMessage()).queue();
        }
    }

    private List<TeamRegistration> getTeamsForRole(long eventId, Member member, AtomicReference<String> optionValue) {
        return eventOrganizer.getActiveTeamRegistrations(eventId).stream()
                .filter(team -> member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(team.getTeamName())))
                .filter(team -> (!optionValue.get().isBlank() &&
                        (team.getCarQualifier().equalsIgnoreCase(optionValue.get()) || team.getAssignedCarNumber().equalsIgnoreCase(optionValue.get())))
                        || optionValue.get().isBlank())
                .collect(Collectors.toList());
    }

    private Message buildReply(List<TeamRegistration> teams) {
        MessageBuilder reply = new MessageBuilder("Member Status\n");
        teams.forEach(team -> {
            StringBuilder teamName = new StringBuilder(team.getAssignedCarNumber())
                    .append(' ').append(team.getTeamName()).append(' ').append(team.getCarQualifier());
            reply.append("**").append(teamName).append("**").append('\n');
            List<Person> teamMembers = team.getTeamMembers();
            teamMembers.forEach(person -> {
                reply.append("*").append(person.getName()).append(": ").append(person.getRole().name()).append("*\n");
                reply.append(person.isLeagueMember() ? "" : "Has to join iRacing league!\n");
                reply.append(person.isRegistered() ? "" : "Should register on racecontrol website\n");
                reply.append(person.isIracingTeamChecked() ? "" : "Is not a member of iRacing team!");
            });
        });
        return reply.build();
    }
}
