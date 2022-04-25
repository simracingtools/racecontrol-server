package de.bausdorf.simracing.racecontrol.web.model.orga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscordCleanupView {
    long eventId;
    long serverId;
    private List<DiscordItemView> categories;
    private List<String> selectedItems;
}
