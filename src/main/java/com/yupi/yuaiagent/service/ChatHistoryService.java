package com.yupi.yuaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuaiagent.model.entity.ChatHistory;

import java.util.List;

public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 根据用户ID和会话ID查询聊天记录，按时间升序
     */
    List<ChatHistory> getByChatId(Long userId, String chatId);

    /**
     * 根据用户ID和会话ID删除所有聊天记录
     */
    void removeByChatId(Long userId, String chatId);
}
