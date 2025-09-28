package com.liling.Agent.service.impl;

import com.liling.Agent.service.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherTool implements Tool {

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return "查询城市天气信息，包括温度、天气状况等";
    }

    @Override
    public String execute(Map<String, String> parameters) {
        String city = parameters.getOrDefault("city", "北京");

        // 模拟天气数据（生产环境应该调用真实天气API）
        Map<String, String> weatherData = Map.of(
                "北京", "晴，15°C～25°C，微风",
                "上海", "多云，18°C～28°C，东南风3级",
                "深圳", "阵雨，22°C～30°C，南风2级",
                "杭州", "晴转多云，16°C～26°C，微风"
        );

        String weather = weatherData.getOrDefault(city, "晴，20°C～28°C，微风");
        return String.format("%s的天气: %s", city, weather);
    }
}
