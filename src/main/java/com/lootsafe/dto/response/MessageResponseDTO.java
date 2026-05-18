package com.lootsafe.dto.response;

import com.lootsafe.enums.MessageAuthor;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponseDTO (

        UUID id,
        MessageAuthor author,
        String messageText,
        LocalDateTime sentAt
)
{ }
