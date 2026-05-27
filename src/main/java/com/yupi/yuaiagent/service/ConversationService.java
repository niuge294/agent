package com.yupi.yuaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yuaiagent.model.entity.Conversation;

public interface ConversationService extends IService<Conversation> {

    /**
     * 删除会话及其所有聊天记录
     */
    void deleteByIdWithHistory(Long id, Long userId);
}
