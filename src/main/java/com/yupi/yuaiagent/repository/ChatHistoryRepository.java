package com.yupi.yuaiagent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.chatmemory.model.MessageRecord;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChatHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ChatHistoryRepository(JdbcTemplate jdbcTemplate,
                                 @Qualifier("chatMemoryObjectMapper") ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        ensureTableExists();
        ensureAgentMessageTableExists();
    }

    private void ensureAgentMessageTableExists() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_message (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    conversation_id VARCHAR(64) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    content TEXT,
                    metadata_json TEXT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_conv (user_id, conversation_id)
                )
                """);
        // 存量表补充 metadata_json 列
        try {
            jdbcTemplate.execute("ALTER TABLE agent_message ADD COLUMN metadata_json TEXT NULL");
        } catch (Exception ignored) {
            // 列已存在，忽略
        }
    }

    private void ensureTableExists() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    conversation_id VARCHAR(64) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    content TEXT,
                    metadata_json TEXT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_conv (user_id, conversation_id)
                )
                """);
    }

    public void append(String conversationId, List<Message> messages) {
        ParsedId parsed = parseConversationId(conversationId);
        if (parsed.userId == null) {
            return;
        }
        String sql = "INSERT INTO chat_history (user_id, conversation_id, role, content, metadata_json, create_time) VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Message msg : messages) {
            MessageRecord record = MessageRecord.fromMessage(msg);
            String metadataJson = toJson(record.getMetadata());
            batchArgs.add(new Object[]{
                    parsed.userId, parsed.realId, record.getRole(), record.getContent(),
                    metadataJson, Timestamp.valueOf(now)
            });
        }
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    /**
     * 持久化 Manus Agent 的完整对话消息到 agent_message 表即生成日志表
     * 排除系统注入的 nextStepPrompt（伪装成 UserMessage 的规划指令）
     */
    public void appendManusMessages(Long userId, String chatId, List<Message> messages, String nextStepPrompt) {
        String sql = "INSERT INTO agent_message (user_id, conversation_id, role, content, metadata_json, create_time) VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Message msg : messages) {
            // 跳过系统注入的 nextStepPrompt
            if (nextStepPrompt != null && msg instanceof UserMessage && nextStepPrompt.equals(msg.getText())) {
                continue;
            }
            // ToolResponseMessage 需要从 responseData 提取内容
            String role = msg.getMessageType().name().toLowerCase();
            String content;
            if (msg instanceof ToolResponseMessage trm) {
                content = trm.getResponses().stream()
                        .map(r -> r.responseData())
                        .collect(Collectors.joining("\n"));
            } else {
                content = msg.getText();
            }
            String metadataJson = toJson(msg.getMetadata());
            batchArgs.add(new Object[]{
                    userId, chatId, role, content, metadataJson,
                    Timestamp.valueOf(now)
            });
        }
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    public List<Message> findLastN(String conversationId, int n) {
        ParsedId parsed = parseConversationId(conversationId);
        if (parsed.userId == null) {
            return Collections.emptyList();
        }
        String sql = "SELECT role, content, metadata_json FROM chat_history WHERE user_id = ? AND conversation_id = ? ORDER BY id DESC LIMIT ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parsed.userId, parsed.realId, n);
        List<Message> messages = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            messages.add(rowToMessage(rows.get(i)));
        }
        return messages;
    }

    /**
     * 实时写入 Agent 每一步的前端可见内容
     */
    public void saveStepOutput(Long userId, String chatId, String role, String content) {
        jdbcTemplate.update(
                "INSERT INTO chat_history (user_id, conversation_id, role, content, create_time) VALUES (?, ?, ?, ?, NOW())",
                userId, chatId, role, content);
    }

    private ParsedId parseConversationId(String conversationId) {
        int split = conversationId.indexOf(':');
        if (split > 0) {
            return new ParsedId(Long.parseLong(conversationId.substring(0, split)),
                    conversationId.substring(split + 1));
        }
        return new ParsedId(null, conversationId);
    }

    private record ParsedId(Long userId, String realId) {}

    private Message rowToMessage(Map<String, Object> row) {
        String role = (String) row.get("role");
        String content = (String) row.get("content");
        String metadataJson = (String) row.get("metadata_json");
        Map<String, Object> metadata = fromJson(metadataJson);
        MessageRecord record = new MessageRecord(role, content, metadata);
        return record.toMessage();
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}
