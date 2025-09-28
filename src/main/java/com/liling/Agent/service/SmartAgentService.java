package com.liling.Agent.service;

import com.liling.Agent.model.ToolDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmartAgentService {
    @Autowired
    private IntelligentToolService toolService;

    @Autowired
    private OllamaService ollamaService;

    /**
     * 处理用户消息的完整流程
     */
    public AgentResponse processMessage(String userMessage) {
        System.out.println("处理用户消息: " + userMessage);

        // 1. 智能分析是否需要工具调用
        ToolDecision decision = toolService.analyzeWithLLM(userMessage);
        System.out.println("工具决策: " + decision);

        if (decision.isNeedsTool()) {
            // 2. 执行工具调用
            String toolResult = toolService.executeTool(
                    decision.getToolName(),
                    decision.getParameters()
            );
            System.out.println("工具执行结果: " + toolResult);

            // 3. 生成最终回复
            String finalResponse = generateFinalResponse(userMessage, toolResult, decision);

            return new AgentResponse(finalResponse, true, decision, toolResult);

        } else {
            // 4. 直接对话
            String response = ollamaService.chatWithDeepSeek(userMessage);
            return new AgentResponse(response, false, null, null);
        }
    }

    /**
     * 结合工具结果生成友好的最终回复
     */
    private String generateFinalResponse(String userMessage, String toolResult, ToolDecision decision) {
        String prompt = """
            用户原问题: %s
            工具调用结果: %s
            调用理由: %s
            
            请根据工具结果生成一个自然、友好、完整的回复，直接回答用户的问题。
            不要提及"根据工具结果"这样的字眼，要让回复看起来像是你直接知道的。
            回复语言要与用户问题语言一致。
            """.formatted(userMessage, toolResult, decision.getReasoning());

        return ollamaService.chatWithDeepSeek(prompt);
    }

    /**
     * 响应数据结构
     */
    public static class AgentResponse {
        private final String response;
        private final boolean usedTool;
        private final ToolDecision toolDecision;
        private final String toolResult;

        public AgentResponse(String response, boolean usedTool,
                             ToolDecision toolDecision, String toolResult) {
            this.response = response;
            this.usedTool = usedTool;
            this.toolDecision = toolDecision;
            this.toolResult = toolResult;
        }

        // Getters
        public String getResponse() { return response; }
        public boolean isUsedTool() { return usedTool; }
        public ToolDecision getToolDecision() { return toolDecision; }
        public String getToolResult() { return toolResult; }
    }

}
