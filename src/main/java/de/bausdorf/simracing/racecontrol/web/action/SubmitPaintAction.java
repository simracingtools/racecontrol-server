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
import de.bausdorf.simracing.racecontrol.orga.model.TeamRegistration;
import de.bausdorf.simracing.racecontrol.util.FileTypeEnum;
import de.bausdorf.simracing.racecontrol.util.UploadFileManager;
import de.bausdorf.simracing.racecontrol.web.EventOrganizer;
import de.bausdorf.simracing.racecontrol.web.model.orga.WorkflowActionEditView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component("PAINT_SUBMITTED")
@Slf4j
public class SubmitPaintAction extends WorkflowAction {

    private static final String TGA_PREFIX = "car_team_";
    private static final String SPEC_PREFIX = "car_spec_team_";

    private final UploadFileManager fileManager;
    private final DocumentMetadataRepository documentRepository;

    public SubmitPaintAction(@Autowired EventOrganizer eventOrganizer,
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
            TeamRegistration registration = getEventOrganizer().getTeamRegistration(editView.getWorkflowItemId());
            if(registration == null) {
                throw new ActionException("Team id " + editView.getWorkflowItemId() + " not found");
            }
            if(editView.getPaintTga() == null) {
                throw new ActionException("No paint submitted");
            }

            try {
                DocumentMetadata metadata = createDocumentData(registration, actor, editView.getPaintTga(), TGA_PREFIX, ".tga");
                String uri = fileManager.uploadEventFile(editView.getPaintTga(), editView.getEventId().toString(), FileTypeEnum.PAINT,
                        metadata.getFileName());
                metadata.setFileUrl(uri);
                documentRepository.save(metadata);
            } catch(IOException e) {
                throw new ActionException(e.getMessage(), e);
            }

            if(!editView.getPaintSpecMap().isEmpty()) {
                try {
                    DocumentMetadata metadata = createDocumentData(registration, actor, editView.getPaintSpecMap(), SPEC_PREFIX,".mip");
                    String uri = fileManager.uploadEventFile(editView.getPaintSpecMap(), editView.getEventId().toString(), FileTypeEnum.PAINT,
                            metadata.getFileName());
                    metadata.setFileUrl(uri);
                    documentRepository.save(metadata);
                } catch(IOException e) {
                    throw new ActionException(e.getMessage(), e);
                }
            }
            getEventOrganizer().updateCurrentAction(currentAction, actor, editView);
            getEventOrganizer().createFollowUpAction(currentAction, actor, editView.getDueDate());
        } else {
            throw new ActionException("Current action not found");
        }
    }

    private DocumentMetadata createDocumentData(TeamRegistration registration, Person actor, MultipartFile file, String prefix, String extension) {
        Optional<DocumentMetadata> existingDocument = documentRepository
                .findAllByEventIdAndDocumentTypeAndRefItemId(registration.getEventId(), FileTypeEnum.PAINT, registration.getId())
                .stream().filter(d -> d.getFileName().endsWith(extension)).findFirst();

        if(existingDocument.isPresent()) {
            existingDocument.get().setLastChanged(OffsetDateTime.now());
            existingDocument.get().setOriginalFileName(file.getOriginalFilename());
            existingDocument.get().setVersion(existingDocument.get().getVersion() + 1);
            return existingDocument.get();
        } else {
            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setCreatedBy(actor);
            metadata.setCreatedAt(OffsetDateTime.now());
            metadata.setLastChanged(OffsetDateTime.now());
            metadata.setDocumentType(FileTypeEnum.PAINT);
            metadata.setEventId(registration.getEventId());
            metadata.setOriginalFileName(file.getOriginalFilename());
            metadata.setFileName(prefix + registration.getIracingId() + extension);
            metadata.setRefItemId(registration.getId());
            metadata.setVersion(1);
            return metadata;
        }
    }
}
