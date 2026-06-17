package com.zeneng.zhixingheyi.model.response;

import com.zeneng.zhixingheyi.model.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatMessagesResponse {
    private ChatMessageVO[] chatMessages;
}

