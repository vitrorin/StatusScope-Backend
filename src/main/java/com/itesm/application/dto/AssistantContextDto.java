package com.itesm.application.dto;

import java.util.List;

public class AssistantContextDto {
    private String stateName;
    private List<OutbreakSummaryDto> outbreaks;

    public AssistantContextDto() {}

    public AssistantContextDto(String stateName, List<OutbreakSummaryDto> outbreaks) {
        this.stateName = stateName;
        this.outbreaks = outbreaks;
    }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public List<OutbreakSummaryDto> getOutbreaks() { return outbreaks; }
    public void setOutbreaks(List<OutbreakSummaryDto> outbreaks) { this.outbreaks = outbreaks; }
}
