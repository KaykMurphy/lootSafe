package com.lootsafe.model;

import com.lootsafe.enums.MessageAuthor;
import com.lootsafe.enums.MessageType;
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
    private MessageAuthor author;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    private LocalDateTime sentAt;

    @PrePersist
    public void onPrePersist() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

}
