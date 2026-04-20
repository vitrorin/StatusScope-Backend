package com.itesm.application.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRoleDto {
    @NotBlank
    private String roleCode;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
