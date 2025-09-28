package com.liling.Agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * LLM 返回的工具调用决策
 */
public class ToolDecision {
    @JsonProperty("needs_tool")
    private boolean needsTool;

    @JsonProperty("tool_name")
    private String toolName;

    @JsonProperty("parameters")
    private Map<String, String> parameters;

    @JsonProperty("reasoning")
    private String reasoning;

    // 构造函数
    public ToolDecision() {}

    public ToolDecision(boolean needsTool, String toolName,
                        Map<String, String> parameters, String reasoning) {
        this.needsTool = needsTool;
        this.toolName = toolName;
        this.parameters = parameters;
        this.reasoning = reasoning;
    }

    // Getter 和 Setter
    public boolean isNeedsTool() { return needsTool; }
    public void setNeedsTool(boolean needsTool) { this.needsTool = needsTool; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    @Override
    public String toString() {
        return String.format("ToolDecision{needsTool=%s, toolName='%s', parameters=%s, reasoning='%s'}",
                needsTool, toolName, parameters, reasoning);
    }
}
