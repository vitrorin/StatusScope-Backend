package com.itesm.application.port.out;

public record OperationalEmailMessage(String to, String subject, String body) {
}
