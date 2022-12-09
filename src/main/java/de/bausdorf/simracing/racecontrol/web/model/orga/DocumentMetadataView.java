package de.bausdorf.simracing.racecontrol.web.model.orga;

import de.bausdorf.simracing.racecontrol.orga.model.DocumentMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadataView {
    long id;

    private long eventId;
    private String originalFileName;
    private String fileName;
    private String createdAt;
    private String lastChanged;
    private PersonView createdBy;
    private String documentType;
    private long refItemId;
    private String teamName;
    private String fileUrl;
    private long version;

    public static DocumentMetadataView fromEntity(DocumentMetadata entity) {
        return DocumentMetadataView.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .createdAt(entity.getCreatedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                .lastChanged(entity.getLastChanged().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                .createdBy(PersonView.fromEntity(entity.getCreatedBy()))
                .documentType(entity.getDocumentType().toString())
                .refItemId(entity.getRefItemId())
                .fileUrl(entity.getFileUrl())
                .originalFileName(entity.getOriginalFileName())
                .fileName(entity.getFileName())
                .version(entity.getVersion())
                .build();
    }
}
