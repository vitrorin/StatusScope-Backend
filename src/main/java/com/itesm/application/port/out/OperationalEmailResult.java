package com.itesm.application.port.out;

public record OperationalEmailResult(boolean sent, String detail) {
    public String status() {
        return sent ? "SENT" : "FAILED";
    }
}
