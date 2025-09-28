package com.liling.Agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApplication {

	public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
        System.out.println("🚀 Ollama AI Agent 启动成功！");
        System.out.println("📍 工具列表: http://localhost:8081/api/ai/tools");
	}

}
