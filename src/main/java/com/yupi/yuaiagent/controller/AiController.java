package com.yupi.yuaiagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yupi.yuaiagent.agent.YuManus;
import com.yupi.yuaiagent.app.LoveApp;
import com.yupi.yuaiagent.context.UserContext;
import com.yupi.yuaiagent.model.entity.Conversation;
import com.yupi.yuaiagent.repository.ChatHistoryRepository;
import com.yupi.yuaiagent.service.ConversationService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * AI 对话控制器
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ConversationService conversationService;

    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * SSE 流式调用 AI 恋爱大师应用
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * 流式调用 Manus 超级智能体
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, String chatId) {
        Long userId = UserContext.getUserId();
        // 合并本地工具 + MCP 远程工具
        ToolCallback[] mergedTools = Stream.concat(
                Arrays.stream(allTools),
                Arrays.stream(toolCallbackProvider.getToolCallbacks())
        ).toArray(ToolCallback[]::new);
        YuManus yuManus = new YuManus(mergedTools, dashscopeChatModel);
        yuManus.setUserId(userId);

        // 1. 自动更新会话标题（仿 LoveApp，仅第一条消息时触发）
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getChatId, chatId);
        Conversation conv = conversationService.getOne(wrapper);
        if (conv != null && "新对话".equals(conv.getTitle())) {
            String title = message.length() > 20 ? message.substring(0, 20) : message;
            conv.setTitle(title);
            conversationService.updateById(conv);
        }

        // 2. 设置每步输出的实时写入回调
        yuManus.onStepOutput((role, content) -> {
            chatHistoryRepository.saveStepOutput(userId, chatId, role, content);
        });

        // 3. 设置 Agent 结束时的日志回调
        yuManus.setOnComplete(messages -> {
            chatHistoryRepository.appendManusMessages(userId, chatId, messages, yuManus.getNextStepPrompt());
        });

        // 4. 启动 Agent
        return yuManus.runStream(message);
    }
}
