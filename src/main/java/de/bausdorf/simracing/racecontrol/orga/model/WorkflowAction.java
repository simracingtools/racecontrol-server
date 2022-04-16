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

import lombok.*;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class WorkflowAction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long eventId;
    @Column(nullable = false)
    private String workflowName;
    @Column(nullable = false)
    @Convert(converter = OffsetDateTimeConverter.class)
    OffsetDateTime created;
    @Column(nullable = false)
    private Long workflowItemId;
    @ManyToOne
    private Person createdBy;
    @Convert(converter = OffsetDateTimeConverter.class)
    OffsetDateTime dueDate;
    @ManyToOne
    private WorkflowState sourceState;
    @ManyToOne
    private WorkflowState targetState;
    @ManyToOne
    private Person doneBy;
    @Convert(converter = OffsetDateTimeConverter.class)
    OffsetDateTime doneAt;
    private String message;
}
