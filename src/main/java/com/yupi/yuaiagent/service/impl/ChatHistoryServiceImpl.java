package com.yupi.yuaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yuaiagent.mapper.ChatHistoryMapper;
import com.yupi.yuaiagent.model.entity.ChatHistory;
import com.yupi.yuaiagent.service.ChatHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天历史服务实现
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
        implements ChatHistoryService {

    /**
     * 根据用户和会话ID查询聊天记录，按时间升序排列
     */
    @Override
    public List<ChatHistory> getByChatId(Long userId, String chatId) {
        return list(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, chatId)
                .orderByAsc(ChatHistory::getId));
    }

    /**
     * 根据用户和会话ID删除所有聊天记录
     */
    @Override
    public void removeByChatId(Long userId, String chatId) {
        remove(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, chatId));
    }
}
