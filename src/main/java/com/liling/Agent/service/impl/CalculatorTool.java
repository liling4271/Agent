package com.liling.Agent.service.impl;

import com.liling.Agent.service.Tool;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CalculatorTool implements Tool {
    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算，支持加减乘除等运算";
    }

    @Override
    public String execute(Map<String, String> parameters) {
        try {
            String expression = parameters.getOrDefault("expression", "");
            if (expression.isEmpty()) {
                return "错误：未提供计算表达式";
            }

            // 简单的表达式计算（生产环境应该使用更安全的计算库）
            double result = evaluateExpression(expression);
            return String.format("计算结果: %s = %.2f", expression, result);

        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    private double evaluateExpression(String expression) {
        // 移除空格
        expression = expression.replaceAll("\\s+", "");

        // 简单的四则运算解析
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            double divisor = Double.parseDouble(parts[1]);
            if (divisor == 0) throw new ArithmeticException("除数不能为零");
            return Double.parseDouble(parts[0]) / divisor;
        } else {
            return Double.parseDouble(expression);
        }
    }
}
