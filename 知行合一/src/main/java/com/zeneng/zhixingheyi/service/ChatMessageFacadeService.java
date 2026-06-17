package com.zeneng.zhixingheyi.service;

import com.zeneng.zhixingheyi.model.dto.ChatMessageDTO;
import com.zeneng.zhixingheyi.model.request.CreateChatMessageRequest;
import com.zeneng.zhixingheyi.model.request.UpdateChatMessageRequest;
import com.zeneng.zhixingheyi.model.response.CreateChatMessageResponse;
import com.zeneng.zhixingheyi.model.response.GetChatMessagesResponse;

import java.util.List;

public interface ChatMessageFacadeService {
    GetChatMessagesResponse getChatMessagesBySessionId(String sessionId);

    List<ChatMessageDTO> getChatMessagesBySessionIdRecently(String sessionId, int limit);

    CreateChatMessageResponse createChatMessage(CreateChatMessageRequest request);

    CreateChatMessageResponse createChatMessage(ChatMessageDTO chatMessageDTO);

    CreateChatMessageResponse agentCreateChatMessage(CreateChatMessageRequest request);

    CreateChatMessageResponse appendChatMessage(String chatMessageId, String appendContent);

    void deleteChatMessage(String chatMessageId);

    void updateChatMessage(String chatMessageId, UpdateChatMessageRequest request);
}
