package com.itesm.application.port.out;

public interface OperationalEmailGateway {
    OperationalEmailResult send(OperationalEmailMessage message);
}
