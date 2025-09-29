package com.liling.Agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liling.Agent.model.ToolDecision;
import com.liling.Agent.utils.ResponseValidator;
import com.liling.Agent.utils.ToolNameNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntelligentToolService {

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private ToolNameNormalizer toolNameNormalizer; // 新增

    @Autowired
    private ResponseValidator responseValidator;

    private final Map<String, Tool> availableTools = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public IntelligentToolService(List<Tool> tools) {
        // 自动注册所有 Tool 接口的实现
        for (Tool tool : tools) {
            availableTools.put(tool.getName(), tool);
        }
        System.out.println("已注册工具: " + availableTools.keySet());
    }
    /**
     * 使用 LLM 智能判断是否需要工具调用
     */
//    public ToolDecision analyzeWithLLM(String userMessage) {
//        try {
//            String analysisPrompt = buildAnalysisPrompt(userMessage);
//            System.out.println("发送给 LLM 的分析提示: " + analysisPrompt);
//
//            String llmResponse = ollamaService.chatWithDeepSeek(analysisPrompt);
//            System.out.println("LLM 原始响应: " + llmResponse);
//
//            ToolDecision decision = parseToolDecision(llmResponse);
//            System.out.println("解析后的决策: " + decision);
//
//            return decision;
//
//        } catch (Exception e) {
//            System.err.println("LLM 分析失败: " + e.getMessage());
//            return fallbackAnalysis(userMessage);
//        }
//    }

    /**
     * 增强的 LLM 分析 - 包含自动清洗和重试
     */
    public ToolDecision analyzeWithLLM(String userMessage) {
        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("=== 第 " + attempt + " 次分析尝试 ===");

                String prompt = buildPromptForAttempt(userMessage, attempt);
                String rawResponse = ollamaService.chatWithDeepSeek(prompt);

                System.out.println("原始 LLM 响应: " + rawResponse);

                // 检查是否需要清洗
                if (responseValidator.containsCodeExample(rawResponse)) {
                    System.out.println("检测到代码示例，启动清洗流程...");
                    rawResponse = responseValidator.cleanLLMResponse(rawResponse);
                    System.out.println("清洗后响应: " + rawResponse);
                }

                ToolDecision decision = parseToolDecision(rawResponse);

                if (decision != null && isValidDecision(decision)) {
                    System.out.println("成功获得有效决策: " + decision);
                    return decision;
                }

                System.out.println("第 " + attempt + " 次尝试结果无效");

            } catch (Exception e) {
                System.err.println("第 " + attempt + " 次尝试异常: " + e.getMessage());
            }

            // 如果不是最后一次尝试，等待一下再重试
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 所有尝试都失败，使用规则降级
        System.out.println("所有 LLM 尝试失败，使用规则降级");
        return fallbackToRuleBased(userMessage);
    }

    /**
     * 根据尝试次数构建不同严格度的提示词
     */
    private String buildPromptForAttempt(String userMessage, int attempt) {
        switch (attempt) {
            case 1:
                return buildStandardPrompt(userMessage);
            case 2:
                return buildStrictPrompt(userMessage);
            case 3:
                return buildUltraStrictPrompt(userMessage);
            default:
                return buildStandardPrompt(userMessage);
        }
    }

    private String buildStandardPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请严格按照以下 JSON 格式回复，返回结果严格区分大小写，不要添加任何其他内容：\n" +
                "            {\n" +
                "                \"needs_tool\": true/false,\n" +
                "                \"tool_name\": \"工具名称\",\n" +
                "                \"parameters\": {\"参数键\": \"参数值\"},\n" +
                "                \"reasoning\": \"判断理由\"\n" +
                "            }");

        prompt.append("可用工具列表：\n");
        for (Tool tool : availableTools.values()) {
            prompt.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }

        prompt.append("\n工具调用规则：\n");
        prompt.append("- calculator: 当用户提到数字、计算、算术、数学问题时使用\n");
        prompt.append("- time: 当用户询问时间、日期、现在几点时使用\n");
        prompt.append("- weather: 当用户询问天气、气温、天气预报时使用\n");
        prompt.append("- 如果不需要工具，needs_tool 设为 false\n\n");

        prompt.append("用户输入: \"").append(userMessage).append("\"\n\n");
        return prompt.toString();
    }

    private String buildStrictPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请仔细分析用户的输入，判断是否需要调用工具，以及调用哪个工具。返回结果严格区分大小写\n\n");

        prompt.append("可用工具列表：\n");
        for (Tool tool : availableTools.values()) {
            prompt.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }

        prompt.append("\n工具调用规则：\n");
        prompt.append("- calculator: 当用户提到数字、计算、算术、数学问题时使用\n");
        prompt.append("- time: 当用户询问时间、日期、现在几点时使用\n");
        prompt.append("- weather: 当用户询问天气、气温、天气预报时使用\n");
        prompt.append("- 如果不需要工具，needs_tool 设为 false\n\n");

        prompt.append("用户输入: \"").append(userMessage).append("\"\n\n");
        prompt.append( """
            [指令] 你是一个 JSON 输出机器，只输出 JSON，不输出任何其他内容。
            [规则] 禁止：代码示例、解释文本、额外内容
            [格式] 必须严格遵守：
            {
                "needs_tool": true/false,
                "tool_name": "工具名称", 
                "parameters": {"参数键": "参数值"},
                "reasoning": "简短理由"
            }
            
            输入: "%s"
            
            立即开始 JSON 输出：
            """.formatted(escapeJson(userMessage)));
        return prompt.toString();
    }

    private String buildUltraStrictPrompt(String userMessage) {
        return """
            {"needs_tool": 
            """.trim(); // 极简提示，让模型直接续写 JSON
    }
    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请仔细分析用户的输入，判断是否需要调用工具，以及调用哪个工具。返回结果严格区分大小写\n\n");

        prompt.append("可用工具列表：\n");
        for (Tool tool : availableTools.values()) {
            prompt.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }

        prompt.append("\n工具调用规则：\n");
        prompt.append("- calculator: 当用户提到数字、计算、算术、数学问题时使用\n");
        prompt.append("- time: 当用户询问时间、日期、现在几点时使用\n");
        prompt.append("- weather: 当用户询问天气、气温、天气预报时使用\n");
        prompt.append("- 如果不需要工具，needs_tool 设为 false\n\n");

        prompt.append("用户输入: \"").append(userMessage).append("\"\n\n");

        prompt.append("""
            请严格按照以下 JSON 格式回复，不要添加任何其他内容：
            {
                "needs_tool": true/false,
                "tool_name": "工具名称",
                "parameters": {"参数键": "参数值"},
                "reasoning": "判断理由"
            }
            
            参数说明：
            - calculator: 需要 "expression" 参数，值为数学表达式
            - weather: 需要 "city" 参数，值为城市名称
            - time: 可选 "type" 参数，值为 "date"/"time"/"full"
            """);

        return prompt.toString();
    }

    /**
     * 解析 LLM 的 JSON 响应（增强版）
     */
