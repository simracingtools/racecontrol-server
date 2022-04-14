package de.bausdorf.simracing.racecontrol.web;

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
import de.bausdorf.simracing.racecontrol.orga.model.WorkflowState;
import de.bausdorf.simracing.racecontrol.orga.model.WorkflowStateRepository;
import de.bausdorf.simracing.racecontrol.web.model.AddWorkflowView;
import de.bausdorf.simracing.racecontrol.web.model.WorkflowStateEditView;
import de.bausdorf.simracing.racecontrol.web.model.WorkflowStateInfoView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Controller
public class WorkflowAdminController extends ControllerBase {
    private static final String WORKFLOW_ADMIN_VIEW = "taskadmin";
    public static final String REDIRECT_TO_WORKFLOW_ADMIN = "redirect:/workflow-admin";
    public static final String WORKFLOW_REDIRECT_PARAM = "?workflow=";

    private final WorkflowStateRepository stateRepository;

    public WorkflowAdminController(@Autowired WorkflowStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @GetMapping("/workflow-admin")
    public String getWorkflowAdminView(@RequestParam Optional<String> error, Optional<String> workflow, Model model) {
        error.ifPresent(e -> addError(e, model));

        Map<String, List<WorkflowStateInfoView>> allWorkflowStates =  new HashMap<>();
        stateRepository.findAll().forEach(workflowState -> {
            List<WorkflowStateInfoView> stateList = allWorkflowStates.get(workflowState.getWorkflowName());
            if (stateList == null) {
                stateList = new ArrayList<>();
            }
            stateList.add(WorkflowStateInfoView.fromEntity(workflowState));
            allWorkflowStates.put(workflowState.getWorkflowName(), stateList);
        });

        workflow.ifPresentOrElse(wf -> {
                    model.addAttribute("activeFlow", wf);
                    model.addAttribute("allCurrentWorkflowStates", WorkflowStateInfoView.fromEntityList(
                            stateRepository.findAllByWorkflowName(wf)
                    ));
                },
                () -> {
                    model.addAttribute("activeFlow", allWorkflowStates.keySet().stream().findFirst().orElse(""));
                    model.addAttribute("allCurrentWorkflowStates", new ArrayList<>());
                });
        model.addAttribute("workflowStateViews", allWorkflowStates);
        model.addAttribute("editWorkflowStateView", WorkflowStateEditView.builder()
                        .workflowName(workflow.orElse(null))
                        .color("#FFFFFF")
                        .textColor("#000000")
                        .build());
        model.addAttribute("addWorkflowView", new AddWorkflowView());
        return WORKFLOW_ADMIN_VIEW;
    }

    @PostMapping("/add-workflow")
    @Transactional
    public String addWorkflow(@ModelAttribute AddWorkflowView addWorkflowView) {
        List<WorkflowState> existingStates = stateRepository.findDistinctByWorkflowName(addWorkflowView.getWorkflowName());
        String error = null;
        if(existingStates.isEmpty()) {
            WorkflowState newWorkflowState = new WorkflowState();
            newWorkflowState.setWorkflowName(addWorkflowView.getWorkflowName());
            newWorkflowState.setStateKey(addWorkflowView.getInitialStateKey().toUpperCase(Locale.ROOT));
            newWorkflowState.setInitialState(true);
            newWorkflowState.setColor("#ffffff");
            newWorkflowState.setTextColor("#000000");
            stateRepository.save(newWorkflowState);
        } else {
            error = "A workflow named " + addWorkflowView.getWorkflowName() + " already exists";
        }

        return REDIRECT_TO_WORKFLOW_ADMIN + WORKFLOW_REDIRECT_PARAM + addWorkflowView.getWorkflowName()
                + (error != null ? "&error=" + error : "");
    }

    @PostMapping("/save-workflow-state")
    @Transactional
    public String saveWorkflowState(@ModelAttribute WorkflowStateEditView editWorkflowStateView) {
        Optional<WorkflowState> existingState = stateRepository.findById(editWorkflowStateView.getId());
        WorkflowState toSave = editWorkflowStateView.toEntity(existingState.orElse(null), stateRepository);
        stateRepository.save(toSave);

        return REDIRECT_TO_WORKFLOW_ADMIN + WORKFLOW_REDIRECT_PARAM + editWorkflowStateView.getWorkflowName();
    }

    @GetMapping("/delete-workflow-state")
    String deleteWorkflowState(@RequestParam String workflowStateId) {
        Optional<WorkflowState> workflowState = stateRepository.findById(Long.parseLong(workflowStateId));
        AtomicReference<String> workflowName = new AtomicReference<>(null);
        workflowState.ifPresent(state -> {
            workflowName.set(state.getWorkflowName());
            stateRepository.delete(state);
        });

        return REDIRECT_TO_WORKFLOW_ADMIN + (workflowName.get() != null ? WORKFLOW_REDIRECT_PARAM + workflowName.get() : "");
    }

    @ModelAttribute("allOrgaRoles")
    List<OrgaRoleType> orgaRoleTypeList() {
        return Arrays.stream(OrgaRoleType.values()).collect(Collectors.toList());
    }
}
