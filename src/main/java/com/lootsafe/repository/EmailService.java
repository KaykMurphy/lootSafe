package com.lootsafe.repository;

import com.lootsafe.model.EmailDetails;

public interface EmailService {
    void sendSimpleEmail(EmailDetails details);
}
