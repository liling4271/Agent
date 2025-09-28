package com.liling.Agent.controller;

import com.liling.Agent.service.DeepSeekAgentService;
import com.liling.Agent.service.IntelligentToolService;
import com.liling.Agent.service.OllamaService;
import com.liling.Agent.service.SmartAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private SmartAgentService smartAgentService;

    @Autowired
    private IntelligentToolService toolService;

    /**
     * 智能对话接口
     */
    @PostMapping("/chat")
    public SmartAgentService.AgentResponse chat(@RequestBody ChatRequest request) {
        return smartAgentService.processMessage(request.getMessage());
    }

    /**
     * 获取可用工具列表
     */
    @GetMapping("/tools")
    public Map<String, String> getTools() {
        return toolService.getAvailableTools();
    }

    /**
     * 测试工具调用
     */
    @PostMapping("/test-tool")
    public String testTool(@RequestBody ToolTestRequest request) {
        return toolService.executeTool(request.getToolName(), request.getParameters());
    }

    // 请求类
    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ToolTestRequest {
        private String toolName;
        private Map<String, String> parameters;

        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }

        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    }
}
