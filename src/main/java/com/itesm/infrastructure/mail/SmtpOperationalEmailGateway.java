package com.itesm.infrastructure.mail;

import com.itesm.application.port.out.OperationalEmailGateway;
import com.itesm.application.port.out.OperationalEmailMessage;
import com.itesm.application.port.out.OperationalEmailResult;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SmtpOperationalEmailGateway implements OperationalEmailGateway {

    @Inject Mailer mailer;

    @ConfigProperty(name = "statusscope.mail.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "statusscope.mail.from", defaultValue = "StatuScope <no-reply@statusscope.local>")
    String from;

    @Override
    public OperationalEmailResult send(OperationalEmailMessage message) {
        if (!enabled) {
            return new OperationalEmailResult(false, "Email delivery disabled by statusscope.mail.enabled.");
        }
        if (message == null || isBlank(message.to())) {
            return new OperationalEmailResult(false, "No email recipient was provided.");
        }
        try {
            mailer.send(Mail.withText(message.to(), message.subject(), message.body()).setFrom(from));
            return new OperationalEmailResult(true, "Email sent to " + message.to());
        } catch (Exception ex) {
            return new OperationalEmailResult(false, "Email delivery failed: " + ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
