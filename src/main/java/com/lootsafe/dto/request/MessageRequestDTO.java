package com.lootsafe.dto.request;

import com.lootsafe.enums.MessageAuthor;
import com.lootsafe.enums.MessageType;

import java.time.LocalDateTime;

public record MessageRequestDTO(

        MessageAuthor author,
        String messageText,
        LocalDateTime sentAt,
        MessageType messageType
) {

}
