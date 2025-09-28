package com.liling.Agent.service;

import java.util.Map;

/**
 * 工具接口
 */
public interface Tool {

    String getName();
    String getDescription();
    String execute(Map<String, String> parameters);
}
