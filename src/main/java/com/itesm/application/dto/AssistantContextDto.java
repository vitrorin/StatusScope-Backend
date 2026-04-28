package com.itesm.application.dto;

import java.util.List;

public class AssistantContextDto {
    private String regionName;
    private List<OutbreakSummaryDto> outbreaks;

    public AssistantContextDto() {}

    public AssistantContextDto(String regionName, List<OutbreakSummaryDto> outbreaks) {
        this.regionName = regionName;
        this.outbreaks = outbreaks;
    }

    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }

    public List<OutbreakSummaryDto> getOutbreaks() { return outbreaks; }
    public void setOutbreaks(List<OutbreakSummaryDto> outbreaks) { this.outbreaks = outbreaks; }
}
