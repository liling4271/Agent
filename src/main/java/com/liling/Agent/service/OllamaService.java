package com.liling.Agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Ollama 默认运行在 11434 端口
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

    public OllamaService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 与 DeepSeek 模型对话
     */
    public String chatWithDeepSeek(String message) {
        try {
            String url = OLLAMA_BASE_URL + "/api/generate";

            // 构建请求体
            String requestBody = """
            {
                "model": "deepseek-coder:6.7b",
                "prompt": "%s",
                "stream": false,
                "options": {
                    "temperature": 0.7,
                    "num_predict": 1000
                }
            }
            """.formatted(escapeJson(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            return extractResponse(response.getBody());

        } catch (Exception e) {
            return "请求失败，请确保 Ollama 服务正在运行。错误信息: " + e.getMessage();
        }
    }

    /**
     * 获取可用的模型列表
     */
    public String getAvailableModels() {
        try {
            String url = OLLAMA_BASE_URL + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "无法获取模型列表: " + e.getMessage();
        }
    }

    /**
     * 检查 Ollama 服务状态
     */
    public String checkHealth() {
        try {
            String url = OLLAMA_BASE_URL + "/api/version";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return "Ollama 服务运行正常。版本信息: " + response.getBody();
        } catch (Exception e) {
            return "Ollama 服务未运行。请先启动 Ollama 服务。";
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.get("response").asText();
        } catch (Exception e) {
            return "解析响应失败: " + e.getMessage() + "\n原始响应: " + responseBody;
        }
    }
}
