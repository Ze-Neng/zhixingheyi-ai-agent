package com.zeneng.zhixingheyi.model.response;

import com.zeneng.zhixingheyi.model.vo.KnowledgeBaseVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetKnowledgeBasesResponse {
    private KnowledgeBaseVO[] knowledgeBases;
}

