package com.lootsafe.service;

import com.lootsafe.model.EmailDetails;
import com.lootsafe.repository.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Override
    @Async("webhookTaskExecutor")
    public void sendSimpleEmail(EmailDetails details) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(details.recipient());
            message.setSubject(details.subject());
            message.setText(details.body());

            mailSender.send(message);
            log.info("E-mail enviado com sucesso para {}", details.recipient());
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail para {}: {}", details.recipient(), e.getMessage());
        }
    }
}