//    private ToolDecision parseToolDecision(String llmResponse) {
//        try {
//            String jsonContent = extractJson(llmResponse);
//            System.out.println("提取的 JSON: " + jsonContent);
//
//            ToolDecision decision = objectMapper.readValue(jsonContent, ToolDecision.class);
//
//            // 新增：规范化工具名称
//            if (decision.isNeedsTool() && decision.getToolName() != null) {
//                String normalizedToolName = toolNameNormalizer.normalize(decision.getToolName());
//
//                if (normalizedToolName != null) {
//                    decision.setToolName(normalizedToolName); // 更新为标准名称
//                    System.out.println("工具名称规范化: " + decision.getToolName() + " -> " + normalizedToolName);
//                }
//            }
//
//            // 验证工具是否存在
//            if (decision.isNeedsTool() && !availableTools.containsKey(decision.getToolName())) {
//                System.out.println("工具不存在: " + decision.getToolName());
//                decision.setNeedsTool(false);
//                decision.setReasoning("请求的工具 '" + decision.getToolName() + "' 不存在，降级为直接对话");
//            }
//
//            return decision;
//
//        } catch (Exception e) {
//            System.err.println("JSON 解析失败: " + e.getMessage());
//            return fallbackAnalysis(llmResponse);
//        }
//    }

    /**
     * 解析工具决策
     */
    private ToolDecision parseToolDecision(String response) {
        try {
            ToolDecision decision = objectMapper.readValue(response, ToolDecision.class);

            // 规范化工具名称
            if (decision.isNeedsTool() && decision.getToolName() != null) {
                String normalized = toolNameNormalizer.normalize(decision.getToolName());
                if (normalized != null) {
                    decision.setToolName(normalized);
                }
            }

            return decision;

        } catch (Exception e) {
            System.err.println("解析 ToolDecision 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证决策是否有效
     */
    private boolean isValidDecision(ToolDecision decision) {
        if (decision == null) return false;

        if (decision.isNeedsTool()) {
            return decision.getToolName() != null &&
                    !decision.getToolName().trim().isEmpty() &&
                    availableTools.containsKey(decision.getToolName());
        }

        return true; // 不需要工具也是有效的
    }

    /**
     * 规则降级处理
     */
    private ToolDecision fallbackToRuleBased(String userMessage) {
        // 这里可以调用你之前实现的规则引擎
        System.out.println("使用规则降级处理: " + userMessage);

        // 简化的规则匹配
        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.matches(".*(计算|算一下|等于多少|\\d+[+\\-*/]\\d+).*")) {
            return new ToolDecision(true, "calculator",
                    Map.of("expression", extractMathExpression(userMessage)),
                    "规则匹配：数学计算");
        }

        if (lowerMessage.matches(".*(现在几点|当前时间|今天几号|什么时间).*")) {
            return new ToolDecision(true, "time", Map.of(), "规则匹配：时间查询");
        }

        if (lowerMessage.matches(".*(天气|气温|天气预报).*")) {
            return new ToolDecision(true, "weather",
                    Map.of("city", extractCity(userMessage)), "规则匹配：天气查询");
        }

        return new ToolDecision(false, null, Map.of(), "规则匹配：不需要工具");
    }

    private String extractJson(String text) {
        // 从 LLM 响应中提取 JSON 部分
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return text;
    }

    /**
     * 尝试修复常见的 JSON 格式问题
     */
    private String attemptJsonRepair(String text) {
        // 移除可能的前缀和后缀
        String cleaned = text.trim();

        // 找到第一个 { 和最后一个 }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        throw new RuntimeException("无法从响应中提取有效的 JSON: " + text);
    }

    /**
     * 降级分析：当 LLM 分析失败时使用规则匹配
     */
    private ToolDecision fallbackAnalysis(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        // 规则匹配
        if (containsMathKeywords(lowerMessage)) {
            return new ToolDecision(true, "calculator",
                    Map.of("expression", extractMathExpression(userMessage)),
                    "规则匹配：包含数学关键词");
        }

        if (containsTimeKeywords(lowerMessage)) {
            return new ToolDecision(true, "time", Map.of(), "规则匹配：询问时间");
        }

        if (containsWeatherKeywords(lowerMessage)) {
            String city = extractCity(userMessage);
            return new ToolDecision(true, "weather",
                    Map.of("city", city), "规则匹配：询问天气");
        }

        return new ToolDecision(false, null, Map.of(), "规则匹配：不需要工具");
    }

    // 关键词匹配方法
    private boolean containsMathKeywords(String message) {
        return message.matches(".*(计算|算一下|等于多少|加减|乘除|\\d+[+\\-*/]\\d+).*");
    }

    private boolean containsTimeKeywords(String message) {
        return message.matches(".*(现在几点|当前时间|今天几号|什么时间|日期).*");
    }

    private boolean containsWeatherKeywords(String message) {
        return message.matches(".*(天气|气温|天气预报|下雨|温度|气候).*");
    }

    private String extractMathExpression(String message) {
        // 简单的数学表达式提取
        Pattern pattern = Pattern.compile("(\\d+[+\\-*/]\\d+)");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : "0+0"; // 默认值
    }

    private String extractCity(String message) {
        // 简单的城市名称提取
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都"};
        for (String city : cities) {
            if (message.contains(city)) return city;
        }
        return "北京"; // 默认城市
    }

    /**
     * 获取可用工具列表
     */
    public Map<String, String> getAvailableTools() {
        Map<String, String> tools = new HashMap<>();
        for (Tool tool : availableTools.values()) {
            tools.put(tool.getName(), tool.getDescription());
        }
        return tools;
    }

    /**
     * 执行工具调用
     */
    public String executeTool(String toolName, Map<String, String> parameters) {
        Tool tool = availableTools.get(toolName);
        if (tool != null) {
            return tool.execute(parameters);
        }
        return "错误：工具未找到 - " + toolName;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

}
