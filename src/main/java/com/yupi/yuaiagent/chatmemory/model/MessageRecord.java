package com.yupi.yuaiagent.chatmemory.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Map;

@Data
@NoArgsConstructor
public class MessageRecord {

    private String role;
    private String content;
    private Map<String, Object> metadata;

    public MessageRecord(String role, String content, Map<String, Object> metadata) {
        this.role = role;
        this.content = content;
        this.metadata = metadata;
    }

    public static MessageRecord fromMessage(Message message) {
        MessageRecord record = new MessageRecord();
        record.setRole(message.getMessageType().name().toLowerCase());
        record.setContent(message.getText());
        record.setMetadata(message.getMetadata());
        return record;
    }

    public Message toMessage() {
        return switch (role.toLowerCase()) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            case "tool" -> new ToolResponseMessage(new ArrayList<>());
            default -> new UserMessage(content);
        };
    }
}
