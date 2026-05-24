package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.YuManus;
import com.yupi.yuaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

//    同步调用AI 恋爱大师应用
//    @GetMapping("/love_app/chat/sync")
//    public String doChatWithLoveAppSync(String message, String chatId) {
//        return loveApp.doChat(message, chatId);
//    }

    /**
     * SSE 流式调用 AI 恋爱大师应用

     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

//    /**
//     * SSE 流式调用 AI 恋爱大师应用（闲置）
//     */
//    @GetMapping(value = "/love_app/chat/server_sent_event")
//    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
//        return loveApp.doChatByStream(message, chatId)
//                .map(chunk -> ServerSentEvent.<String>builder()
//                        .data(chunk)
//                        .build());
//    }
//
//    /**
//     * SSE 流式调用 AI 恋爱大师应用（闲置）
//     */
//    @GetMapping(value = "/love_app/chat/sse_emitter")
//    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId) {
//        SseEmitter sseEmitter = new SseEmitter(180000L);
//        loveApp.doChatByStream(message, chatId)
//                .subscribe(chunk -> {
//                    try {
//                        sseEmitter.send(chunk);
//                    } catch (IOException e) {
//                        sseEmitter.completeWithError(e);
//                    }
//                }, sseEmitter::completeWithError, sseEmitter::complete);
//        return sseEmitter;
//    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }
}
