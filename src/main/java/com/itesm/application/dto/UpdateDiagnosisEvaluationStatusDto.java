package com.itesm.application.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDiagnosisEvaluationStatusDto {

    @NotBlank
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
