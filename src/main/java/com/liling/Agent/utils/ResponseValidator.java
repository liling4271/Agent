package com.liling.Agent.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResponseValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 主清洗方法 - 处理各种不规范的 LLM 响应
     */
    public String cleanLLMResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "{\"needs_tool\": false, \"tool_name\": \"\", \"parameters\": {}, \"reasoning\": \"空响应\"}";
        }

        System.out.println("原始响应: " + rawResponse);

        // 步骤1: 移除代码块和示例
        String step1 = removeCodeExamples(rawResponse);
        System.out.println("移除代码后: " + step1);

        // 步骤2: 移除解释性文本
        String step2 = removeExplanatoryText(step1);
        System.out.println("移除解释后: " + step2);

        // 步骤3: 提取 JSON 部分
        String step3 = extractJsonContent(step2);
        System.out.println("提取 JSON 后: " + step3);

        // 步骤4: 验证和修复 JSON 格式
        String step4 = validateAndFixJson(step3);
        System.out.println("修复 JSON 后: " + step4);

        return step4;
    }

    /**
     * 移除代码示例和代码块
     */
    private String removeCodeExamples(String text) {
        // 移除 Python 代码示例
        text = text.replaceAll("```python.*?```", "");
        text = text.replaceAll("def\\s+\\w+.*?return.*", "");
        text = text.replaceAll("print\\(.*?\\)", "");

        // 移除 Java 代码示例
        text = text.replaceAll("```java.*?```", "");
        text = text.replaceAll("public.*?\\{.*?\\}", "");

        // 移除通用代码块标记
        text = text.replaceAll("```json", "");
        text = text.replaceAll("```", "");

        // 移除函数定义
        text = text.replaceAll("def\\s+\\w+\\s*\\(.*?\\):", "");
        text = text.replaceAll("function\\s+\\w+\\s*\\(.*?\\)\\s*\\{", "");

        return text.trim();
    }

    /**
     * 移除解释性文本和示例说明
     */
    private String removeExplanatoryText(String text) {
        // 移除中文解释文本
        text = text.replaceAll("以下是.*?：", "");
        text = text.replaceAll("这是一个.*?：", "");
        text = text.replaceAll("可能的.*?：", "");
        text = text.replaceAll("示例.*?：", "");
        text = text.replaceAll("我们可以.*?：", "");
        text = text.replaceAll("解决方案.*?：", "");

        // 移除英文解释文本
        text = text.replaceAll("Here is.*?:", "");
        text = text.replaceAll("The following.*?:", "");
        text = text.replaceAll("Example.*?:", "");
        text = text.replaceAll("We can.*?:", "");
        text = text.replaceAll("Solution.*?:", "");

        // 移除常见的开头短语
        String[] prefixesToRemove = {
                "根据用户输入", "针对这个问题", "对于这个请求",
                "Based on the input", "For this request", "Regarding this query"
        };

        for (String prefix : prefixesToRemove) {
            if (text.startsWith(prefix)) {
                text = text.substring(prefix.length()).trim();
            }
        }

        return text.trim();
    }

    /**
     * 提取 JSON 内容
     */
    private String extractJsonContent(String text) {
        // 方法1: 尝试找到完整的 JSON 对象
        Pattern jsonPattern = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(text);

        if (matcher.find()) {
            String jsonCandidate = matcher.group();
            // 验证这个 JSON 是否包含必要的字段
            if (isValidToolDecisionJson(jsonCandidate)) {
                return jsonCandidate;
            }
        }

        // 方法2: 如果找不到完整 JSON，尝试逐行查找
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("{") && line.contains("needs_tool")) {
                return line;
            }
        }

        // 方法3: 如果还是找不到，返回原始文本让后续步骤处理
        return text;
    }

    /**
     * 验证和修复 JSON 格式
     */
    private String validateAndFixJson(String jsonText) {
        try {
            // 尝试直接解析
            objectMapper.readTree(jsonText);
            return jsonText; // 格式正确，直接返回
        } catch (Exception e) {
            System.out.println("JSON 解析失败，尝试修复: " + e.getMessage());
            return attemptJsonRepair(jsonText);
        }
    }

    /**
     * 尝试修复常见的 JSON 格式问题
     */
    private String attemptJsonRepair(String brokenJson) {
        String repaired = brokenJson;

        // 修复1: 确保有起始和结束大括号
        if (!repaired.trim().startsWith("{")) {
            repaired = "{" + repaired;
        }
        if (!repaired.trim().endsWith("}")) {
            repaired = repaired + "}";
        }

        // 修复2: 修复常见的符号问题
        repaired = repaired.replaceAll("，", ","); // 中文逗号转英文
        repaired = repaired.replaceAll("：", ":"); // 中文冒号转英文
        repaired = repaired.replaceAll("“", "\"").replaceAll("”", "\""); // 中文引号转英文

        // 修复3: 确保键有引号
        repaired = repaired.replaceAll("(\\w+)\\s*:", "\"$1\":");

        // 修复4: 处理布尔值
        repaired = repaired.replaceAll("\"true\"", "true");
        repaired = repaired.replaceAll("\"false\"", "false");

        // 修复5: 处理嵌套引号
        repaired = repaired.replaceAll("'([^']*)'", "\"$1\"");

        try {
            // 再次验证修复后的 JSON
            objectMapper.readTree(repaired);
            return repaired;
        } catch (Exception e) {
            System.out.println("JSON 修复失败，返回降级响应");
            return createFallbackJson();
        }
    }

    /**
     * 验证是否是有效的 ToolDecision JSON
     */
    private boolean isValidToolDecisionJson(String jsonText) {
        try {
            String cleaned = jsonText.trim();
            return cleaned.startsWith("{") &&
                    cleaned.endsWith("}") &&
                    cleaned.contains("needs_tool") &&
                    cleaned.contains("tool_name");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建降级 JSON 响应
     */
    private String createFallbackJson() {
        return "{\"needs_tool\": false, \"tool_name\": \"\", \"parameters\": {}, \"reasoning\": \"JSON解析失败\"}";
    }

    /**
     * 检查响应是否包含代码示例（用于决策是否要清洗）
     */
    public boolean containsCodeExample(String response) {
        if (response == null) return false;

        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("def ") ||
                lowerResponse.contains("function") ||
                lowerResponse.contains("print(") ||
                lowerResponse.contains("public ") ||
                response.contains("```") ||
                response.contains("以下是可能的") ||
                response.contains("这是一个示例");
    }
}
