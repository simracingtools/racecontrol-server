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
import org.springframework.stereotype.Component;

import javax.security.auth.login.LoginException;
import java.util.Optional;

@Component
@Slf4j
public class JdaClient extends ListenerAdapter {
    @Getter
    private JDA api;
    @Getter
    private Guild connectedGuild;
    private final RacecontrolServerProperties config;
    @Getter
    private Role roleRaceControl;
    @Getter
    private Role roleOrganization;
    @Getter
    private Role roleEveryone;
    @Getter
    private Category teamParentCategory;
    @Getter
    private TextChannel presetChannel;

    public JdaClient(@Autowired RacecontrolServerProperties config) {
        this.config = config;
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

    public Optional<Member> getMember(String iRacingName) {
        if(connectedGuild != null) {
            return connectedGuild.getMembers().stream()
                    .filter(member -> member.getEffectiveName().contains(iRacingName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Category> getCategory(String categoryName) {
        if(connectedGuild != null) {
            return connectedGuild.getCategories().stream()
                    .filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
                    .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Role> getRole(String roleName) {
        if(connectedGuild != null) {
            return connectedGuild.getRoles().stream()
                    .filter(role -> role.getName().equalsIgnoreCase(roleName))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        log.debug("JDA ready event: {}/{}", event.getGuildAvailableCount(), event.getGuildTotalCount());
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        log.debug("JDA guild ready: {}({})", event.getGuild().getName(), event.getGuild().getId());
        if(event.getGuild().getId().equals(config.getDiscordGuildId())) {
            this.connectedGuild = event.getGuild();
            log.info("Connected to discord server {}", connectedGuild.getName());
            if(log.isDebugEnabled()) {
                connectedGuild.getMembers().forEach(m -> log.debug("Member: {}({})", m.getNickname(), m.getEffectiveName()));
                connectedGuild.getCategories().forEach(cat -> {
                    if(cat.getName().equalsIgnoreCase("----------------------------------------")) {
                        log.info("Team parent category found");
                        this.teamParentCategory = cat;
                    }
                    log.debug("Category: {}({})", cat.getName(), cat.getPosition());
                });
                connectedGuild.getChannels().forEach(chan -> {
                    if(chan.getName().equalsIgnoreCase("presets")) {
                        presetChannel = (TextChannel) chan;
                        log.info("Preset channel identified");
                    }
                    log.debug("Channel: {}", chan.getName());
                });
                connectedGuild.getRoles().forEach(role -> {
                    if(role.getName().equalsIgnoreCase("RaceControl")) {
                        roleRaceControl = role;
                        log.info("Role RaceControl identified");
                    } else if(role.getName().equalsIgnoreCase("Organization")) {
                        roleOrganization = role;
                        log.info("Role Organization identified");
                    } else if(role.getName().equalsIgnoreCase("@everyone")) {
                        roleEveryone = role;
                        log.info("Role @everyone identified");
                    }
                    log.debug("Role: {}", role.getName());
                });
            }

        }
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
