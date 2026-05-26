package com.lootsafe.controller;

import com.lootsafe.dto.request.MessageRequestDTO;
import com.lootsafe.dto.response.MessageResponseDTO;
import com.lootsafe.service.ChatService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@RestController
@AllArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/api/chat/history")
    public List<MessageResponseDTO> getHistory() {
        return chatService.getLatestMessages(50);
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public MessageResponseDTO broadcastMessage(MessageRequestDTO input) {
        String safeMessage = HtmlUtils.htmlEscape(input.messageText());
        MessageRequestDTO safeInput = new MessageRequestDTO(input.author(), safeMessage, input.sentAt(), input.messageType());

        return chatService.savePublicMessage(safeInput);
    }
}
