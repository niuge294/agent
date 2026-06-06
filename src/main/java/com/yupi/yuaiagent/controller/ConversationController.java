package com.yupi.yuaiagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.yuaiagent.context.UserContext;
import com.yupi.yuaiagent.model.entity.ChatHistory;
import com.yupi.yuaiagent.model.entity.Conversation;
import com.yupi.yuaiagent.service.ChatHistoryService;
import com.yupi.yuaiagent.service.ConversationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话管理接口
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatHistoryService chatHistoryService;

    public ConversationController(ConversationService conversationService,
                                  ChatHistoryService chatHistoryService) {
        this.conversationService = conversationService;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * 获取当前用户的会话列表，按最近更新时间倒序
     */
    @GetMapping("/list")
    public List<Conversation> list() {
        Long userId = UserContext.getUserId();
        return conversationService.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdateTime));
    }

    /**
     * 创建新会话（标题默认为"新对话"，第一条消息后由后端自动更新）
     */
    @PostMapping("/create")
    public Conversation create(String chatId, String title) {
        Long userId = UserContext.getUserId();
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setChatId(chatId);
        conv.setTitle(title != null ? title : "新对话");
        conversationService.save(conv);
        return conv;
    }

    /**
     * 删除指定会话及其所有聊天记录
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        conversationService.deleteByIdWithHistory(id, UserContext.getUserId());
    }

    /**
     * 查询指定会话的聊天历史记录
     */
    @GetMapping("/{chatId}/history")
    public List<ChatHistory> history(@PathVariable String chatId) {
        Long userId = UserContext.getUserId();
        return chatHistoryService.getByChatId(userId, chatId);
    }

    /**
     * 获取当前用户的 Manus 会话列表（仅 agent_ 前缀的会话）
     */
    @GetMapping("/manus/list")
    public List<Conversation> manusList() {
        Long userId = UserContext.getUserId();
        return conversationService.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .likeRight(Conversation::getChatId, "agent_")
                .orderByDesc(Conversation::getUpdateTime));
    }

    /**
     * 查询 Manus 会话的聊天记录（仅返回 user 和 assistant，排除 tool 消息）
     */
    @GetMapping("/manus/{chatId}/history")
    public List<ChatHistory> manusHistory(@PathVariable String chatId) {
        Long userId = UserContext.getUserId();
        return chatHistoryService.list(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, chatId)
                .in(ChatHistory::getRole, "user", "assistant")
                .orderByAsc(ChatHistory::getId));
    }
}
