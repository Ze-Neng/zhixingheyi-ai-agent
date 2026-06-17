package com.zeneng.zhixingheyi.agent.tools.test;

import com.zeneng.zhixingheyi.agent.tools.Tool;
import com.zeneng.zhixingheyi.agent.tools.ToolType;
import org.springframework.stereotype.Component;

// 注释 @Component 注解，功能已被 WeatherQueryTool 替代
// @Component
public class CityTool implements Tool {
    @Override
    public String getName() {
        return "cityTool";
    }

    @Override
    public String getDescription() {
        return "获取当前的城市";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @org.springframework.ai.tool.annotation.Tool(name = "getCity", description = "获取当前的城市")
    public String getCity() {
        return "深圳";
    }
}
