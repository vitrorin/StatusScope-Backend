package com.itesm.application.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateHospitalDto {
    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String address;
    private String phone;
    private String inviteCode;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
