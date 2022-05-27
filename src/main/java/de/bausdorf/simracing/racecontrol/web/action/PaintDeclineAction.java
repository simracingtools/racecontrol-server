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

import de.bausdorf.simracing.racecontrol.orga.model.DocumentMetadata;
import de.bausdorf.simracing.racecontrol.orga.model.DocumentMetadataRepository;
import de.bausdorf.simracing.racecontrol.orga.model.Person;
import de.bausdorf.simracing.racecontrol.util.FileTypeEnum;
import de.bausdorf.simracing.racecontrol.util.UploadFileManager;
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import de.bausdorf.simracing.racecontrol.web.model.orga.WorkflowActionEditView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

@Component("PAINT_DECLINED")
@Slf4j
public class PaintDeclineAction extends WorkflowAction {

    private final UploadFileManager fileManager;
    private final DocumentMetadataRepository documentRepository;
    public PaintDeclineAction(@Autowired EventOrganizer eventOrganizer,
                              @Autowired UploadFileManager uploadFileManager,
                              @Autowired DocumentMetadataRepository documentRepository) {
        super(eventOrganizer);
        this.fileManager = uploadFileManager;
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional
    public void performAction(WorkflowActionEditView editView, Person actor) throws ActionException {
        de.bausdorf.simracing.racecontrol.orga.model.WorkflowAction currentAction = getEventOrganizer().getWorkflowAction(editView.getId());
        if(currentAction != null) {
            List<DocumentMetadata> documents = documentRepository.deleteAllByEventIdAndDocumentTypeAndRefItemId(
                    editView.getEventId(), FileTypeEnum.PAINT, editView.getWorkflowItemId()
            );
            try {
                for(DocumentMetadata doc : documents) {
                    fileManager.deleteEventFile(doc.getEventId(), doc.getDocumentType(), doc.getFileName());
                }
            } catch (IOException e) {
                throw new ActionException(e.getMessage(), e);
            }
            getEventOrganizer().updateCurrentAction(currentAction, actor, editView);
            getEventOrganizer().createFollowUpAction(currentAction, actor, editView.getDueDate());
        } else {
            throw new ActionException("Current action not found");
        }
    }
}
