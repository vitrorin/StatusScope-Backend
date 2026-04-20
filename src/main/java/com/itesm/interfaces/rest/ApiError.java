package com.itesm.interfaces.rest;

public class ApiError {
    private int status;
    private String code;
    private String message;

    public ApiError() {
    }

    public ApiError(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
