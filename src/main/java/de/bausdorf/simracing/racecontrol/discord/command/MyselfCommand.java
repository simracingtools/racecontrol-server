package de.bausdorf.simracing.racecontrol.discord.command;

import de.bausdorf.simracing.racecontrol.discord.JdaClient;
import de.bausdorf.simracing.racecontrol.orga.model.EventSeries;
import de.bausdorf.simracing.racecontrol.orga.model.Person;
import de.bausdorf.simracing.racecontrol.orga.model.PersonRepository;
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class MyselfCommand extends AbstractCommand {
    private final PersonRepository personRepository;
    private final EventOrganizer eventOrganizer;
    public MyselfCommand(@Autowired PersonRepository personRepository,
                         @Autowired EventOrganizer eventOrganizer,
                         @Autowired CommandHolder commandHolder) {
        super("myself", "Check if race control can match your Discord identity");
        this.personRepository = personRepository;
        this.eventOrganizer = eventOrganizer;
        commandHolder.addCommand(this);
    }
    @Override
    public void onEvent(@NotNull SlashCommandInteractionEvent event) {
        try {
            Guild guild = event.getGuild();
            List<EventSeries> eventSeries = eventOrganizer.getActiveEventsForGuildId(Objects.requireNonNull(guild).getIdLong());
            if (eventSeries.size() != 1) {
                log.info("No active event identified");
                event.reply("Sorry, can not identify an active event").queue();
                return;
            }

            Optional<Person> match = personRepository.findAllByEventId(eventSeries.get(0).getId()).stream()
                    .filter(p -> JdaClient.matchMemberName(event.getMember(), p.getName()))
                    .findFirst();

            match.ifPresentOrElse(p -> event.reply(buildReply(p)).queue(),
                    () -> event.reply("Found no match on current event in race control system").queue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            event.reply("Error: " + e.getMessage()).queue();
        }
    }

    private Message buildReply(Person person) {
        MessageBuilder reply = new MessageBuilder("You are recognized as:\n");
        reply.append(person.getName()).append(", ").append(person.getRole().name()).append('\n');
        reply.append("iRacing Id: ").append(person.getIracingId());
        return reply.build();
    }
}
