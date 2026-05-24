package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.context.UserContext;
import com.yupi.yuaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    public LoveApp(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 基础对话：记忆 + RAG 知识库 + 工具 + MCP + 流式调用
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        Long userId = UserContext.getUserId();
        String fullChatId = userId != null ? userId + ":" + chatId : chatId;
        String rewritten = queryRewriter.doQueryRewrite(message);
        return chatClient
                .prompt()
                .user(rewritten)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, fullChatId))
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                        pgVectorVectorStore, null))
                .toolCallbacks(allTools)
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .stream()
                .content();
    }
//    AI 基础对话（支持多轮对话记忆）
//    public String doChat(String message, String chatId) {
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .call()
//                .chatResponse();
//        return chatResponse.getResult().getOutput().getText();
//    }
//    rag调用
//    public String doChatWithRag(String message, String chatId) {
//        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(rewrittenMessage)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                pgVectorVectorStore, null
//                        )
//                )
//                .call()
//                .chatResponse();
//        return chatResponse.getResult().getOutput().getText();
//    }
//    方法调用
//    public String doChatWithTools(String message, String chatId) {
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .toolCallbacks(allTools)
//                .call()
//                .chatResponse();
//        return chatResponse.getResult().getOutput().getText();
//    }
//    mcp调用
//    public String doChatWithMcp(String message, String chatId) {
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .toolCallbacks(toolCallbackProvider)
//                .call()
//                .chatResponse();
//        return chatResponse.getResult().getOutput().getText();
//    }
//
//    结构化输出貌似没用
//    record LoveReport(String title, List<String> suggestions) {}
//
//    public LoveReport doChatWithReport(String message, String chatId) {
//        LoveReport loveReport = chatClient
//                .prompt()
//                .system("每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
//                .user(message)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                .call()
//                .entity(LoveReport.class);
//        log.info("loveReport: {}", loveReport);
//        return loveReport;
//    }
//
//    @Resource
//    内存存储
//    private VectorStore loveAppVectorStore;
//
//    @Resource
//    阿里云知识库
//    private Advisor loveAppRagCloudAdvisor;
}
