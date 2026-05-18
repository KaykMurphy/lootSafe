package com.lootsafe.model;

import com.lootsafe.enums.MessageAuthor;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Offer offer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageAuthor messageAuthor;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime sentAt;

    @PrePersist
    public void onPrePersist() {
        this.sentAt = LocalDateTime.now();
    }

}
