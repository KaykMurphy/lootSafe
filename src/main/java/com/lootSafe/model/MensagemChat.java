package com.lootSafe.model;

import com.lootSafe.enums.AutorMensagem;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class MensagemChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Oferta oferta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AutorMensagem autorMensagem;

    @Column(columnDefinition = "TEXT")
    private String texto;

    private LocalDateTime dataEnvio;

    @PrePersist
    public void onPrePersist() {
        this.dataEnvio = LocalDateTime.now();
    }

}
