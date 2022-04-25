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
public class DiscordItemView {
    public enum DiscordItemType {
        TEXT_CHANNEL,
        VOICE_CHANNEL,
        CATEGORY
    }

    private long id;
    private long categoryId;
    private String name;
    private DiscordItemType type;
    private List<DiscordItemView> children;
    private boolean inEvent;
}
