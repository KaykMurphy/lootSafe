package com.lootSafe.repository;

import com.lootSafe.model.MensagemChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, UUID> {

    List<MensagemChat> findByOfertaIdOrderByDataEnvioAsc(UUID ofertaId);
}
