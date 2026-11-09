package dev.ivanvoloshyn.theraflowapi.exception.exceptions;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String recipient, Throwable cause) {
        super(String.format("Failed to send email to [%s].", recipient), cause);
    }
}
