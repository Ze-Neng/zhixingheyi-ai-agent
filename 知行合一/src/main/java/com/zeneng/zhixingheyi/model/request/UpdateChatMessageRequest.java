package com.zeneng.zhixingheyi.model.request;

import com.zeneng.zhixingheyi.model.dto.ChatMessageDTO;
import lombok.Data;

@Data
public class UpdateChatMessageRequest {
    private String content;
    private ChatMessageDTO.MetaData metadata;
}

