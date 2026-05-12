package com.lootSafe.repository;

import com.lootSafe.model.EmailDetails;

public interface EmailService {

    String enviarMailSimples(EmailDetails details);
}
