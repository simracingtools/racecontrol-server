package de.bausdorf.simracing.racecontrol.orga.model;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class WorkflowState {
    @Id
    @GeneratedValue
    private long id;

    private String workflowName;
    private String stateKey;
    private String description;
    private String color;
    private String textColor;
    private boolean initialState;
    private boolean inActive;
    @ElementCollection(targetClass = OrgaRoleType.class)
    @CollectionTable(name="state_duty_roles")
    @Enumerated(EnumType.STRING)
    private List<OrgaRoleType> dutyRoles = new ArrayList<>();
    @ManyToMany
    @JoinTable(
            name = "followups_predecessors",
            joinColumns = @JoinColumn(name = "followUp_id"),
            inverseJoinColumns = @JoinColumn(name = "predecessor_id"))
    private List<WorkflowState> followUps = new ArrayList<>();
    @ManyToMany(mappedBy = "followUps")
    private List<WorkflowState> predecessors = new ArrayList<>();
}
