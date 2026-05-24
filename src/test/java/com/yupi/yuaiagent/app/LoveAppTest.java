package com.yupi.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChatByStream() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮";
        String answer = loveApp.doChatByStream(message, chatId)
                .collectList()
                .block()
                .stream()
                .reduce("", String::concat);
        Assertions.assertNotNull(answer);
    }
}
