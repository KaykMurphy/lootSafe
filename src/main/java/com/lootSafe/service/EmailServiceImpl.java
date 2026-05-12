package com.lootSafe.service;

import com.lootSafe.model.EmailDetails;
import com.lootSafe.repository.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Override
    @Async("webhookTaskExecutor")
    public String enviarMailSimples(EmailDetails details) {

        try {
            SimpleMailMessage mailMessage =
                    new SimpleMailMessage();

            mailMessage.setFrom(remetente);
            mailMessage.setTo(details.destinatario());
            mailMessage.setText(details.corpoMensagem());
            mailMessage.setSubject(details.assunto());

            javaMailSender.send(mailMessage);

            return "Mail Sent Successfully";

        } catch (Exception e) {
            return "Error while sending mail";

        }

    }
}

