package com.itesm.application.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDiagnosisEvaluationDto {

    @NotBlank
    private String patientFullName;

    @NotBlank
    private String birthDate;

    @NotBlank
    private String sex;

    @NotBlank
    private String symptomsText;

    public String getPatientFullName() { return patientFullName; }
    public void setPatientFullName(String patientFullName) { this.patientFullName = patientFullName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getSymptomsText() { return symptomsText; }
    public void setSymptomsText(String symptomsText) { this.symptomsText = symptomsText; }
}
