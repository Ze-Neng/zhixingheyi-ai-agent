package com.zeneng.zhixingheyi.service;

import com.zeneng.zhixingheyi.model.request.CreateAgentRequest;
import com.zeneng.zhixingheyi.model.request.UpdateAgentRequest;
import com.zeneng.zhixingheyi.model.response.CreateAgentResponse;
import com.zeneng.zhixingheyi.model.response.GetAgentsResponse;

public interface AgentFacadeService {
    GetAgentsResponse getAgents();

    CreateAgentResponse createAgent(CreateAgentRequest request);

    void deleteAgent(String agentId);

    void updateAgent(String agentId, UpdateAgentRequest request);
}
