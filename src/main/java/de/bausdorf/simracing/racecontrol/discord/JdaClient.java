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

import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeriesRepository;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JdaClient extends ListenerAdapter {
    private final EventSeriesRepository eventSeriesRepository;

    @Getter
    private JDA api;
    @Getter
    private Role roleRaceControl;
    @Getter
    private Role roleOrganization;
    @Getter
    private Role roleEveryone;

    public JdaClient(@Autowired RacecontrolServerProperties config,
                     @Autowired EventSeriesRepository eventSeriesRepository) {
        this.eventSeriesRepository = eventSeriesRepository;
        try {
            this.api = JDABuilder.createDefault(config.getDiscordBotToken())
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.ROLE_TAGS, CacheFlag.CLIENT_STATUS)
                    .addEventListeners(this)
                    .build();
        } catch (LoginException | IllegalStateException e) {
            log.error(e.getMessage());
        }
    }

    public Guild getGuildById(long eventId) {
        Optional<EventSeries> eventSeries = eventSeriesRepository.findById(eventId);
        return eventSeries.map(series -> api.getGuildById(series.getDiscordGuildId())).orElse(null);
    }

    public Optional<Member> getMember(long eventId, String iRacingName) {
        Guild connectedGuild = getGuildById(eventId);
        if(connectedGuild != null) {
            return connectedGuild.getMembers().stream()
                    .filter(member -> member.getEffectiveName().contains(iRacingName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Category> getCategory(long eventId, String categoryName) {
        Guild connectedGuild = getGuildById(eventId);
        if(connectedGuild != null) {
            return connectedGuild.getCategories().stream()
                    .filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Role> getRole(long eventId, String roleName) {
        Guild connectedGuild = getGuildById(eventId);
        if(connectedGuild != null) {
            return connectedGuild.getRoles().stream()
                    .filter(role -> role.getName().equalsIgnoreCase(roleName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Category getTeamSpacerCategory(long eventId) {
        Optional<EventSeries> eventSeries = eventSeriesRepository.findById(eventId);
        if(eventSeries.isPresent()) {
            Guild guild = api.getGuildById(eventSeries.get().getDiscordGuildId());
            if(guild!=null) {
                return guild.getCategoryById(eventSeries.get().getDiscordSpacerCategoryId());
            }
        }
        return null;
    }

    public MessageChannel getPresetChannel(long eventId) {
        Optional<EventSeries> eventSeries = eventSeriesRepository.findById(eventId);
        if(eventSeries.isPresent()) {
            Guild guild = api.getGuildById(eventSeries.get().getDiscordGuildId());
            if(guild != null ) {
                return guild.getChannelById(MessageChannel.class, eventSeries.get().getDiscordPresetChannelId());
            }
        }
        return null;
    }

    public List<Category> getTeamCategories(long discordGuildId, long spacerCategoryId) {
        Guild guild = api.getGuildById(discordGuildId);
        if(guild != null ) {
            Category spacer = guild.getCategoryById(spacerCategoryId);
            if(spacer != null) {
                return guild.getCategories().stream()
                        .filter(cat -> cat.getPosition() > spacer.getPosition())
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        log.debug("JDA ready event: {}/{}", event.getGuildAvailableCount(), event.getGuildTotalCount());
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        log.debug("JDA guild ready: {}({})", event.getGuild().getName(), event.getGuild().getId());
    }

    @Override
    public void onGuildAvailable(@NotNull GuildAvailableEvent event) {
        log.debug("JDA guild available: {}({})", event.getGuild().getName(), event.getGuild().getId());
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        log.debug("JDA member role added: {}", event.getGuild().getName());
    }
}
