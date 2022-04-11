package de.bausdorf.simracing.racecontrol.web.model;

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
    private Long iRacingId;
    private String name;
    private String role;

    public static PersonView fromEntity(Person person) {
        return PersonView.builder()
                .iRacingId(person.getId())
                .name(person.getName())
                .role(person.getRole().name())
                .build();
    }

    public static List<PersonView> fromEntityList(List<Person> person) {
        return person.stream()
                .map(PersonView::fromEntity)
                .collect(Collectors.toList());
    }
}
