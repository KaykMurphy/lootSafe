package com.lootsafe.dto.request;

import com.lootsafe.enums.MessageAuthor;

public record MessageRequestDTO(

        MessageAuthor messageAuthor,
        String messageText
) {

}
