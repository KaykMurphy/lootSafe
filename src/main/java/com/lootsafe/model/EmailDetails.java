package com.lootsafe.model;


public record EmailDetails(
        String recipient,
        String subject,
        String body
) {
}
