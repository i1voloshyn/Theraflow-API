package dev.ivanvoloshyn.theraflowapi.event;

import dev.ivanvoloshyn.theraflowapi.email.EmailService;
import dev.ivanvoloshyn.theraflowapi.event.model.EmailVerificationRequest;
import dev.ivanvoloshyn.theraflowapi.event.model.PasswordResetRequestEvent;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailVerificationEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendVerificationEmail(EmailVerificationRequest request) {
        try {
            emailService.sendVerificationEmail(request.email(), request.token());
        } catch (EmailDeliveryException e) {
            log.error(
                    "Verification email delivery failed for account {}",
                    request.accountId(),
                    e
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPasswordResetEmailRequest(PasswordResetRequestEvent request) {
        try {
            emailService.sendVerificationEmail(request.email(), request.token());
        } catch (EmailDeliveryException e) {
            log.error(
                    "Verification email delivery failed for account {}",
                  //  request.accountId(),
                    e
            );
        }
    }
}
