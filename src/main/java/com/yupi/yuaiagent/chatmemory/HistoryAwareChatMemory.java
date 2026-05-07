package com.yupi.yuaiagent.chatmemory;

import com.yupi.yuaiagent.repository.ChatHistoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.Collections;
import java.util.List;

public class HistoryAwareChatMemory implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;
    private final ChatHistoryRepository historyRepo;

    public HistoryAwareChatMemory(ChatMemoryRepository delegate, ChatHistoryRepository historyRepo) {
        this.delegate = delegate;
        this.historyRepo = historyRepo;
    }

    @Override
    public List<String> findConversationIds() {
        return delegate.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<Message> oldMessages = delegate.findByConversationId(conversationId);
        int oldSize = oldMessages.size();
        if (messages.size() > oldSize) {
            List<Message> newMessages = messages.subList(oldSize, messages.size());
            historyRepo.append(conversationId, newMessages);
        }
        delegate.saveAll(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }
}
