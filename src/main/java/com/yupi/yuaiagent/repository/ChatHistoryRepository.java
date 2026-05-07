package com.yupi.yuaiagent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.chatmemory.model.MessageRecord;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChatHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ChatHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        ensureTableExists();
    }

    private void ensureTableExists() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    conversation_id VARCHAR(64) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    content TEXT,
                    metadata_json TEXT,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_conversation_id (conversation_id)
                )
                """);
    }

    public void append(String conversationId, List<Message> messages) {
        String sql = "INSERT INTO chat_history (conversation_id, role, content, metadata_json, create_time) VALUES (?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Message msg : messages) {
            MessageRecord record = MessageRecord.fromMessage(msg);
            String metadataJson = toJson(record.getMetadata());
            batchArgs.add(new Object[]{
                    conversationId, record.getRole(), record.getContent(),
                    metadataJson, Timestamp.valueOf(now)
            });
        }
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    public List<Message> findLastN(String conversationId, int n) {
        String sql = "SELECT role, content, metadata_json FROM chat_history WHERE conversation_id = ? ORDER BY id DESC LIMIT ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, conversationId, n);
        List<Message> messages = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            messages.add(rowToMessage(rows.get(i)));
        }
        return messages;
    }

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
