package com.yupi.yuaiagent.chatmemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yupi.yuaiagent.chatmemory.model.MessageRecord;
import com.yupi.yuaiagent.repository.ChatHistoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedisChatMemory implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:mem:";
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatHistoryRepository historyRepo;

    public RedisChatMemory(RedisTemplate<String, String> redisTemplate,
                           ObjectMapper objectMapper,
                           ChatHistoryRepository historyRepo) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.historyRepo = historyRepo;
    }

    @Override
    public List<String> findConversationIds() {
        return Collections.emptyList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > 0) {
            redisTemplate.expire(key, TTL);
            return deserializeMessages(redisTemplate.opsForList().range(key, 0, -1));
        }
        List<Message> dbMessages = historyRepo.findLastN(conversationId, 20);
        if (!dbMessages.isEmpty()) {
            saveAll(conversationId, dbMessages);
        }
        return dbMessages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        List<String> jsonList = messages.stream()
                .map(this::toJson)
                .toList();
        redisTemplate.delete(key);
        redisTemplate.opsForList().rightPushAll(key, jsonList);
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    private String toJson(Message message) {
        try {
            MessageRecord record = MessageRecord.fromMessage(message);
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    private List<Message> deserializeMessages(List<String> jsonList) {
        if (jsonList == null || jsonList.isEmpty()) {
            return new ArrayList<>();
        }
        return jsonList.stream()
                .map(json -> {
                    try {
                        MessageRecord record = objectMapper.readValue(json, MessageRecord.class);
                        return record.toMessage();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("反序列化消息失败", e);
                    }
                })
                .toList();
    }
}
