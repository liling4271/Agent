package com.liling.Agent.service.impl;


import com.liling.Agent.service.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TimeTool implements Tool {

    @Override
    public String getName() {
        return "time";
    }

    @Override
    public String getDescription() {
        return "获取当前时间、日期和时间信息";
    }

    @Override
    public String execute(Map<String, String> parameters) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");

        String timeType = parameters.getOrDefault("type", "full");

        switch (timeType) {
            case "date":
                return "当前日期: " + now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            case "time":
                return "当前时间: " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            case "full":
            default:
                return "当前时间: " + now.format(formatter);
        }
    }
}
