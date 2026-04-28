package com.itesm.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AssistantRequestDto {

    @NotEmpty
    @Size(max = 20)
    @Valid
    private List<AssistantMessageDto> messages;

    private PatientContextDto patientContext;

    public List<AssistantMessageDto> getMessages() { return messages; }
    public void setMessages(List<AssistantMessageDto> messages) { this.messages = messages; }

    public PatientContextDto getPatientContext() { return patientContext; }
    public void setPatientContext(PatientContextDto patientContext) { this.patientContext = patientContext; }
}
