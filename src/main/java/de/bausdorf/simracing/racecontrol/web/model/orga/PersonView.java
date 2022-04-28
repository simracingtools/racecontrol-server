package de.bausdorf.simracing.racecontrol.web.model.orga;

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

import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import de.bausdorf.simracing.racecontrol.orga.model.Person;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PersonView {
    private long id;
    private long iracingId;
    private long eventId;
    // Helper for team member association
    private long teamId;
    private String name;
    private String role;
    private Boolean leagueMember;
    private Boolean registered;
    private Boolean iracingChecked;

    public Person toEntity(Person person) {
        if(person == null) {
            person = new Person();
        }
        person.setId(id == 0 ? person.getId() : id);
        person.setEventId(eventId == 0 ? person.getEventId() : eventId);
        person.setName(name == null ? person.getName() : name);
        person.setRole(role == null ? person.getRole() : OrgaRoleType.valueOf(role));
        person.setIracingId(iracingId == 0 ? person.getIracingId() : iracingId);
        person.setLeagueMember(leagueMember == null ? person.isLeagueMember() : leagueMember);
        person.setRegistered(registered == null ? person.isRegistered() : registered);
        person.setIracingChecked(iracingChecked == null ? person.isIracingChecked() : iracingChecked);
        return person;
    }

    public static PersonView fromEntity(Person person) {
        if(person == null) {
            return PersonView.builder()
                    .name("")
                    .build();
        }
        return PersonView.builder()
                .id(person.getId())
                .iracingId(person.getIracingId())
                .name(person.getName())
                .eventId(person.getEventId())
                .role(person.getRole().name())
                .registered(person.isRegistered())
                .leagueMember(person.isLeagueMember())
                .iracingChecked(person.isIracingChecked())
                .build();
    }

    public static List<PersonView> fromEntityList(List<Person> person) {
        return person.stream()
                .map(PersonView::fromEntity)
                .collect(Collectors.toList());
    }
}
