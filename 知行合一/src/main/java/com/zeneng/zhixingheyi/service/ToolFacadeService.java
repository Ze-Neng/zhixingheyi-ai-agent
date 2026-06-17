package com.zeneng.zhixingheyi.service;

import com.zeneng.zhixingheyi.agent.tools.Tool;

import java.util.List;

public interface ToolFacadeService {
    List<Tool> getAllTools();

    List<Tool> getOptionalTools();

    List<Tool> getFixedTools();
}
