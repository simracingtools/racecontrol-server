package de.bausdorf.simracing.racecontrol.web.action;

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

import de.bausdorf.simracing.racecontrol.discord.JdaClient;
import de.bausdorf.simracing.racecontrol.orga.model.*;
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import de.bausdorf.simracing.racecontrol.web.model.orga.WorkflowActionEditView;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import javax.transaction.Transactional;
import java.util.*;

@Component("DISCORD_PREPARED")
public class PrepareDisordAction extends WorkflowAction {

    private final CarClassRepository carClassRepository;
    private final JdaClient jdaClient;
    private final List<Permission> allowedForMember;
    private final List<Permission> allowedForBot;

    public PrepareDisordAction(@Autowired EventOrganizer eventOrganizer,
                               @Autowired CarClassRepository carClassRepository,
                               @Autowired JdaClient jdaClient) {
        super(eventOrganizer);
        this.jdaClient = jdaClient;
        this.carClassRepository = carClassRepository;
        this.allowedForMember = new ArrayList<>();
        this.allowedForBot = new ArrayList<>();
        initRoles();
    }

    @Override
    @Transactional
    public void performAction(WorkflowActionEditView editView, Person actor) throws ActionException {
        de.bausdorf.simracing.racecontrol.orga.model.WorkflowAction currentAction = getEventOrganizer().getWorkflowAction(editView.getId());
        if(currentAction != null) {
            TeamRegistration registration = updateCurrentAction(editView, currentAction, actor);
//            TeamRegistration registration = getEventOrganizer().getTeamRegistration(currentAction.getWorkflowItemId());
            prepareDiscord(registration, editView.getMessage());
            getEventOrganizer().saveRegistration(registration);
            getEventOrganizer().createFollowUpAction(currentAction, actor, editView.getDueDate());
        } else {
            throw new ActionException("Current action not found");
        }
    }

    private void prepareDiscord(TeamRegistration registration, String actionMessage) {
        Guild guild = jdaClient.getGuildByEventId(registration.getEventId());
        String discordName = !StringUtils.isEmpty(actionMessage) ? actionMessage.trim() : registration.getTeamName();

        if(guild != null) {
            Role role = jdaClient.getRole(registration.getEventId(), discordName).orElse(null);
            if(role == null) {
                RoleAction action = guild.createRole().setName(discordName).setPermissions(allowedForMember);
                role = action.complete();
            }
            Category category = jdaClient.getCategory(registration.getEventId(), discordName).orElse(null);
            if(category == null) {
                ChannelAction<Category> action = guild.createCategory(discordName)
                        .addMemberPermissionOverride(role.getIdLong(), allowedForMember, null)
                        .addMemberPermissionOverride(jdaClient.getApi().getSelfUser().getIdLong(), allowedForBot, null)
                        .addRolePermissionOverride(role.getIdLong(), allowedForMember, null)
                        .addRolePermissionOverride(jdaClient.getRoleByName(guild, "RaceControl").getIdLong(), allowedForMember, null)
                        .addRolePermissionOverride(jdaClient.getRoleByName(guild, "Organization").getIdLong(), allowedForMember, null)
                        .addRolePermissionOverride(guild.getPublicRole().getIdLong(), null, allowedForMember)
                        .setPosition(getTeamCategoryInsertPosition(registration.getEventId(), discordName));
                category = action.complete();
            }

            Optional<Long> textChannelId = category.getChannels().stream()
                    .filter(channel -> channel.getName().equals("text"))
                    .map(GuildChannel::getIdLong)
                    .findFirst();
            if(textChannelId.isEmpty()) {
                TextChannel textChannel = category.createTextChannel("text").complete();
                registration.setDiscordChannelId(textChannel.getIdLong());
                Role mentionedRole = role;
                getPresetPosts(registration.getEventId()).forEach(m -> {
                    String text = m.getContentDisplay();
                    textChannel.sendMessage(mentionedRole.getAsMention() + " " + text)
                            .mention(mentionedRole)
                            .queue();
                });
            } else {
                registration.setDiscordChannelId(textChannelId.get());
            }

            String voiceChannelName = "#" + registration.getAssignedCarNumber() + " voice";
            if(category.getChannels().stream().noneMatch(channel -> channel.getName().equals(voiceChannelName))) {
                category.createVoiceChannel(voiceChannelName)
                        .syncPermissionOverrides()
                        .complete();
            }

            Member discordMember = jdaClient.getMember(registration.getEventId(), registration.getCreatedBy().getName()).orElse(null);
            if(discordMember != null) {
                Optional<CarClass> carClass = carClassRepository.findById(registration.getCar().getCarClassId());
                carClass.ifPresent(cc -> {
                        Optional<Role> carClassRole = guild.getRoles().stream()
                                .filter(r -> r.getName().equalsIgnoreCase(cc.getName()))
                                .findFirst();

                        carClassRole.ifPresentOrElse(
                                r -> guild.addRoleToMember(discordMember, r).complete(),
                                () -> {
                                    Role classRole = guild.createRole().setName(cc.getName()).complete();
                                    guild.addRoleToMember(discordMember, classRole).complete();
                                });
                });
                guild.addRoleToMember(discordMember, role).complete();
            }
        }
    }

    private int getTeamCategoryInsertPosition(long eventId, String categoryName) {
        EventSeries event = getEventOrganizer().getEventSeries(eventId);
        int insertPosition = 0;
        if(event != null) {
            List<Category> teamCategories = jdaClient.getTeamCategories(event.getDiscordGuildId(), event.getDiscordSpacerCategoryId());
            for(Category category : teamCategories) {
                insertPosition = category.getPosition();
                if(category.getName().compareTo(categoryName) > 0) {
                    break;
                }
            }
        }
        return insertPosition;
    }

    private List<Message> getPresetPosts(long eventId) {
        MessageHistory presetHistory = jdaClient.getPresetChannel(eventId).getHistoryFromBeginning(5).complete();
        if(!presetHistory.isEmpty()) {
            return presetHistory.getRetrievedHistory();
        }
        return List.of();
    }

    private void initRoles() {
        allowedForMember.addAll(Permission.getPermissions(Permission.ALL_TEXT_PERMISSIONS));
        allowedForMember.addAll(List.of(
                Permission.VIEW_CHANNEL,
                Permission.CREATE_INSTANT_INVITE,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_SEND_IN_THREADS,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_HISTORY,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_USE_VAD,
                Permission.NICKNAME_CHANGE
        ));

        allowedForBot.addAll(allowedForMember);
        allowedForBot.add(Permission.MANAGE_CHANNEL);
    }
}
