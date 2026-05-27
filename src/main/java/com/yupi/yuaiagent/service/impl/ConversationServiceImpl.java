package com.yupi.yuaiagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuaiagent.mapper.ConversationMapper;
import com.yupi.yuaiagent.model.entity.Conversation;
import com.yupi.yuaiagent.service.ChatHistoryService;
import com.yupi.yuaiagent.service.ConversationService;
import org.springframework.stereotype.Service;

/**
 * 会话服务实现
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {

    private final ChatHistoryService chatHistoryService;

    public ConversationServiceImpl(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * 删除会话及其所有聊天记录
     */
    @Override
    public void deleteByIdWithHistory(Long id, Long userId) {
        Conversation conv = getById(id);
        if (conv == null) return;
        chatHistoryService.removeByChatId(userId, conv.getChatId());
        removeById(id);
    }
}
