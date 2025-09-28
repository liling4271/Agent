package com.liling.Agent.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeepSeekAgentService {

    private final OllamaService ollamaService;

    // 模拟工具函数库
    private final Map<String, String> availableTools = new HashMap<>();

    public DeepSeekAgentService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
        initializeTools();
    }

    private void initializeTools() {
        availableTools.put("calculator", "执行数学计算");
        availableTools.put("time", "获取当前时间");
        availableTools.put("weather", "查询天气信息（模拟）");
        availableTools.put("file_operation", "文件操作（模拟）");
    }

    /**
     * 智能对话，自动识别是否需要工具调用
     */
    public String intelligentChat(String userMessage) {
        // 先检查是否是工具调用请求
        String toolResponse = handleToolRequest(userMessage);
        if (toolResponse != null) {
            return toolResponse;
        }

        // 如果不是工具调用，直接与 AI 对话
        return ollamaService.chatWithDeepSeek(userMessage);
    }

    /**
     * 处理工具调用请求
     */
    private String handleToolRequest(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.contains("计算") || lowerMessage.contains("算一下") ||
                lowerMessage.matches(".*\\d+[+\\-*/]\\d+.*")) {
            return executeCalculator(userMessage);
        }

        if (lowerMessage.contains("时间") || lowerMessage.contains("现在几点")) {
            return executeTimeQuery();
        }

        if (lowerMessage.contains("天气")) {
            return executeWeatherQuery(userMessage);
        }

        if (lowerMessage.contains("写")) {
            return executeWeatherQuery(userMessage);
        }

        return null; // 不是工具调用
    }

    private String executeCalculator(String expression) {
        try {
            // 简单的数学表达式解析
            String mathExpr = expression.replaceAll("[^0-9+\\-*/.()]", "");
            if (mathExpr.length() > 0) {
                // 这里可以添加更复杂的计算逻辑
                return "计算功能: 您想问的是 \"" + mathExpr + "\" 吗？";
            }
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }

        // 如果无法解析，让 AI 处理
        return null;
    }

    private String executeTimeQuery() {
        return "当前时间: " + LocalDateTime.now() + "\n" +
                "这是模拟的时间查询功能。";
    }

    private String executeWeatherQuery(String message) {
        return "天气查询: 这是模拟的天气查询功能。\n" +
                "您询问了: " + message + "\n" +
                "实际天气查询需要接入第三方API。";
    }

    /**
     * 获取可用的工具列表
     */
    public Map<String, String> getAvailableTools() {
        return new HashMap<>(availableTools);
    }
}
