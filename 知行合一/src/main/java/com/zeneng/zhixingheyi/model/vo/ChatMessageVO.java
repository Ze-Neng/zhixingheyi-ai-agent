package com.zeneng.zhixingheyi.model.vo;

import com.zeneng.zhixingheyi.model.dto.ChatMessageDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageVO {
    private String id;
    private String sessionId;
    private ChatMessageDTO.RoleType role;
    private String content;
    private ChatMessageDTO.MetaData metadata;
}
