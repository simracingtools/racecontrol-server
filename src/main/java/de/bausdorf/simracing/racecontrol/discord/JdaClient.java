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

import de.bausdorf.simracing.racecontrol.discord.command.AbstractCommand;
import de.bausdorf.simracing.racecontrol.discord.command.CommandHolder;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeriesRepository;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JdaClient extends ListenerAdapter {
    private final EventSeriesRepository eventSeriesRepository;
    private final CommandHolder commandHolder;
    @Getter
    private JDA api;

    private final Map<Long, Guild> guildCache = new HashMap<>();

    public JdaClient(@Autowired RacecontrolServerProperties config,
                     @Autowired EventSeriesRepository eventSeriesRepository,
                     @Autowired CommandHolder commandHolder) {
        this.eventSeriesRepository = eventSeriesRepository;
        this.commandHolder = commandHolder;
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

    public Guild getGuildByEventId(long eventId) {
        AtomicReference<Guild> guild = new AtomicReference<>(guildCache.get(eventId));
        if(guild.get() == null) {
            Optional<EventSeries> eventSeries = eventSeriesRepository.findById(eventId);
            eventSeries.ifPresent(series -> guild.set(api.getGuildById(series.getDiscordGuildId())));
        }
        return guild.get();
    }

    public void addRoleToMember(long eventId, Member member, String roleName) {
        Guild guild = getGuildByEventId(eventId);
        Optional<Role> role = guild.getRoles().stream()
                .filter(r -> r.getName().equalsIgnoreCase(roleName))
                .findFirst();

        role.ifPresentOrElse(
                r -> guild.addRoleToMember(member, r).complete(),
                () -> {
                    Role classRole = guild.createRole().setName(roleName).complete();
                    guild.addRoleToMember(member, classRole).complete();
                });
    }

    public void removeRoleFromMember(long eventId, Member member, String roleName) {
        Guild guild = getGuildByEventId(eventId);
        Optional<Role> roleOptional = member.getRoles().stream()
                .filter(r -> r.getName().equalsIgnoreCase(roleName))
                .findFirst();
        roleOptional.ifPresent(role -> guild.removeRoleFromMember(member, role).complete());
    }

    public Optional<Member> getMember(long eventId, String iRacingName) {
        Guild connectedGuild = getGuildByEventId(eventId);
        if(connectedGuild != null) {
            return connectedGuild.getMembers().stream()
                    .filter(member -> matchMemberName(member, iRacingName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Member> getMemberUncached(long eventId, String iRacingName) {
        Guild connectedGuild = getGuildByEventId(eventId);
        if(connectedGuild != null) {
            AtomicReference<List<Member>> loadedMembers = new AtomicReference<>(List.of());
            AtomicBoolean success = new AtomicBoolean(false);
            connectedGuild.loadMembers().onSuccess(
                    (List<Member> members) -> {
                        loadedMembers.set(members);
                        success.set(true);
                    }
            );
            int checkCounter = 0;
            while(!success.get()) {
                try {
                    Thread.currentThread().join(500);
                    log.debug("wait for {} ms", ++checkCounter * 500);
                } catch (InterruptedException e) {
                    log.error("Current thread interrupted: {}", e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
            return loadedMembers.get().stream()
                    .filter(member -> matchMemberName(member, iRacingName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Category> getCategory(long eventId, String categoryName) {
        Guild connectedGuild = getGuildByEventId(eventId);
        if(connectedGuild != null) {
            return connectedGuild.getCategories().stream()
                    .filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Role> getRole(long eventId, String roleName) {
        Guild connectedGuild = getGuildByEventId(eventId);
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

    public Role getRoleByName(Guild guild, String roleName) {
        List<Role> roles = guild.getRolesByName(roleName, true);
        return roles.stream().findFirst().orElse(null);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        log.info("Slash command: {}", event.getInteraction().getName());
        Optional<AbstractCommand> command = commandHolder.getCommand(event.getInteraction().getName());
        command.ifPresent(c -> c.onEvent(event));
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        log.info("Guild join: {}", event.getMember().getNickname());
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        log.info("{} changed nickname from {} to {}", event.getMember().getId(), event.getOldNickname(), event.getNewNickname());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        log.debug("JDA ready event: {}/{}", event.getGuildAvailableCount(), event.getGuildTotalCount());
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        log.debug("JDA guild ready: {}({})", event.getGuild().getName(), event.getGuild().getId());
        commandHolder.stream().forEach(command -> command.buildCommand(event.getGuild()));
    }

    @Override
    public void onGuildAvailable(@NotNull GuildAvailableEvent event) {
        log.debug("JDA guild available: {}({})", event.getGuild().getName(), event.getGuild().getId());
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        log.debug("JDA member role added: {}", event.getGuild().getName());
    }

    public static boolean matchMemberName(Member member, String fullName) {
        String[] nameParts = fullName.trim().split(" ");
        return Arrays.stream(nameParts).allMatch(part -> member.getEffectiveName().toLowerCase().contains(part.toLowerCase()));
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanGuildCache() {
        log.info("Cleaning guild cache");
        guildCache.clear();
    }
}
