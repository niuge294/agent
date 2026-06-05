package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 鱼皮的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class YuManus extends ToolCallAgent {

    public YuManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("yuManus");
        String SYSTEM_PROMPT = """
               你是全能型 AI 助手 YuManus，致力于完成用户提出的各类任务，可调用各类可用工具高效处理复杂需求。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                根据用户需求，主动挑选最合适的单个工具或工具组合。
                 遇到复杂任务时，拆解问题，分步调用不同工具完成处理。
                  每调用完一个工具后，清晰说明执行结果并给出后续操作建议。
                  如果感觉自己输出的内容相比上次没什么变化，或者思考不出下一步决策，就结束本轮对话。
                   如需在任意节点结束本轮对话，请调用terminate终止工具。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
