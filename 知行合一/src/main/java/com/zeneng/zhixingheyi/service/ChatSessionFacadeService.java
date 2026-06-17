package com.zeneng.zhixingheyi.service;

import com.zeneng.zhixingheyi.model.request.CreateChatSessionRequest;
import com.zeneng.zhixingheyi.model.request.UpdateChatSessionRequest;
import com.zeneng.zhixingheyi.model.response.CreateChatSessionResponse;
import com.zeneng.zhixingheyi.model.response.GetChatSessionResponse;
import com.zeneng.zhixingheyi.model.response.GetChatSessionsResponse;

public interface ChatSessionFacadeService {
    GetChatSessionsResponse getChatSessions();

    GetChatSessionResponse getChatSession(String chatSessionId);

    GetChatSessionsResponse getChatSessionsByAgentId(String agentId);

    CreateChatSessionResponse createChatSession(CreateChatSessionRequest request);

    void deleteChatSession(String chatSessionId);

    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);
}
