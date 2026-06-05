package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具（作用是让自主规划智能体能够合理地中断）
 */
public class TerminateTool {

    @Tool(description = """
            当用户需求已全部完成，或是助手无法继续推进任务时，终止本次交互。
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
