package com.liling.Agent.utils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ToolNameNormalizer {

    // 工具名称映射：标准名称 -> 可能的变体
    private final Map<String, String> toolNameVariants = new HashMap<>();

    public ToolNameNormalizer() {
        initializeToolNameMapping();
    }

    private void initializeToolNameMapping() {
        // 小写变体
        toolNameVariants.put("time", "time");
        toolNameVariants.put("timer", "time");
        toolNameVariants.put("currenttime", "time");
        toolNameVariants.put("gettime", "time");

        // 大写变体
        toolNameVariants.put("Time", "time");
        toolNameVariants.put("TIME", "time");
        toolNameVariants.put("Timer", "time");

        // 计算器变体
        toolNameVariants.put("calculator", "calculator");
        toolNameVariants.put("Calculator", "calculator");
        toolNameVariants.put("CALCULATOR", "calculator");
        toolNameVariants.put("calc", "calculator");
        toolNameVariants.put("Calc", "calculator");
        toolNameVariants.put("calculation", "calculator");

        // 天气变体
        toolNameVariants.put("weather", "weather");
        toolNameVariants.put("Weather", "weather");
        toolNameVariants.put("WEATHER", "weather");
        toolNameVariants.put("forecast", "weather");
        toolNameVariants.put("Forecast", "weather");
    }

    /**
     * 规范化工具名称
     */
    public String normalize(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null;
        }

        String trimmed = toolName.trim();

        // 1. 首先检查映射表
        if (toolNameVariants.containsKey(trimmed)) {
            return toolNameVariants.get(trimmed);
        }

        // 2. 转换为小写检查
        String lowerCase = trimmed.toLowerCase();
        if (toolNameVariants.containsKey(lowerCase)) {
            return toolNameVariants.get(lowerCase);
        }

        // 3. 模糊匹配
        return fuzzyMatch(trimmed);
    }

    /**
     * 模糊匹配工具名称
     */
    private String fuzzyMatch(String toolName) {
        String lowerToolName = toolName.toLowerCase();

        for (String variant : toolNameVariants.keySet()) {
            String lowerVariant = variant.toLowerCase();

            // 包含关系匹配
            if (lowerToolName.contains(lowerVariant) || lowerVariant.contains(lowerToolName)) {
                return toolNameVariants.get(variant);
            }

            // 编辑距离匹配（用于拼写错误）
            if (calculateLevenshteinDistance(lowerToolName, lowerVariant) <= 2) {
                return toolNameVariants.get(variant);
            }
        }

        return null; // 无法匹配
    }

    /**
     * 计算编辑距离（Levenshtein Distance）
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                                    dp[i - 1][j] + 1,     // 删除
                                    dp[i][j - 1] + 1),    // 插入
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1) // 替换
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 获取所有支持的工具名称
     */
    public Map<String, String> getSupportedTools() {
        Map<String, String> supported = new HashMap<>();
        for (String variant : toolNameVariants.keySet()) {
            String standardName = toolNameVariants.get(variant);
            if (!supported.containsKey(standardName)) {
                supported.put(standardName, standardName);
            }
        }
        return supported;
    }
}
